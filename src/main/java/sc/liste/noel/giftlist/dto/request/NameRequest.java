package sc.liste.noel.giftlist.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record NameRequest(@JsonProperty("nomListe") @NotBlank String listName) {
}
