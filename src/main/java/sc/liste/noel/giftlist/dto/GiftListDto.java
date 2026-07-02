package sc.liste.noel.giftlist.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import sc.liste.noel.gift.dto.GiftDto;

import java.io.Serializable;
import java.util.List;

public class GiftListDto implements Serializable {

    @JsonProperty("urlPartage")
    private String shareUrl;

    @JsonProperty("idListe")
    private Long giftListId;

    @JsonProperty("nomListe")
    private String name;

    @JsonProperty("listeObjet")
    private List<GiftDto> gifts;

    @JsonProperty("nombreObjet")
    private int giftCount;

    @JsonProperty("proprietaire")
    private String owner;

    @JsonProperty("pseudoProprietaire")
    private String ownerPseudo;

    private String shareToken;

    private boolean isPublic;


    public List<GiftDto> getGifts() {
        return gifts;
    }

    public void setGifts(List<GiftDto> gifts) {
        this.gifts = gifts;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public Long getGiftListId() {
        return giftListId;
    }

    public void setGiftListId(Long giftListId) {
        this.giftListId = giftListId;
    }

    public String getShareUrl() {
        return shareUrl;
    }

    public void setShareUrl(String shareUrl) {
        this.shareUrl = shareUrl;
    }

    public String getOwnerPseudo() {
        return ownerPseudo;
    }

    public void setOwnerPseudo(String ownerPseudo) {
        this.ownerPseudo = ownerPseudo;
    }

    @JsonProperty("publique")
    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public int getGiftCount() {
        return giftCount;
    }

    public void setGiftCount(int giftCount) {
        this.giftCount = giftCount;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }
}
