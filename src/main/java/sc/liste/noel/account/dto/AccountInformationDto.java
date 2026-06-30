package sc.liste.noel.account.dto;

public class AccountInformationDto {

    private String email;
    private String pseudo;
    private String lastLoginDate;
    private String lastPasswordChangeDate;

    /**
     * Empty constructeur
     */
    public AccountInformationDto() {
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

    public String getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(String lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public String getLastPasswordChangeDate() {
        return lastPasswordChangeDate;
    }

    public void setLastPasswordChangeDate(String lastPasswordChangeDate) {
        this.lastPasswordChangeDate = lastPasswordChangeDate;
    }
}
