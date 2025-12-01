package bg.softuni.magelan.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class UserNotFoundException extends RuntimeException {

    private final UUID userId;

    public UserNotFoundException(UUID userId) {
        super("User with ID [%s] was not found.".formatted(userId));
        this.userId = userId;
    }

}

