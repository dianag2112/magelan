package bg.softuni.magelan.payment;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(
        name = "paymentClient",
        url = "${paymentsvc.url}/api/v1/payments"
)
public interface PaymentClient {

    @PostMapping
    PaymentResponse createPayment(@RequestBody PaymentRequest request);

    @GetMapping("/{paymentId}")
    PaymentResponse getPaymentById(@PathVariable("paymentId") UUID paymentId);

    @PostMapping("/{paymentId}/process")
    PaymentResponse processPayment(@PathVariable("paymentId") UUID paymentId);

    @GetMapping("/order/{orderId}")
    PaymentResponse getPaymentByOrderId(@PathVariable("orderId") UUID orderId);
}
