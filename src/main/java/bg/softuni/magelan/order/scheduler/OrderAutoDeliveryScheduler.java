package bg.softuni.magelan.order.scheduler;

import bg.softuni.magelan.order.model.Order;
import bg.softuni.magelan.order.model.OrderStatus;
import bg.softuni.magelan.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderAutoDeliveryScheduler {

    private final OrderRepository orderRepository;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void autoDeliverOldConfirmedOrders() {

        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);

        List<Order> oldConfirmedOrders =
                orderRepository.findAllByOrderStatusAndCreatedOnBefore(
                        OrderStatus.CONFIRMED,
                        cutoff
                );

        if (oldConfirmedOrders.isEmpty()) {
            log.debug("No CONFIRMED orders older than 1 hour to auto-deliver.");
            return;
        }

        log.info("Auto-marking {} CONFIRMED orders as DELIVERED (older than 1 hour).",
                oldConfirmedOrders.size());

        oldConfirmedOrders.forEach(order -> order.setOrderStatus(OrderStatus.DELIVERED));
        orderRepository.saveAll(oldConfirmedOrders);

        log.info("Successfully auto-delivered {} orders.", oldConfirmedOrders.size());
    }
}
