package bg.softuni.magelan.web;

import bg.softuni.magelan.order.model.Order;
import bg.softuni.magelan.order.model.OrderStatus;
import bg.softuni.magelan.order.service.OrderService;
import bg.softuni.magelan.payment.PaymentResponse;
import bg.softuni.magelan.product.model.Product;
import bg.softuni.magelan.product.service.ProductService;
import bg.softuni.magelan.security.UserData;
import bg.softuni.magelan.user.model.User;
import bg.softuni.magelan.user.model.UserRole;
import bg.softuni.magelan.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private OrderService orderService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderController orderController;

    private User user;
    private UserData userData;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setUsername("testUser");

        userData = new UserData(
                user.getId(),
                user.getUsername(),
                "encoded",
                UserRole.USER,
                true
        );
    }

    private Order createPendingOrderWithoutPayment() {
        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setCustomer(user);
        order.setOrderStatus(OrderStatus.PENDING);
        order.setCreatedOn(LocalDateTime.now());
        order.setItems(new ArrayList<>());
        return order;
    }

    @Test
    void getOrdersPage_shouldCreatePendingOrder_whenNoneExists() {
        Order newOrder = createPendingOrderWithoutPayment();
        List<Order> pastOrders = List.of(new Order());

        when(userService.getById(user.getId())).thenReturn(user);
        when(orderService.findPendingOrderByCustomerId(user.getId()))
                .thenReturn(Optional.empty());
        when(orderService.createPendingOrderForCustomer(user)).thenReturn(newOrder);
        when(orderService.getPastOrders(user.getId())).thenReturn(pastOrders);
        when(orderService.getAvailableProducts()).thenReturn(Collections.emptyList());
        when(orderService.calculateTotal(newOrder)).thenReturn(BigDecimal.ZERO);

        ModelAndView mav = orderController.getOrdersPage(userData);

        assertThat(mav.getViewName()).isEqualTo("orders");
        assertThat(mav.getModel().get("user")).isEqualTo(user);
        assertThat(mav.getModel().get("orderItems")).isEqualTo(newOrder.getItems());
        assertThat(mav.getModel().get("products")).isEqualTo(Collections.emptyList());
        assertThat(mav.getModel().get("totalAmount")).isEqualTo(BigDecimal.ZERO);
        assertThat(mav.getModel().get("pastOrders")).isEqualTo(pastOrders);
        assertThat(mav.getModel().get("isAuthenticated")).isEqualTo(true);
    }

    @Test
    void getOrdersPage_shouldRedirectToPayment_whenPendingOrderHasPayment() {
        Order order = createPendingOrderWithoutPayment();
        UUID paymentId = UUID.randomUUID();
        order.setPaymentId(paymentId);

        when(userService.getById(user.getId())).thenReturn(user);
        when(orderService.findPendingOrderByCustomerId(user.getId()))
                .thenReturn(Optional.of(order));

        ModelAndView mav = orderController.getOrdersPage(userData);

        assertThat(mav.getViewName())
                .isEqualTo("redirect:/orders/payment/" + paymentId);
    }

    @Test
    void addProductToOrder_shouldRedirectToLogin_whenUserNotAuthenticated() {
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        String result = orderController.addProductToOrder(
                null,
                UUID.randomUUID(),
                1,
                "menu",
                attrs
        );

        assertThat(result).isEqualTo("redirect:/login");
    }

    @Test
    void addProductToOrder_shouldAddProductAndRedirectToMenuByDefault() {
        UUID productId = UUID.randomUUID();
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        Order pending = createPendingOrderWithoutPayment();
        Product product = new Product();
        product.setId(productId);
        product.setName("Magelan Burger");

        when(userService.getById(user.getId())).thenReturn(user);
        when(orderService.findPendingOrderByCustomerId(user.getId()))
                .thenReturn(Optional.of(pending));
        when(productService.getById(productId)).thenReturn(product);

        String result = orderController.addProductToOrder(
                userData,
                productId,
                2,
                "menu",
                attrs
        );

        verify(orderService).addProductToCustomerOrder(user, productId, 2);
        assertThat(result).isEqualTo("redirect:/menu");
        assertThat(attrs.getFlashAttributes())
                .containsKey("orderMessage");
    }

    @Test
    void addProductToOrder_shouldRedirectToOrders_whenReturnToOrders() {
        UUID productId = UUID.randomUUID();
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        Order pending = createPendingOrderWithoutPayment();
        Product product = new Product();
        product.setId(productId);
        product.setName("Magelan Burger");

        when(userService.getById(user.getId())).thenReturn(user);
        when(orderService.findPendingOrderByCustomerId(user.getId()))
                .thenReturn(Optional.of(pending));
        when(productService.getById(productId)).thenReturn(product);

        String result = orderController.addProductToOrder(
                userData,
                productId,
                1,
                "orders",
                attrs
        );

        verify(orderService).addProductToCustomerOrder(user, productId, 1);
        assertThat(result).isEqualTo("redirect:/orders");
    }

    @Test
    void addProductToOrder_shouldPreventAddingToSubmittedOrder() {
        UUID productId = UUID.randomUUID();
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        Order pending = createPendingOrderWithoutPayment();
        pending.setPaymentId(UUID.randomUUID());

        when(userService.getById(user.getId())).thenReturn(user);
        when(orderService.findPendingOrderByCustomerId(user.getId()))
                .thenReturn(Optional.of(pending));

        String result = orderController.addProductToOrder(
                userData,
                productId,
                1,
                "menu",
                attrs
        );

        verify(orderService, never()).addProductToCustomerOrder(any(), any(), anyInt());
        assertThat(result).isEqualTo("redirect:/orders");
        assertThat(attrs.getFlashAttributes())
                .containsKey("orderMessage");
    }

    @Test
    void removeItem_shouldCallServiceAndRedirect() {
        UUID itemId = UUID.randomUUID();

        String result = orderController.removeItem(userData, itemId);

        verify(orderService).removeItemFromOrder(user.getId(), itemId);
        assertThat(result).isEqualTo("redirect:/orders");
    }

    @Test
    void submitOrder_shouldRedirectToLogin_whenUserNotAuthenticated() {
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        String result = orderController.submitOrder(
                null,
                "Name",
                "123",
                "Address",
                null,
                attrs
        );

        assertThat(result).isEqualTo("redirect:/login");
    }

    @Test
    void submitOrder_shouldStartPaymentAndRedirectToPaymentPage() {
        RedirectAttributes attrs = new RedirectAttributesModelMap();
        UUID paymentId = UUID.randomUUID();

        PaymentResponse paymentResponse = mock(PaymentResponse.class);
        when(paymentResponse.getId()).thenReturn(paymentId);

        when(orderService.startPaymentForCurrentOrder(
                eq(user.getId()),
                anyString(),
                anyString(),
                anyString(),
                any())
        ).thenReturn(paymentResponse);

        String result = orderController.submitOrder(
                userData,
                "John Doe",
                "123456",
                "Somewhere",
                "notes",
                attrs
        );

        verify(orderService).startPaymentForCurrentOrder(
                eq(user.getId()),
                eq("John Doe"),
                eq("123456"),
                eq("Somewhere"),
                eq("notes")
        );

        assertThat(result).isEqualTo("redirect:/orders/payment/" + paymentId);
        assertThat(attrs.getFlashAttributes())
                .containsKey("orderMessage");
    }

    @Test
    void showPaymentPage_shouldReturnViewWithPayment() {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse payment = mock(PaymentResponse.class);

        when(orderService.getPaymentById(paymentId)).thenReturn(payment);

        ModelAndView mav = orderController.showPaymentPage(paymentId, userData);

        assertThat(mav.getViewName()).isEqualTo("payment");
        assertThat(mav.getModel().get("payment")).isSameAs(payment);
        assertThat(mav.getModel().get("isAuthenticated")).isEqualTo(true);
    }

    @Test
    void processPayment_shouldRedirectToOrders() {
        UUID paymentId = UUID.randomUUID();
        PaymentResponse response = mock(PaymentResponse.class);
        when(response.getStatus()).thenReturn("PAID");

        when(orderService.processPayment(paymentId)).thenReturn(response);

        RedirectAttributes attrs = new RedirectAttributesModelMap();

        String result = orderController.processPayment(paymentId, attrs);

        verify(orderService).processPayment(paymentId);
        assertThat(result).isEqualTo("redirect:/orders");
        assertThat(attrs.getFlashAttributes())
                .containsKey("orderMessage");
    }

    @Test
    void viewOrder_shouldRedirectToLogin_whenUserNotAuthenticated() {
        ModelAndView mav = orderController.viewOrder(null, UUID.randomUUID());

        assertThat(mav.getViewName()).isEqualTo("redirect:/login");
    }

    @Test
    void viewOrder_shouldThrow_whenUserNotOwner() {
        UUID orderId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());

        Order order = new Order();
        order.setCustomer(otherUser);

        when(userService.getById(user.getId())).thenReturn(user);
        when(orderService.getOrderById(orderId)).thenReturn(order);

        assertThatThrownBy(() -> orderController.viewOrder(userData, orderId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void viewOrder_shouldReturnDetailsView_whenUserIsOwner() {
        UUID orderId = UUID.randomUUID();

        Order order = new Order();
        order.setCustomer(user);

        PaymentResponse payment = mock(PaymentResponse.class);

        when(userService.getById(user.getId())).thenReturn(user);
        when(orderService.getOrderById(orderId)).thenReturn(order);
        when(orderService.getPaymentForOrder(orderId)).thenReturn(payment);

        ModelAndView mav = orderController.viewOrder(userData, orderId);

        assertThat(mav.getViewName()).isEqualTo("order-details-user");
        assertThat(mav.getModel().get("order")).isSameAs(order);
        assertThat(mav.getModel().get("payment")).isSameAs(payment);
    }

    @Test
    void cancelOrder_shouldRedirectToLogin_whenUserNotAuthenticated() {
        RedirectAttributes attrs = new RedirectAttributesModelMap();

        String result = orderController.cancelOrder(null, UUID.randomUUID(), attrs);

        assertThat(result).isEqualTo("redirect:/login");
    }

    @Test
    void cancelOrder_shouldCallServiceAndRedirect() {
        RedirectAttributes attrs = new RedirectAttributesModelMap();
        UUID orderId = UUID.randomUUID();

        String result = orderController.cancelOrder(userData, orderId, attrs);

        verify(orderService).cancelOrder(orderId, user.getId());
        assertThat(result).isEqualTo("redirect:/orders");
        assertThat(attrs.getFlashAttributes().get("orderMessage"))
                .isEqualTo("Order has been cancelled.");

    }
}
