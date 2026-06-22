package sc.liste.noel.account.exception;

public class MailServiceDisabledException extends Exception {

    public MailServiceDisabledException(String errorMessage) {
        super(errorMessage);
    }

}
