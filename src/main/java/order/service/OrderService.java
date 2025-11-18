package order.service;

import lombok.RequiredArgsConstructor;
import order.model.Order;
import order.model.OrderItem;
import order.model.OrderStatus;
import order.repository.OrderItemRepository;
import order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import product.model.Product;
import product.repository.ProductRepository;
import user.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

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
    public void confirmPendingOrder(UUID userId,
                                    String fullName,
                                    String phone,
                                    String address,
                                    String notes) {

        Order order = findPendingOrderByCustomerId(userId)
                .orElseThrow(() -> new IllegalStateException("No pending order to confirm"));

        if (order.getItems().isEmpty()) {
            throw new IllegalStateException("Cannot confirm empty order");
        }

        order.setDeliveryFullName(fullName);
        order.setDeliveryPhone(phone);
        order.setDeliveryAddress(address);
        order.setDeliveryNotes(notes);

        order.setOrderStatus(OrderStatus.SUBMITTED);
        updateOrderTotal(order);

        orderRepository.save(order);
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
}
