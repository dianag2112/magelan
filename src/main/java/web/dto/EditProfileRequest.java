package web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.validator.constraints.URL;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EditProfileRequest {
    @Size(min = 2, max = 20)
    private String firstName;

    @Size(min = 2, max = 20)
    private String lastName;

    @Email
    private String email;

    @Size(min = 6, max = 20, message = "Phone number must be between 6 and 20 symbols.")
    private String phoneNumber;

    @Size(max = 100, message = "Address must be up to 100 symbols.")
    private String address;

    @URL
    private String profilePictureUrl;
}
