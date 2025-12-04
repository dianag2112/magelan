package bg.softuni.magelan.web;

import bg.softuni.magelan.order.model.Order;
import bg.softuni.magelan.order.model.OrderStatus;
import bg.softuni.magelan.order.service.OrderService;
import bg.softuni.magelan.payment.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminOrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AdminOrderController adminOrderController;

    private Order submitted;
    private Order confirmed;
    private Order delivered;

    @BeforeEach
    void setUp() {
        submitted = new Order();
        confirmed = new Order();
        delivered = new Order();
    }

    @Test
    void getAdminOrdersPage_shouldPopulateModelWithOrdersGroupedByStatus() {
        when(orderService.getOrdersByStatus(OrderStatus.SUBMITTED))
                .thenReturn(List.of(submitted));
        when(orderService.getOrdersByStatus(OrderStatus.CONFIRMED))
                .thenReturn(List.of(confirmed));
        when(orderService.getOrdersByStatus(OrderStatus.DELIVERED))
                .thenReturn(List.of(delivered));

        ModelAndView mav = adminOrderController.getAdminOrdersPage("", "");

        assertThat(mav.getViewName()).isEqualTo("admin-orders");
        assertThat(mav.getModel().get("submittedOrders")).isEqualTo(List.of(submitted));
        assertThat(mav.getModel().get("confirmedOrders")).isEqualTo(List.of(confirmed));
        assertThat(mav.getModel().get("deliveredOrders")).isEqualTo(List.of(delivered));
    }

    @Test
    void getOrderDetails_shouldReturnViewWithOrderAndPayment() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        PaymentResponse payment = mock(PaymentResponse.class);

        when(orderService.getOrderById(orderId)).thenReturn(order);
        when(orderService.getPaymentForOrder(orderId)).thenReturn(payment);

        ModelAndView mav = adminOrderController.getOrderDetails(orderId, "msg", "err");

        assertThat(mav.getViewName()).isEqualTo("admin-order-details");
        assertThat(mav.getModel().get("order")).isSameAs(order);
        assertThat(mav.getModel().get("payment")).isSameAs(payment);
        assertThat(mav.getModel().get("message")).isEqualTo("msg");
        assertThat(mav.getModel().get("error")).isEqualTo("err");
    }

    @Test
    void updateOrderStatus_shouldCallServiceAndRedirect() {
        UUID orderId = UUID.randomUUID();

        String result = adminOrderController.updateOrderStatus(orderId, OrderStatus.CONFIRMED);

        verify(orderService).changeAdminOrderStatus(orderId, OrderStatus.CONFIRMED);
        assertThat(result).isEqualTo("redirect:/admin/orders/" + orderId);
    }
}
