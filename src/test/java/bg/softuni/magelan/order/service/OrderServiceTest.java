package bg.softuni.magelan.order.service;

import bg.softuni.magelan.exception.OrderNotFoundException;
import bg.softuni.magelan.order.model.Order;
import bg.softuni.magelan.order.model.OrderItem;
import bg.softuni.magelan.order.model.OrderStatus;
import bg.softuni.magelan.order.repository.OrderItemRepository;
import bg.softuni.magelan.order.repository.OrderRepository;
import bg.softuni.magelan.payment.PaymentClient;
import bg.softuni.magelan.payment.PaymentResponse;
import bg.softuni.magelan.product.model.Product;
import bg.softuni.magelan.product.repository.ProductRepository;
import bg.softuni.magelan.user.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private PaymentClient paymentClient;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User createUser() {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setUsername("testUser");
        return u;
    }

    private Order createOrder(User customer, OrderStatus status) {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomer(customer);
        order.setOrderStatus(status);
        order.setAmount(BigDecimal.ZERO);
        order.setCreatedOn(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        return order;
    }

    private OrderItem createOrderItem(Order order, BigDecimal price, int qty) {
        OrderItem item = new OrderItem();
        item.setId(UUID.randomUUID());
        item.setOrder(order);
        item.setQuantity(qty);
        item.setUnitPrice(price);
        item.setCreatedOn(LocalDateTime.now());
        return item;
    }

    @Test
    void getOrCreatePendingOrderForCustomer_shouldReturnExistingPendingOrder() {
        User user = createUser();
        Order existing = createOrder(user, OrderStatus.PENDING);

        when(orderRepository.findByOrderStatusAndCustomer_Id(OrderStatus.PENDING, user.getId()))
                .thenReturn(Optional.of(existing));

        Order result = orderService.getOrCreatePendingOrderForCustomer(user);

        assertThat(result).isSameAs(existing);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrCreatePendingOrderForCustomer_shouldCreateNewWhenNoPendingOrder() {
        User user = createUser();

        when(orderRepository.findByOrderStatusAndCustomer_Id(OrderStatus.PENDING, user.getId()))
                .thenReturn(Optional.empty());

        Order saved = createOrder(user, OrderStatus.PENDING);
        when(orderRepository.save(any(Order.class))).thenReturn(saved);

        Order result = orderService.getOrCreatePendingOrderForCustomer(user);

        assertThat(result).isSameAs(saved);
        assertThat(result.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void calculateTotal_shouldReturnZero_whenNoItems() {
        Order order = new Order();
        order.setItems(Collections.emptyList());

        BigDecimal total = orderService.calculateTotal(order);

        assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateTotal_shouldSumAllItems() {
        Order order = new Order();
        OrderItem i1 = createOrderItem(order, new BigDecimal("10.00"), 2);
        OrderItem i2 = createOrderItem(order, new BigDecimal("5.50"), 1);
        order.setItems(List.of(i1, i2));

        BigDecimal total = orderService.calculateTotal(order);

        assertThat(total).isEqualByComparingTo(new BigDecimal("25.50"));
    }

    @Test
    void addProductToCustomerOrder_shouldIgnoreNonPositiveQuantity() {
        User user = createUser();
        UUID productId = UUID.randomUUID();

        orderService.addProductToCustomerOrder(user, productId, 0);

        verify(orderRepository, never()).findByOrderStatusAndCustomer_Id(any(), any());
        verify(orderRepository, never()).save(any());
        verify(productRepository, never()).findByIdAndActiveTrue(any());
    }

    @Test
    void addProductToOrder_shouldIncreaseQuantity_whenItemExists() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Order order = createOrder(createUser(), OrderStatus.PENDING);

        Product product = new Product();
        product.setId(productId);
        product.setPrice(new BigDecimal("4.00"));

        OrderItem existing = createOrderItem(order, product.getPrice(), 2);
        existing.setProduct(product);
        order.setItems(new ArrayList<>(List.of(existing)));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndActiveTrue(productId)).thenReturn(Optional.of(product));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        orderService.addProductToOrder(orderId, productId, 3);

        assertThat(existing.getQuantity()).isEqualTo(5);
        verify(orderItemRepository).save(existing);
        verify(orderRepository, atLeastOnce()).save(order);
    }

    @Test
    void addProductToOrder_shouldThrow_whenOrderNotFound() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.addProductToOrder(orderId, productId, 1))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void addProductToOrder_shouldThrow_whenProductNotAvailable() {
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Order order = createOrder(createUser(), OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(productRepository.findByIdAndActiveTrue(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.addProductToOrder(orderId, productId, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product is not available");
    }

    @Test
    void removeItemFromOrder_shouldRemoveItem_whenUserOwnsOrder() {
        UUID userId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Order order = createOrder(user, OrderStatus.PENDING);

        OrderItem item = createOrderItem(order, new BigDecimal("3.00"), 2);
        item.setId(itemId);
        order.setItems(new ArrayList<>(List.of(item)));

        when(orderItemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.removeItemFromOrder(userId, itemId);

        assertThat(order.getItems()).isEmpty();
        verify(orderItemRepository).delete(item);
        verify(orderRepository, atLeastOnce()).save(order);
    }

    @Test
    void removeItemFromOrder_shouldThrow_whenItemBelongsToAnotherUser() {
        UUID userId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        User owner = new User();
        owner.setId(otherUserId);

        Order order = createOrder(owner, OrderStatus.PENDING);

        OrderItem item = createOrderItem(order, new BigDecimal("3.00"), 2);
        item.setId(itemId);
        order.setItems(new ArrayList<>(List.of(item)));

        when(orderItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThatThrownBy(() -> orderService.removeItemFromOrder(userId, itemId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot remove item from another user's order");
    }

    @Test
    void changeAdminOrderStatus_shouldConfirmSubmittedOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(createUser(), OrderStatus.SUBMITTED);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.changeAdminOrderStatus(orderId, OrderStatus.CONFIRMED);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    @Test
    void changeAdminOrderStatus_shouldThrow_whenInvalidTransitionToConfirmed() {
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(createUser(), OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.changeAdminOrderStatus(orderId, OrderStatus.CONFIRMED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Only SUBMITTED orders can be confirmed");
    }

    @Test
    void startPaymentForCurrentOrder_shouldCreatePayment_whenNoExistingPayment() {
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Order order = createOrder(user, OrderStatus.PENDING);

        OrderItem item1 = createOrderItem(order, new BigDecimal("10.00"), 1);
        order.getItems().add(item1);

        when(orderRepository.findByOrderStatusAndCustomer_Id(OrderStatus.PENDING, userId))
                .thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse paymentResponse = mock(PaymentResponse.class);
        UUID paymentId = UUID.randomUUID();
        when(paymentResponse.getId()).thenReturn(paymentId);

        when(paymentClient.createPayment(any())).thenReturn(paymentResponse);

        PaymentResponse result = orderService.startPaymentForCurrentOrder(
                userId,
                "John Doe",
                "123456",
                "Some street",
                "leave at door"
        );

        assertThat(result).isSameAs(paymentResponse);
        assertThat(order.getPaymentId()).isEqualTo(paymentId);
        assertThat(order.getAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        verify(orderRepository).save(order);
    }

    @Test
    void processPayment_shouldSetOrderToSubmitted_whenPaymentSuccessful() {
        UUID paymentId = UUID.randomUUID();

        PaymentResponse paymentResponse = mock(PaymentResponse.class);
        when(paymentResponse.getStatus()).thenReturn("SUCCESSFUL");

        when(paymentClient.processPayment(paymentId)).thenReturn(paymentResponse);

        Order order = createOrder(createUser(), OrderStatus.PENDING);
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse result = orderService.processPayment(paymentId);

        assertThat(result).isSameAs(paymentResponse);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.SUBMITTED);
        verify(orderRepository).save(order);
    }

    @Test
    void processPayment_shouldNotChangeStatus_whenPaymentNotSuccessful() {
        UUID paymentId = UUID.randomUUID();

        PaymentResponse paymentResponse = mock(PaymentResponse.class);
        when(paymentResponse.getStatus()).thenReturn("FAILED");

        when(paymentClient.processPayment(paymentId)).thenReturn(paymentResponse);

        Order order = createOrder(createUser(), OrderStatus.PENDING);
        when(orderRepository.findByPaymentId(paymentId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        orderService.processPayment(paymentId);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.PENDING);
        verify(orderRepository).save(order);
    }

    @Test
    void getOrderById_shouldReturnOrder_whenFound() {
        UUID orderId = UUID.randomUUID();
        Order order = createOrder(createUser(), OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        Order result = orderService.getOrderById(orderId);

        assertThat(result).isSameAs(order);
    }

    @Test
    void getOrderById_shouldThrow_whenNotFound() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(orderId))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cancelOrder_shouldDeleteOrder_whenUserIsOwner() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);

        Order order = createOrder(user, OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        orderService.cancelOrder(orderId, userId);

        verify(orderRepository).delete(order);
    }

    @Test
    void cancelOrder_shouldThrow_whenUserIsNotOwner() {
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        User owner = new User();
        owner.setId(UUID.randomUUID());

        Order order = createOrder(owner, OrderStatus.PENDING);

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(orderId, userId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not allowed");
        verify(orderRepository, never()).delete(any());
    }
}
