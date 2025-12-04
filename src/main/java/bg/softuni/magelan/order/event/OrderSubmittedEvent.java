package bg.softuni.magelan.order.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record OrderSubmittedEvent(
        UUID orderId,
        UUID userId,
        BigDecimal amount,
        LocalDateTime createdOn
) {
}
