package sc.liste.noel.account.dto;

import sc.liste.noel.giftlist.dto.GiftListDto;

import java.util.List;

public class AccountDto {

    private String email;
    private String pseudo;

    private List<GiftListDto> giftLists;

    public AccountDto() {
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public List<GiftListDto> getGiftLists() {
        return giftLists;
    }

    public void setGiftLists(List<GiftListDto> giftLists) {
        this.giftLists = giftLists;
    }
}
