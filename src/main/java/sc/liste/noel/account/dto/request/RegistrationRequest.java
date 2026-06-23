package sc.liste.noel.account.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationRequest(@NotBlank(message = "{pseudo.required}") String pseudo,
                                  @NotBlank(message = "{email.required}") @Email(message = "{email.invalid}") String email,
                                  @NotBlank(message = "{password.required}") String password,
                                  String confirmPassword,
                                  @JsonProperty("acceptCGU") Boolean acceptTerms) {
}
