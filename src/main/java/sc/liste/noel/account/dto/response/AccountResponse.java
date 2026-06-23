package sc.liste.noel.account.dto.response;

import sc.liste.noel.common.dto.response.GenericResponse;

public class AccountResponse extends GenericResponse {

    private static final long serialVersionUID = 1L;

    private String email;

    private String pseudo;

    public AccountResponse() {
    }

    public AccountResponse(String email) {
        this.email = email;
    }

    public AccountResponse(String email, String returnMessage, int returnCode) {
        super(returnMessage, returnCode);
        this.email = email;
    }

    public AccountResponse(String returnMessage, int returnCode) {
        super(returnMessage, returnCode);
    }

    public AccountResponse(String email, String pseudo, String returnMessage, int returnCode) {
        super(returnMessage, returnCode);
        this.email = email;
        this.pseudo = pseudo;
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
}
