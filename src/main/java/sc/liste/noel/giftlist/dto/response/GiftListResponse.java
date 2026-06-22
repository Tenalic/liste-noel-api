package sc.liste.noel.giftlist.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import sc.liste.noel.giftlist.dto.GiftListDto;
import sc.liste.noel.common.dto.response.GenericResponse;

public class GiftListResponse extends GenericResponse {

    @JsonProperty("listeCadeaux")
    private GiftListDto giftList;
    @JsonProperty("estProprietaire")
    private boolean ownedByCurrentUser;
    @JsonProperty("estEnFavoris")
    private boolean inFavorites;

    public GiftListResponse(String returnMessage, int returnCode) {
        super(returnMessage, returnCode);
    }

    public GiftListResponse(String returnMessage, int returnCode, GiftListDto giftList) {
        super(returnMessage, returnCode);
        this.giftList = giftList;
    }

    public GiftListResponse(String returnMessage, int returnCode, GiftListDto giftList, boolean ownedByCurrentUser, boolean inFavorites) {
        super(returnMessage, returnCode);
        this.giftList = giftList;
        this.ownedByCurrentUser = ownedByCurrentUser;
        this.inFavorites = inFavorites;
    }

    public GiftListDto getGiftList() {
        return giftList;
    }

    public void setGiftList(GiftListDto giftList) {
        this.giftList = giftList;
    }

    public boolean isOwnedByCurrentUser() {
        return ownedByCurrentUser;
    }

    public void setOwnedByCurrentUser(boolean ownedByCurrentUser) {
        this.ownedByCurrentUser = ownedByCurrentUser;
    }

    public boolean isInFavorites() {
        return inFavorites;
    }

    public void setInFavorites(boolean inFavorites) {
        this.inFavorites = inFavorites;
    }
}
