package bg.softuni.magelan.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import bg.softuni.magelan.web.validation.PasswordMatches;
import bg.softuni.magelan.web.validation.UniqueUsername;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@PasswordMatches
public class RegisterRequest {

    @UniqueUsername
    @NotBlank
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 symbols.")
    private String username;

    @NotBlank
    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 symbols.")
    private String password;

    @NotBlank
    @Size(min = 6, max = 20, message = "Password must be between 6 and 20 symbols.")
    private String confirmPassword;
}
