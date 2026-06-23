package sc.liste.noel.account.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import static sc.liste.noel.common.constant.Constants.API_ACCOUNT_EMAIL_REQUIRED_KEY;
import static sc.liste.noel.common.constant.Constants.API_ACCOUNT_PASSWORD_REQUIRED;

public record LoginRequest(
        @NotBlank(message = API_ACCOUNT_EMAIL_REQUIRED_KEY)
        @Email(message = "{email.required}")
        String email,

        @NotBlank(message = API_ACCOUNT_PASSWORD_REQUIRED)
        String password
) {}
