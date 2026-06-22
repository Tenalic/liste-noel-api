package sc.liste.noel.gift.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class GiftDto implements Serializable {

    @JsonProperty("idObjet")
    private Long giftId;

    @JsonProperty("titre")
    private String title;

    private String description;

    private String url;

    @JsonProperty("estPrit")
    private Boolean taken;

    @JsonProperty("detenteur")
    private String holder;

    @JsonProperty("pseudoDetenteur")
    private String holderPseudo;

    @JsonProperty("priorite")
    private String priorityLabel;
    @JsonProperty("valuePriorite")
    private Integer priorityValue;

    public GiftDto() {
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

    public Long getGiftId() {
        return giftId;
    }

    public void setGiftId(Long giftId) {
        this.giftId = giftId;
    }

    public String getHolderPseudo() {
        return holderPseudo;
    }

    public void setHolderPseudo(String holderPseudo) {
        this.holderPseudo = holderPseudo;
    }

    public String getPriorityLabel() {
        return priorityLabel;
    }

    public void setPriorityLabel(String priorityLabel) {
        this.priorityLabel = priorityLabel;
    }

    public Integer getPriorityValue() {
        return priorityValue;
    }

    public void setPriorityValue(Integer priorityValue) {
        this.priorityValue = priorityValue;
    }
}
