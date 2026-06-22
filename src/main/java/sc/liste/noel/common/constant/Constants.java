package sc.liste.noel.common.constant;

public class Constants {

    public static final int API_RETURN_OK = 0;

    public static final int API_RETURN_KO = 1;

    public static final String FORGOT_PASSWORD_P1_KEY = "motDePasseOublie_P1";
    public static final String FORGOT_PASSWORD_P2_KEY = "motDePasseOublie_P2";

    // Success messages
    public static final String API_ACCOUNT_CREATION_SUCCESS_KEY = "api.compte.creation.succes";
    public static final String API_ACCOUNT_DELETION_SUCCESS_KEY = "api.compte.suppression.succes";
    public static final String API_ACCOUNT_PASSWORD_UPDATE_SUCCESS_KEY = "api.compte.password.update.success";

    // Error messages (required fields)
    public static final String API_ACCOUNT_EMAIL_REQUIRED_KEY = "api.compte.email.obligatoire";
    public static final String API_ACCOUNT_PASSWORD_REQUIRED = "api.compte.password.obligatoire";

    // Error messages (technical)
    public static final String API_GIFTLIST_ERROR_KEY = "api.liste.erreur";


    // Error messages (business logic)
    public static final String API_ACCOUNT_PASSWORD_DIFFERENT_KEY = "api.compte.password.different";
    public static final String API_ACCOUNT_DELETION_FAILURE_KEY = "api.compte.suppression.echec";
    public static final String API_ACCOUNT_ACTIVATION_SUCCESS_KEY = "api.compte.activation.succes";
    public static final String API_ACCOUNT_ACTIVATION_FAILURE_KEY = "api.compte.activation.echec";
    public static final String API_SECRET_INVALID_KEY = "api.secret.invalid";
    public static final String API_ERROR_GENERIC_KEY = "api.error.generic";
    public static final String ACCOUNT_EXISTS_KEY = "compteExiste";
    public static final String PSEUDO_EXISTS_KEY = "pseudoExiste";
    public static final String ACCOUNT_ERROR_KEY = "compteError";
    public static final String LOGIN_FAIL_KEY = "connexionFail";
    public static final String EMAIL_DISABLED = "api.compte.mail.desactive";
    public static final String PASSWORD_CHANGE_INCORRECT = "api.compte.mdp.change.incorecte";
    public static final String PASSWORD_CHANGE_NOT_FOUND = "api.compte.mdp.change.introuvable";
    public static final String ACCOUNT_NOT_FOUND = "api.compte.introuvable";
    public static final String GIFTLIST_NOT_FOUND = "api.liste.notfound";
    public static final String MODIFICATION_FORBIDDEN = "api.modification.interdit";
    public static final String DELETION_FORBIDDEN = "api.liste.suppression.interdit";


}
