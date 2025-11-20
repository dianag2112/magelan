package bg.softuni.magelan.order.service;

import bg.softuni.magelan.payment.PaymentClient;
import bg.softuni.magelan.payment.PaymentRequest;
import bg.softuni.magelan.payment.PaymentResponse;
import bg.softuni.magelan.product.service.ProductService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import bg.softuni.magelan.order.model.Order;
import bg.softuni.magelan.order.model.OrderItem;
import bg.softuni.magelan.order.model.OrderStatus;
import bg.softuni.magelan.order.repository.OrderItemRepository;
import bg.softuni.magelan.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import bg.softuni.magelan.product.model.Product;
import bg.softuni.magelan.product.repository.ProductRepository;
import bg.softuni.magelan.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final PaymentClient paymentClient;
    private final ProductService productService;

    public Optional<Order> findPendingOrderByCustomerId(UUID customerId) {
        return orderRepository.findByOrderStatusAndCustomer_Id(OrderStatus.PENDING, customerId);
    }

    public List<Product> getAvailableProducts() {
        return productRepository.findAllByActiveTrueOrderByNameAsc();
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findAllByOrderStatusOrderByCreatedOnDesc(status);
    }

    @Transactional
    public Order getOrCreatePendingOrderForCustomer(User customer) {
        return findPendingOrderByCustomerId(customer.getId())
                .orElseGet(() -> createPendingOrderForCustomer(customer));
    }

    @Transactional
    public void addProductToCustomerOrder(User customer, UUID productId, int quantity) {
        if (quantity <= 0) {
            return;
        }
        Order order = getOrCreatePendingOrderForCustomer(customer);
        addProductToOrder(order.getId(), productId, quantity);
    }

    @Transactional
    public Order createPendingOrderForCustomer(User customer) {
        Order order = Order.builder()
                .customer(customer)
                .orderStatus(OrderStatus.PENDING)
                .amount(BigDecimal.ZERO)
                .createdOn(LocalDateTime.now())
                .build();
        return orderRepository.save(order);
    }

    @Transactional
    public void addProductToOrder(UUID orderId, UUID productId, int quantity) {
        if (quantity <= 0) {
            return;
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        OrderItem existingItem = order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            orderItemRepository.save(existingItem);
        } else {
            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .createdOn(LocalDateTime.now())
                    .build();

            orderItemRepository.save(item);
            order.getItems().add(item);
        }

        updateOrderTotal(order);
    }

    @Transactional
    public void removeItemFromOrder(UUID userId, UUID itemId) {
        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));

        Order order = item.getOrder();

        if (!order.getCustomer().getId().equals(userId)) {
            throw new IllegalStateException("Cannot remove item from another user's order");
        }

        order.getItems().remove(item);
        orderItemRepository.delete(item);

        updateOrderTotal(order);
    }

    public BigDecimal calculateTotal(Order order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return BigDecimal.ZERO;
        }
        return order.getItems().stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public PaymentResponse startPaymentForCurrentOrder(
            UUID userId,
            String fullName,
            String phone,
            String address,
            String notes
    ) {
        Order order = findPendingOrderByCustomerId(userId)
                .orElseThrow(() -> new IllegalStateException("No pending order"));

        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot start payment for empty order.");
        }

        order.setDeliveryFullName(fullName);
        order.setDeliveryPhone(phone);
        order.setDeliveryAddress(address);
        order.setDeliveryNotes(notes);

        BigDecimal total = calculateTotal(order);
        order.setAmount(total);

        if (order.getPaymentId() != null) {
            PaymentResponse existing = paymentClient.getPaymentById(order.getPaymentId());
            return existing;
        }

        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .amount(total)
                .method("CARD")
                .build();

        try {
            PaymentResponse payment = paymentClient.createPayment(request);
            order.setPaymentId(payment.getId());

            orderRepository.save(order);
            return payment;

        } catch (FeignException e) {

            if (e.status() == 409) {

                PaymentResponse existing = paymentClient.getPaymentByOrderId(order.getId());
                order.setPaymentId(existing.getId());
                orderRepository.save(order);
                return existing;
            }

            throw e;
        }
    }

    @Transactional
    public void changeAdminOrderStatus(UUID orderId, OrderStatus targetStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));

        OrderStatus current = order.getOrderStatus();

        if (targetStatus == OrderStatus.CONFIRMED) {
            if (current != OrderStatus.SUBMITTED) {
                throw new IllegalStateException("Only SUBMITTED orders can be confirmed.");
            }
            order.setOrderStatus(OrderStatus.CONFIRMED);
        } else if (targetStatus == OrderStatus.DELIVERED) {
            if (current != OrderStatus.CONFIRMED) {
                throw new IllegalStateException("Only CONFIRMED orders can be marked as DELIVERED.");
            }
            order.setOrderStatus(OrderStatus.DELIVERED);
        } else {
            throw new IllegalStateException("Unsupported status change.");
        }

        orderRepository.save(order);
    }

    @Transactional
    protected void updateOrderTotal(Order order) {
        BigDecimal total = calculateTotal(order);
        order.setAmount(total);
        orderRepository.save(order);
    }

    public List<Order> getPastOrders(UUID customerId) {
        return orderRepository.findAllByCustomer_IdAndOrderStatusNotOrderByCreatedOnDesc(
                customerId,
                OrderStatus.PENDING
        );
    }

    public Order getOrderById(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    @Transactional
    public PaymentResponse processPayment(UUID paymentId) {

        PaymentResponse updated = paymentClient.processPayment(paymentId);

        Order order = orderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> new IllegalStateException("Order not found for payment " + paymentId));

        if ("PAID".equalsIgnoreCase(updated.getStatus())) {
            order.setOrderStatus(OrderStatus.SUBMITTED);
        }

        orderRepository.save(order);
        return updated;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        return paymentClient.getPaymentById(paymentId);
    }

    public PaymentResponse getPaymentForOrder(UUID orderId) {
        return null;
    }
}
