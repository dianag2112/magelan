package bg.softuni.magelan.order.service;

import bg.softuni.magelan.payment.PaymentClient;
import bg.softuni.magelan.payment.PaymentRequest;
import bg.softuni.magelan.payment.PaymentResponse;
import bg.softuni.magelan.product.service.ProductService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import bg.softuni.magelan.order.model.Order;
import bg.softuni.magelan.order.model.OrderItem;
import bg.softuni.magelan.order.model.OrderStatus;
import bg.softuni.magelan.order.repository.OrderItemRepository;
import bg.softuni.magelan.order.repository.OrderRepository;
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
        log.debug("Looking for pending order for customer {}", customerId);
        return orderRepository.findByOrderStatusAndCustomer_Id(OrderStatus.PENDING, customerId);
    }

    public List<Product> getAvailableProducts() {
        log.debug("Fetching all active products to show in menu");
        return productRepository.findAllByActiveTrueOrderByNameAsc();
    }

    public List<Order> getOrdersByStatus(OrderStatus status) {
        log.debug("Fetching orders with status {}", status);
        return orderRepository.findAllByOrderStatusOrderByCreatedOnDesc(status);
    }

    @Transactional
    public Order getOrCreatePendingOrderForCustomer(User customer) {
        return findPendingOrderByCustomerId(customer.getId())
                .map(existing -> {
                    log.info("Using existing pending order {} for customer {}",
                            existing.getId(), customer.getUsername());
                    return existing;
                })
                .orElseGet(() -> {
                    log.info("No pending order for customer {} – creating new one",
                            customer.getUsername());
                    return createPendingOrderForCustomer(customer);
                });
    }

    @Transactional
    public void addProductToCustomerOrder(User customer, UUID productId, int quantity) {
        if (quantity <= 0) {
            log.warn("Attempt to add product {} with non-positive quantity {} to customer {} order. Ignored.",
                    productId, quantity, customer.getUsername());
            return;
        }

        log.info("Adding product {} (quantity {}) to pending order for customer {}",
                productId, quantity, customer.getUsername());

        Order order = getOrCreatePendingOrderForCustomer(customer);
        addProductToOrder(order.getId(), productId, quantity);
    }

    @Transactional
    public Order createPendingOrderForCustomer(User customer) {
        log.info("Creating new PENDING order for customer {}", customer.getUsername());

        Order order = Order.builder()
                .customer(customer)
                .orderStatus(OrderStatus.PENDING)
                .amount(BigDecimal.ZERO)
                .createdOn(LocalDateTime.now())
                .build();

        Order saved = orderRepository.save(order);
        log.info("Created order {} for customer {}", saved.getId(), customer.getUsername());
        return saved;
    }

    @Transactional
    public void addProductToOrder(UUID orderId, UUID productId, int quantity) {
        if (quantity <= 0) {
            log.warn("Attempt to add product {} with non-positive quantity {} to order {}. Ignored.",
                    productId, quantity, orderId);
            return;
        }

        log.info("Adding product {} (quantity {}) to order {}", productId, quantity, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found when trying to add product {}", orderId, productId);
                    return new IllegalArgumentException("Order not found");
                });

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product {} not found when trying to add to order {}", productId, orderId);
                    return new IllegalArgumentException("Product not found");
                });

        OrderItem existingItem = order.getItems().stream()
                .filter(i -> i.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            int oldQty = existingItem.getQuantity();
            existingItem.setQuantity(oldQty + quantity);
            orderItemRepository.save(existingItem);
            log.info("Updated existing item {} in order {}: {} -> {}",
                    existingItem.getId(), orderId, oldQty, existingItem.getQuantity());
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
            log.info("Added new item {} (product {}, quantity {}) to order {}",
                    item.getId(), productId, quantity, orderId);
        }

        updateOrderTotal(order);
    }

    @Transactional
    public void removeItemFromOrder(UUID userId, UUID itemId) {
        log.info("User {} is attempting to remove item {} from their order", userId, itemId);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> {
                    log.warn("Order item {} not found for user {}", itemId, userId);
                    return new IllegalArgumentException("Item not found");
                });

        Order order = item.getOrder();

        if (!order.getCustomer().getId().equals(userId)) {
            log.warn("User {} tried to remove item {} from order {} belonging to user {}",
                    userId, itemId, order.getId(), order.getCustomer().getId());
            throw new IllegalStateException("Cannot remove item from another user's order");
        }

        order.getItems().remove(item);
        orderItemRepository.delete(item);
        log.info("Item {} removed from order {}", itemId, order.getId());

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
        log.info("User {} is starting payment for their current pending order", userId);

        Order order = findPendingOrderByCustomerId(userId)
                .orElseThrow(() -> {
                    log.warn("No pending order found for user {} when starting payment", userId);
                    return new IllegalStateException("No pending order");
                });

        if (order.getItems() == null || order.getItems().isEmpty()) {
            log.warn("User {} tried to start payment for an empty order {}", userId, order.getId());
            throw new IllegalStateException("Cannot start payment for empty order.");
        }

        order.setDeliveryFullName(fullName);
        order.setDeliveryPhone(phone);
        order.setDeliveryAddress(address);
        order.setDeliveryNotes(notes);

        BigDecimal total = calculateTotal(order);
        order.setAmount(total);
        log.info("Order {} total calculated as {} before starting payment", order.getId(), total);

        if (order.getPaymentId() != null) {
            log.info("Order {} already has payment {}. Fetching existing payment.",
                    order.getId(), order.getPaymentId());
            PaymentResponse existing = paymentClient.getPaymentById(order.getPaymentId());
            return existing;
        }

        PaymentRequest request = PaymentRequest.builder()
                .orderId(order.getId())
                .amount(total)
                .method("CARD")
                .build();

        try {
            log.info("Creating payment for order {} with amount {}", order.getId(), total);
            PaymentResponse payment = paymentClient.createPayment(request);
            order.setPaymentId(payment.getId());

            orderRepository.save(order);
            log.info("Payment {} created for order {}", payment.getId(), order.getId());
            return payment;

        } catch (FeignException e) {
            if (e.status() == 409) {
                log.warn("Payment already exists for order {}. Trying to fetch existing payment.", order.getId());

                PaymentResponse existing = paymentClient.getPaymentByOrderId(order.getId());
                order.setPaymentId(existing.getId());
                orderRepository.save(order);

                log.info("Existing payment {} linked to order {}", existing.getId(), order.getId());
                return existing;
            }

            log.error("Error while creating payment for order {}: HTTP status {}", order.getId(), e.status(), e);
            throw e;
        }
    }

    @Transactional
    public void changeAdminOrderStatus(UUID orderId, OrderStatus targetStatus) {
        log.info("Changing status of order {} to {} (admin action)", orderId, targetStatus);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found for admin status change to {}", orderId, targetStatus);
                    return new IllegalArgumentException("Order not found");
                });

        OrderStatus current = order.getOrderStatus();

        if (targetStatus == OrderStatus.CONFIRMED) {
            if (current != OrderStatus.SUBMITTED) {
                log.warn("Invalid status change: order {} is {} but CONFIRMED was requested",
                        orderId, current);
                throw new IllegalStateException("Only SUBMITTED orders can be confirmed.");
            }
            order.setOrderStatus(OrderStatus.CONFIRMED);
        } else if (targetStatus == OrderStatus.DELIVERED) {
            if (current != OrderStatus.CONFIRMED) {
                log.warn("Invalid status change: order {} is {} but DELIVERED was requested",
                        orderId, current);
                throw new IllegalStateException("Only CONFIRMED orders can be marked as DELIVERED.");
            }
            order.setOrderStatus(OrderStatus.DELIVERED);
        } else {
            log.warn("Unsupported status change {} requested for order {}", targetStatus, orderId);
            throw new IllegalStateException("Unsupported status change.");
        }

        orderRepository.save(order);
        log.info("Order {} status changed from {} to {}", orderId, current, targetStatus);
    }

    @Transactional
    protected void updateOrderTotal(Order order) {
        BigDecimal total = calculateTotal(order);
        order.setAmount(total);
        orderRepository.save(order);
        log.debug("Order {} total updated to {}", order.getId(), total);
    }

    public List<Order> getPastOrders(UUID customerId) {
        log.debug("Fetching past (non-pending) orders for customer {}", customerId);
        return orderRepository.findAllByCustomer_IdAndOrderStatusNotOrderByCreatedOnDesc(
                customerId,
                OrderStatus.PENDING
        );
    }

    public Order getOrderById(UUID orderId) {
        log.debug("Fetching order by id {}", orderId);
        return orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found when fetching details", orderId);
                    return new IllegalArgumentException("Order not found");
                });
    }

    @Transactional
    public PaymentResponse processPayment(UUID paymentId) {
        log.info("Processing payment {} in main application", paymentId);

        PaymentResponse updated = paymentClient.processPayment(paymentId);
        log.info("Payment {} processed in payment-svc, new status: {}",
                paymentId, updated.getStatus());

        Order order = orderRepository.findByPaymentId(paymentId)
                .orElseThrow(() -> {
                    log.error("Order not found for payment {} after processing", paymentId);
                    return new IllegalStateException("Order not found for payment " + paymentId);
                });

        if ("SUCCESSFUL".equalsIgnoreCase(updated.getStatus())) {
            log.info("Payment {} SUCCESSFUL – setting order {} status to SUBMITTED",
                    paymentId, order.getId());
            order.setOrderStatus(OrderStatus.SUBMITTED);
        } else {
            log.warn("Payment {} processed with status {} – order {} will stay in status {}",
                    paymentId, updated.getStatus(), order.getId(), order.getOrderStatus());
        }

        orderRepository.save(order);
        return updated;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        log.debug("Fetching payment {} via payment-svc", paymentId);
        return paymentClient.getPaymentById(paymentId);
    }

    public PaymentResponse getPaymentForOrder(UUID orderId) {
        log.debug("Fetching payment for order {}", orderId);
        try {
            return paymentClient.getPaymentByOrderId(orderId);
        } catch (FeignException.NotFound e) {
            log.info("No payment found for order {}", orderId);
            return null;
        }
    }

    @Transactional
    public void cancelOrder(UUID orderId, UUID userId) {
        log.info("User {} is attempting to cancel order {}", userId, orderId);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> {
                    log.warn("Order {} not found for user {} when trying to cancel", orderId, userId);
                    return new IllegalArgumentException("Order not found");
                });

        if (!order.getCustomer().getId().equals(userId)) {
            log.warn("User {} attempted to cancel order {} belonging to user {}",
                    userId, orderId, order.getCustomer().getId());
            throw new IllegalStateException("You are not allowed to cancel this order.");
        }

        orderRepository.delete(order);
        log.info("Order {} was cancelled by user {}", orderId, userId);
    }
}
