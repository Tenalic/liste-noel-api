package sc.liste.noel.gift.db.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "objet")
public class GiftEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_objet")
    private Long giftId;
    @Column(name = "id_liste")
    private Long giftListId;
    @Column(name = "titre")
    private String title;
    @Column(name = "description")
    private String description;
    @Column(name = "url")
    private String url;
    @Column(name = "est_prit")
    private Boolean taken;
    @Column(name = "detenteur")
    private String holder;
    @Column(name = "pseudo_detenteur")
    private String holderPseudo;
    @Column(name = "priorite", nullable = false)
    private Integer priorityValue;

    public GiftEntity() {
    }

    public Long getGiftId() {
        return giftId;
    }

    public void setGiftId(Long giftId) {
        this.giftId = giftId;
    }

    public Long getGiftListId() {
        return giftListId;
    }

    public void setGiftListId(Long giftListId) {
        this.giftListId = giftListId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Boolean getTaken() {
        return taken;
    }

    public void setTaken(Boolean taken) {
        this.taken = taken;
    }

    public String getHolder() {
        return holder;
    }

    public void setHolder(String holder) {
        this.holder = holder;
    }

    public String getHolderPseudo() {
        return holderPseudo;
    }

    public void setHolderPseudo(String holderPseudo) {
        this.holderPseudo = holderPseudo;
    }

    public Integer getPriorityValue() {
        return priorityValue;
    }

    public void setPriorityValue(Integer priorityValue) {
        this.priorityValue = priorityValue;
    }
}
