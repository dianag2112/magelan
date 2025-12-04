package bg.softuni.magelan.order.scheduler;

import bg.softuni.magelan.order.model.Order;
import bg.softuni.magelan.order.model.OrderStatus;
import bg.softuni.magelan.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderAutoDeliverySchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderAutoDeliveryScheduler scheduler;

    @Test
    void autoDeliverOldConfirmedOrders_shouldDoNothing_whenNoOldConfirmedOrders() {
        when(orderRepository.findAllByOrderStatusAndCreatedOnBefore(
                eq(OrderStatus.CONFIRMED),
                any(LocalDateTime.class)))
                .thenReturn(List.of());

        scheduler.autoDeliverOldConfirmedOrders();

        verify(orderRepository, never()).saveAll(any());
    }

    @Test
    void autoDeliverOldConfirmedOrders_shouldMarkOrdersAsDelivered_andSave() {
        Order o1 = new Order();
        o1.setOrderStatus(OrderStatus.CONFIRMED);
        Order o2 = new Order();
        o2.setOrderStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findAllByOrderStatusAndCreatedOnBefore(
                eq(OrderStatus.CONFIRMED),
                any(LocalDateTime.class)))
                .thenReturn(List.of(o1, o2));

        scheduler.autoDeliverOldConfirmedOrders();

        assertThat(o1.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(o2.getOrderStatus()).isEqualTo(OrderStatus.DELIVERED);

        ArgumentCaptor<List<Order>> captor = ArgumentCaptor.forClass(List.class);
        verify(orderRepository).saveAll(captor.capture());

        List<Order> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved)
                .allMatch(o -> o.getOrderStatus() == OrderStatus.DELIVERED);
    }
}
