package sc.liste.noel.account.exception;

public class AccountAlreadyExistsException extends Exception {

    public AccountAlreadyExistsException(String errorMessage) {
        super(errorMessage);
    }

}
