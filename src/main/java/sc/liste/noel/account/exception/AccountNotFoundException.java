package sc.liste.noel.account.exception;

public class AccountNotFoundException extends Exception {

    public AccountNotFoundException(String errorMessage) {
        super(errorMessage);
    }

}
