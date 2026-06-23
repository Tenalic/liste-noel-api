package sc.liste.noel.giftlist.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateGiftListRequest(@JsonProperty("nomListe") @NotBlank(message = "name vide") String name,
                                    @JsonProperty("publique") @NotNull(message = "isPublic null") Boolean isPublic) {
}
