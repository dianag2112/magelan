package bg.softuni.magelan.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class BookingNotFoundException extends RuntimeException {

    private final UUID bookingId;

    public BookingNotFoundException(UUID bookingId) {
        super("Booking with ID [%s] was not found.".formatted(bookingId));
        this.bookingId = bookingId;
    }

}

