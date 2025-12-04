package bg.softuni.magelan.order.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderSubmittedListener {

    @EventListener
    public void handleOrderSubmitted(OrderSubmittedEvent event) {
        log.info("OrderSubmittedEvent received: orderId={}, userId={}, amount={}",
                event.orderId(), event.userId(), event.amount());
    }
}
