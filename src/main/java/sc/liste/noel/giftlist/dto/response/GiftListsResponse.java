package sc.liste.noel.giftlist.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import sc.liste.noel.giftlist.dto.GiftListDto;
import sc.liste.noel.common.dto.response.GenericResponse;

import java.util.List;

public class GiftListsResponse extends GenericResponse {

    @JsonProperty("lisOfListesCadeaux")
    private List<GiftListDto> giftLists;

    public GiftListsResponse(String returnMessage, int returnCode) {
        super(returnMessage, returnCode);
    }

    public GiftListsResponse(String returnMessage, int returnCode, List<GiftListDto> giftLists) {
        super(returnMessage, returnCode);
        this.giftLists = giftLists;
    }

    public List<GiftListDto> getGiftLists() {
        return giftLists;
    }

    public void setGiftLists(List<GiftListDto> giftLists) {
        this.giftLists = giftLists;
    }
}
