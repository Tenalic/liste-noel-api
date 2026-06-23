package sc.liste.noel.account.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @JsonProperty("ancienMotDePasse") @NotBlank(message = "{password.required}") String oldPassword,
        @JsonProperty("nouveauMotDePasse") @NotBlank(message = "{password.required}") String newPassword,
        @JsonProperty("confirmationMotDePasse") @NotBlank(message = "{password.required}") String confirmPassword) {
}
