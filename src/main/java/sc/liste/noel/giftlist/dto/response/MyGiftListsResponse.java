package sc.liste.noel.giftlist.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import sc.liste.noel.giftlist.dto.GiftListDto;
import sc.liste.noel.common.dto.response.GenericResponse;

import java.util.List;

public class MyGiftListsResponse extends GenericResponse {
    @JsonProperty("listes")
    private List<GiftListDto> giftLists;
    @JsonProperty("favoris")
    private List<GiftListDto> favorites;

    public MyGiftListsResponse() {
    }

    public MyGiftListsResponse(List<GiftListDto> giftLists, List<GiftListDto> favorites) {
        super("Succès", 0); // 0 pour OK selon ta convention
        this.giftLists = giftLists;
        this.favorites = favorites;
    }

    public List<GiftListDto> getGiftLists() {
        return giftLists;
    }

    public void setGiftLists(List<GiftListDto> giftLists) {
        this.giftLists = giftLists;
    }

    public List<GiftListDto> getFavorites() {
        return favorites;
    }

    public void setFavorites(List<GiftListDto> favorites) {
        this.favorites = favorites;
    }
}
