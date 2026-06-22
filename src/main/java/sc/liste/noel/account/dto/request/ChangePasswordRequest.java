package sc.liste.noel.account.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChangePasswordRequest(@JsonProperty("ancienMotDePasse") String oldPassword,
                                    @JsonProperty("nouveauMotDePasse") String newPassword,
                                    @JsonProperty("confirmationMotDePasse") String confirmPassword) {
}
