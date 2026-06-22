package sc.liste.noel.account.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RegistrationRequest(String pseudo,
                                  String email,
                                  String password,
                                  String confirmPassword,
                                  @JsonProperty("acceptCGU") Boolean acceptTerms) {
}
