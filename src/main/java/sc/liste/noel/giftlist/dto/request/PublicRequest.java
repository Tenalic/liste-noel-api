package sc.liste.noel.giftlist.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PublicRequest(@JsonProperty("publique") boolean isPublic) {
}
