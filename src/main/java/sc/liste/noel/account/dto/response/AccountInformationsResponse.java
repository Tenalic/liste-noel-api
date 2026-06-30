package sc.liste.noel.account.dto.response;

public class AccountInformationsResponse extends AccountResponse {

    private String lastLoginDate;
    private String lastPasswordChangeDate;

    public AccountInformationsResponse(String email, String returnMessage, int returnCode) {
        super(email, returnMessage, returnCode);
    }

    public AccountInformationsResponse(String email, String pseudo, String returnMessage, int returnCode, String lastLoginDate, String lastPasswordChangeDate) {
        super(email, pseudo, returnMessage, returnCode);
        this.lastLoginDate = lastLoginDate;
        this.lastPasswordChangeDate = lastPasswordChangeDate;
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
