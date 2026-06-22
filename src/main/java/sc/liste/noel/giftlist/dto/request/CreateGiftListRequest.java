package sc.liste.noel.giftlist.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateGiftListRequest(@JsonProperty("nomListe") String name,
                                    @JsonProperty("publique") Boolean isPublic) {
}
