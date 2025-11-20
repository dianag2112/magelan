package bg.softuni.magelan.payment;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRequest {
    private UUID orderId;
    private BigDecimal amount;
    private String method;
}
