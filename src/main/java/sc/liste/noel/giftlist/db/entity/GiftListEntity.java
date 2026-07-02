package sc.liste.noel.giftlist.db.entity;

import jakarta.persistence.*;
import sc.liste.noel.gift.db.entity.GiftEntity;

import java.util.List;

@Entity
@Table(name = "liste")
public class GiftListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_liste")
    private Long giftListId;
    @Column(name = "share_token", unique = true, nullable = false, updatable = false)
    private String shareToken;
    @Column(name = "email")
    private String owner;
    @Column(name = "nom_liste")
    private String name;
    @OneToMany(mappedBy = "giftListId", cascade = CascadeType.ALL)
    private List<GiftEntity> gifts;
    @Column(name = "publique")
    private Boolean isPublic;

    public GiftListEntity() {
    }

    public Long getGiftListId() {
        return giftListId;
    }

    public void setGiftListId(Long giftListId) {
        this.giftListId = giftListId;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<GiftEntity> getGifts() {
        return gifts;
    }

    public void setGifts(List<GiftEntity> gifts) {
        this.gifts = gifts;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getShareToken() {
        return shareToken;
    }

    public void setShareToken(String shareToken) {
        this.shareToken = shareToken;
    }
}
