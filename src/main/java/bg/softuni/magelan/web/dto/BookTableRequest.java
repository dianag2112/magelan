package bg.softuni.magelan.web.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookTableRequest {

    @NotNull
    @FutureOrPresent(message = "Date must be today or in the future.")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime time;

    @NotNull
    @Min(value = 1, message = "At least 1 guest.")
    @Max(value = 20, message = "Maximum 20 guests per booking.")
    private Integer guests;

    @NotBlank(message = "Phone number is required")
    @Size(max = 20, message = "The phone number must be up to 20 symbols")
    private String phone;

    @Size(max = 500, message = "Notes must be up to 500 symbols.")
    private String notes;
}
