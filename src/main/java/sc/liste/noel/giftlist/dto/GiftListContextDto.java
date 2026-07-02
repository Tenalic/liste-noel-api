package sc.liste.noel.giftlist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GiftListContextDto extends GiftListDto {

    @JsonProperty("estProprietaire")
    private boolean ownedByCurrentUser;
    @JsonProperty("estFavoris")
    private boolean favorite;

    public GiftListContextDto() {
    }

    public GiftListContextDto(GiftListDto giftList) {
        this.setGiftListId(giftList.getGiftListId());
        this.setName(giftList.getName());
        this.setOwner(giftList.getOwner());
        this.setGifts(giftList.getGifts());
        this.setShareUrl(giftList.getShareUrl());
        this.setOwnerPseudo(giftList.getOwnerPseudo());
        this.setPublic(giftList.isPublic());
        this.setGiftCount(giftList.getGiftCount());
        this.setShareToken(giftList.getShareToken());
    }

    public boolean isOwnedByCurrentUser() {
        return ownedByCurrentUser;
    }

    public void setOwnedByCurrentUser(boolean ownedByCurrentUser) {
        this.ownedByCurrentUser = ownedByCurrentUser;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }
}
