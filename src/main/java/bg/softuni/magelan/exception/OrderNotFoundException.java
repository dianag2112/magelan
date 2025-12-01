package bg.softuni.magelan.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class OrderNotFoundException extends RuntimeException {

    private final UUID orderId;

    public OrderNotFoundException(UUID orderId) {
        super("Order with ID [%s] was not found.".formatted(orderId));
        this.orderId = orderId;
    }

}
