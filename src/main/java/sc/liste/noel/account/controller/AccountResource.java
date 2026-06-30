package sc.liste.noel.account.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sc.liste.noel.account.dto.AccountDto;
import sc.liste.noel.account.dto.AccountInformationDto;
import sc.liste.noel.account.dto.request.ChangePasswordRequest;
import sc.liste.noel.account.dto.request.ForgotPasswordRequest;
import sc.liste.noel.account.dto.request.LoginRequest;
import sc.liste.noel.account.dto.request.RegistrationRequest;
import sc.liste.noel.account.dto.response.AccountInformationsResponse;
import sc.liste.noel.account.dto.response.AccountResponse;
import sc.liste.noel.account.exception.*;
import sc.liste.noel.account.service.AccountService;
import sc.liste.noel.account.service.JwtService;
import sc.liste.noel.account.service.SecretService;
import sc.liste.noel.common.constant.Constants;
import sc.liste.noel.common.dto.response.GenericResponse;
import sc.liste.noel.common.service.MessageService;

import java.security.Principal;
import java.time.Duration;
import java.util.Locale;

import static sc.liste.noel.common.constant.Constants.*;

/**
 * Contrôleur REST gérant les opérations liées aux comptes utilisateurs.
 *
 * <p>Expose les endpoints sous le préfixe {@code /api/compte} et couvre :
 * l'inscription, la connexion, la déconnexion, la gestion des mots de passe,
 * la récupération du profil courant, la suppression et l'activation de compte.</p>
 *
 * <p>L'authentification repose sur un cookie HTTP-only contenant un JWT,
 * ce qui protège le token contre les attaques XSS.</p>
 */
@Tag(name = "Comptes utilisateurs", description = "Inscription, connexion, gestion du mot de passe et du profil")
@RestController
@RequestMapping("/api/compte")
public class AccountResource {

    private static final Logger LOGGER = LogManager.getLogger(AccountResource.class);

    private final AccountService accountService;

    private final SecretService secretService;

    private final MessageService messageService;

    private final JwtService jwtService;

    /**
     * Nom du cookie d'authentification posé et lu par le navigateur.
     */
    private static final String AUT_TOKEN_KEY = "auth-token";

    /**
     * Construit le contrôleur avec ses dépendances.
     *
     * @param accountService service métier de gestion des comptes
     * @param secretService  service de vérification du secret d'administration
     * @param messageService service d'internationalisation des messages
     * @param jwtService     service de génération et validation des JWT
     */
    public AccountResource(AccountService accountService, SecretService secretService, MessageService messageService, JwtService jwtService) {
        this.accountService = accountService;
        this.secretService = secretService;
        this.messageService = messageService;
        this.jwtService = jwtService;
    }

    /**
     * Inscrit un nouvel utilisateur et initie sa session via un cookie JWT.
     *
     * <p>En cas de succès, un cookie HTTP-only {@code auth-token} est posé avec
     * une durée de vie de 24 heures. Un email de confirmation d'activation est
     * envoyé à l'adresse fournie.</p>
     *
     * @param registrationRequest corps de la requête contenant l'email, le mot de passe,
     *                            sa confirmation, le pseudo et l'acceptation des CGU
     * @param locale              locale issue de l'en-tête {@code Accept-Language},
     *                            utilisée pour l'internationalisation des messages de retour
     * @return {@code 200 OK} avec le cookie et les informations du compte créé ;
     * {@code 409 Conflict} si l'email ou le pseudo est déjà utilisé ;
     * {@code 403 Forbidden} si les CGU ne sont pas acceptées ou si les mots de passe
     * ne correspondent pas
     */
    @Operation(summary = "Inscrire un nouvel utilisateur",
            description = "Crée un compte, pose un cookie JWT HTTP-only et envoie un email d'activation.")
    @ApiResponse(responseCode = "200", description = "Compte créé, cookie posé")
    @ApiResponse(responseCode = "403", description = "CGU non acceptées ou mots de passe différents")
    @ApiResponse(responseCode = "409", description = "Email ou pseudo déjà utilisé")
    @PostMapping("/inscription")
    public ResponseEntity<AccountResponse> register(
            @RequestBody @Valid RegistrationRequest registrationRequest,
            @Parameter(description = "Locale pour les messages de retour (défaut : fr)", example = "fr")
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "fr")
            Locale locale) {
        try {
            String email = accountService.createAccount(registrationRequest.email(), registrationRequest.password(), registrationRequest.confirmPassword(), registrationRequest.acceptTerms(), registrationRequest.pseudo());

            String token = jwtService.generateToken(email);

            // HTTP-only cookie: JavaScript CANNOT read it → protected against XSS
            ResponseCookie cookie = ResponseCookie.from(AUT_TOKEN_KEY, token)
                    .httpOnly(true)                     // not accessible from JS
                    .secure(true)                       // HTTPS only (false in local dev)
                    .path("/")                          // valid on all routes
                    .maxAge(Duration.ofSeconds(86400))  // 24h, same as jwt.expiration
                    .sameSite("None")                 // CSRF protection
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new AccountResponse(registrationRequest.email(), registrationRequest.pseudo(), messageService.getMessage(Constants.API_ACCOUNT_CREATION_SUCCESS_KEY, locale), Constants.API_RETURN_OK));
        } catch (AccountAlreadyExistsException e) {
            LOGGER.warn("Account already exists {}", registrationRequest.email());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AccountResponse(registrationRequest.email(), messageService.getMessage(ACCOUNT_EXISTS_KEY, locale), Constants.API_RETURN_KO));
        } catch (PseudoAlreadyExistsException e) {
            LOGGER.warn("Pseudo already exists {}", registrationRequest.pseudo());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AccountResponse(registrationRequest.pseudo(), messageService.getMessage(PSEUDO_EXISTS_KEY, locale), Constants.API_RETURN_KO));
        } catch (TermsNotAcceptedException e) {
            LOGGER.warn("CGU not accepted {} {}", registrationRequest.email(), registrationRequest.pseudo());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AccountResponse(messageService.getMessage(CGU, locale), Constants.API_RETURN_KO));
        } catch (PasswordNotEqualsException e) {
            LOGGER.warn("Password not equals {} {}", registrationRequest.email(), registrationRequest.pseudo());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new AccountResponse(messageService.getMessage(PASSWORD, locale), Constants.API_RETURN_KO));
        } catch (Exception e) {
            LOGGER.error("Error while creating the account for the email: {}", registrationRequest.email(), e);
            throw e;
        }
    }

    /**
     * Authentifie un utilisateur et pose un cookie JWT HTTP-only.
     *
     * <p>Le cookie {@code auth-token} est valide 24 heures, accessible sur toutes
     * les routes ({@code path="/"}) et uniquement transmis en HTTPS.</p>
     *
     * @param loginRequest corps de la requête contenant l'email et le mot de passe
     * @param locale       locale courante pour l'internationalisation des messages
     * @return {@code 200 OK} avec le cookie et les informations du compte ;
     * {@code 404 Not Found} si les identifiants sont incorrects ;
     * {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Connecter un utilisateur",
            description = "Vérifie les identifiants et pose un cookie JWT HTTP-only valide 24 heures.")
    @ApiResponse(responseCode = "200", description = "Connexion réussie, cookie posé")
    @ApiResponse(responseCode = "404", description = "Identifiants incorrects")
    @ApiResponse(responseCode = "500", description = "Erreur interne")
    @PostMapping("/connexion")
    public ResponseEntity<AccountResponse> login(@RequestBody @Valid LoginRequest loginRequest,
                                                 Locale locale) {
        String email = loginRequest.email();

        LOGGER.info("Entering login service: {}", email);

        try {
            AccountDto account = accountService.login(email, loginRequest.password());

            String token = jwtService.generateToken(account.getEmail());

            // HTTP-only cookie: JavaScript CANNOT read it → protected against XSS
            ResponseCookie cookie = ResponseCookie.from(AUT_TOKEN_KEY, token)
                    .httpOnly(true)                     // not accessible from JS
                    .secure(true)                       // HTTPS only (false in local dev)
                    .path("/")                          // valid on all routes
                    .maxAge(Duration.ofSeconds(86400))  // 24h, same as jwt.expiration
                    .sameSite("None")                 // CSRF protection
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new AccountResponse(account.getEmail(), account.getPseudo(), "Connexion réussie", API_RETURN_OK));

        } catch (AccountNotFoundException exception) {
            LOGGER.warn("[connexion] account not found {}", loginRequest.email());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AccountResponse(messageService.getMessage(ACCOUNT_NOT_FOUND, locale), Constants.API_RETURN_KO));

        } catch (Exception e) {
            LOGGER.error("Error during login: {}", email, e);
            return ResponseEntity.internalServerError()
                    .body(new AccountResponse(email, messageService.getMessage(API_ERROR_GENERIC_KEY, locale), Constants.API_RETURN_KO));
        }
    }

    /**
     * Déconnecte l'utilisateur en invalidant le cookie d'authentification côté navigateur.
     *
     * <p>L'invalidation est réalisée en réécrivant le cookie {@code auth-token} avec
     * un {@code maxAge} à {@code 0}, ce qui ordonne au navigateur de le supprimer
     * immédiatement. Aucune liste noire de JWT n'est maintenue côté serveur.</p>
     *
     * @return {@code 200 OK} sans corps, avec l'en-tête {@code Set-Cookie} qui efface le cookie
     */
    @Operation(summary = "Déconnecter l'utilisateur",
            description = "Invalide le cookie JWT côté navigateur en le réécrivant avec maxAge=0. Aucune liste noire côté serveur.")
    @ApiResponse(responseCode = "200", description = "Cookie invalidé, utilisateur déconnecté")
    @PostMapping("/deconnexion")
    public ResponseEntity<Void> logout() {
        // Overwrite the cookie with maxAge=0 → the browser deletes it immediately
        ResponseCookie cookie = ResponseCookie.from(AUT_TOKEN_KEY, "")
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(0)          // ← this is what deletes the cookie
                .sameSite("None")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    /**
     * Génère un nouveau mot de passe et l'envoie par email à l'utilisateur.
     *
     * <p>Le mot de passe généré remplace immédiatement l'ancien en base.
     * L'utilisateur est invité à le modifier une fois reconnecté.
     * Si le service d'envoi d'email est désactivé, la demande est rejetée.</p>
     *
     * @param forgotPasswordRequest corps de la requête contenant l'adresse email du compte
     * @param locale                locale courante pour l'internationalisation des messages
     * @return {@code 200 OK} avec un message de confirmation d'envoi ;
     * {@code 403 Forbidden} si le service mail est désactivé ;
     * {@code 404 Not Found} si aucun compte ne correspond à l'email ;
     * {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Mot de passe oublié",
            description = "Génère un nouveau mot de passe et l'envoie par email. Nécessite que le service mail soit actif.")
    @ApiResponse(responseCode = "200", description = "Nouveau mot de passe envoyé par email")
    @ApiResponse(responseCode = "403", description = "Service mail désactivé")
    @ApiResponse(responseCode = "404", description = "Compte introuvable")
    @ApiResponse(responseCode = "500", description = "Erreur interne")
    @PostMapping("/mot-de-passe-oublie")
    public ResponseEntity<GenericResponse> forgotPassword(@RequestBody @Valid ForgotPasswordRequest forgotPasswordRequest, Locale locale) {
        try {
            accountService.generateAndSendPassword(forgotPasswordRequest.email());
            return ResponseEntity.ok().body(new GenericResponse(messageService.getMessage(FORGOT_PASSWORD_P1_KEY, locale) + forgotPasswordRequest.email()
                    + " " + messageService.getMessage(FORGOT_PASSWORD_P2_KEY, locale), Constants.API_RETURN_OK));
        } catch (MailServiceDisabledException e) {
            LOGGER.warn("Email sending is disabled, the password for account {} was not generated", forgotPasswordRequest.email());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new GenericResponse(messageService.getMessage(EMAIL_DISABLED, locale), Constants.API_RETURN_KO));
        } catch (AccountNotFoundException e) {
            LOGGER.warn("[mot-de-passe-oublie] account not found {}", forgotPasswordRequest.email());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new GenericResponse(messageService.getMessage(ACCOUNT_NOT_FOUND, locale), Constants.API_RETURN_KO));

        } catch (Exception e) {
            LOGGER.error("[mot-de-passe-oublie] An error occurred", e);
            return ResponseEntity.internalServerError()
                    .body(new GenericResponse(messageService.getMessage(API_ERROR_GENERIC_KEY, locale), Constants.API_RETURN_KO));
        }
    }

    /**
     * Met à jour le mot de passe de l'utilisateur authentifié.
     *
     * <p>L'utilisateur doit fournir son ancien mot de passe ainsi que le nouveau
     * et sa confirmation. L'email est récupéré depuis le {@link Principal} fourni
     * par Spring Security — il n'est donc pas accepté en paramètre de la requête.</p>
     *
     * @param changePasswordRequest corps de la requête contenant l'ancien mot de passe,
     *                              le nouveau et sa confirmation
     * @param principal             principal Spring Security représentant l'utilisateur connecté
     * @param locale                locale courante pour l'internationalisation des messages
     * @return {@code 200 OK} si le mot de passe a été mis à jour ;
     * {@code 401 Unauthorized} si l'ancien mot de passe est incorrect ;
     * {@code 400 Bad Request} si le nouveau mot de passe et sa confirmation
     * ne correspondent pas ;
     * {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Modifier le mot de passe",
            description = "Met à jour le mot de passe de l'utilisateur connecté après vérification de l'ancien.")
    @ApiResponse(responseCode = "200", description = "Mot de passe mis à jour")
    @ApiResponse(responseCode = "400", description = "Nouveau mot de passe et confirmation différents")
    @ApiResponse(responseCode = "401", description = "Ancien mot de passe incorrect")
    @ApiResponse(responseCode = "500", description = "Erreur interne")
    @PostMapping("/update-password")
    public ResponseEntity<AccountResponse> updatePassword(@RequestBody @Valid ChangePasswordRequest changePasswordRequest,
                                                          Principal principal,
                                                          Locale locale) {
        String email = principal.getName();

        try {
            accountService.updatePassword(email, changePasswordRequest.oldPassword(), changePasswordRequest.newPassword(), changePasswordRequest.confirmPassword());
            return ResponseEntity.ok(new AccountResponse(email, messageService.getMessage(API_ACCOUNT_PASSWORD_UPDATE_SUCCESS_KEY, locale), Constants.API_RETURN_OK));
        } catch (AccountNotFoundException exception) {
            LOGGER.warn("[update-password] Account not found, the password must be incorrect for the email: {}", email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new AccountResponse(email, messageService.getMessage(PASSWORD_CHANGE_NOT_FOUND, locale), Constants.API_RETURN_KO));
        } catch (PasswordException exception) {
            LOGGER.warn("[update-password] The passwords do not match for the email {}", email);
            return ResponseEntity.badRequest()
                    .body(new AccountResponse(email, messageService.getMessage(PASSWORD_CHANGE_INCORRECT, locale), Constants.API_RETURN_KO));
        } catch (Exception e) {
            LOGGER.error("[update-password] Error while updating the password for the email: {}", email, e);
            return ResponseEntity.internalServerError()
                    .body(new AccountResponse(email, messageService.getMessage(API_ERROR_GENERIC_KEY, locale), Constants.API_RETURN_KO));
        }
    }

    /**
     * Retourne les informations de session de l'utilisateur actuellement connecté.
     *
     * <p>Si le {@link Principal} est {@code null}, la requête est rejetée sans appel
     * au service métier. Dans le cas contraire, Spring Security a déjà validé le JWT
     * contenu dans le cookie, et l'email est extrait du principal.</p>
     *
     * @param principal principal Spring Security représentant l'utilisateur connecté,
     *                  ou {@code null} si la requête n'est pas authentifiée
     * @param locale    locale courante pour l'internationalisation des messages
     * @return {@code 200 OK} avec l'email et le pseudo de l'utilisateur ;
     * {@code 401 Unauthorized} si le principal est absent ;
     * {@code 404 Not Found} si le compte associé au principal est introuvable ;
     * {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Récupérer le profil de session",
            description = "Retourne l'email et le pseudo de l'utilisateur connecté. Renvoie 401 si non authentifié.")
    @ApiResponse(responseCode = "200", description = "Session active, profil retourné")
    @ApiResponse(responseCode = "401", description = "Non authentifié")
    @ApiResponse(responseCode = "404", description = "Compte introuvable")
    @ApiResponse(responseCode = "500", description = "Erreur interne")
    @GetMapping("/me")
    public ResponseEntity<AccountResponse> getMe(Principal principal, Locale locale) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        // If principal is not null, Spring Security has validated the cookie/token
        String email = principal.getName();
        String pseudo;
        try {
            pseudo = accountService.getPseudo(email);
        } catch (AccountNotFoundException e) {
            LOGGER.warn("The account {} was not found", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AccountResponse(email, messageService.getMessage(ACCOUNT_NOT_FOUND, locale), Constants.API_RETURN_KO));
        } catch (Exception e) {
            LOGGER.error("[getMe] An error occurred: {}", email, e);
            return ResponseEntity.internalServerError()
                    .body(new AccountResponse(email, messageService.getMessage(API_ERROR_GENERIC_KEY, locale), Constants.API_RETURN_KO));
        }
        return ResponseEntity.ok(new AccountResponse(email, pseudo, "Session active", API_RETURN_OK));
    }

    /**
     * Supprime définitivement un compte utilisateur.
     *
     * <p>Cet endpoint est protégé par un secret d'administration transmis dans
     * l'en-tête {@code secret}. Si le secret est invalide, la suppression est refusée
     * sans révéler si le compte existe ou non.</p>
     *
     * @param email  adresse email du compte à supprimer, validée par {@code @Email}
     * @param secret valeur secrète d'administration transmise dans l'en-tête {@code secret}
     * @param locale locale courante pour l'internationalisation des messages
     * @return {@code 200 OK} si le compte a été supprimé ;
     * {@code 401 Unauthorized} si le secret est invalide ;
     * {@code 404 Not Found} si aucun compte ne correspond à l'email
     */
    @Operation(summary = "Supprimer un compte utilisateur",
            description = "Suppression définitive protégée par un secret d'administration transmis dans l'en-tête 'secret'.")
    @ApiResponse(responseCode = "200", description = "Compte supprimé")
    @ApiResponse(responseCode = "401", description = "Secret invalide")
    @ApiResponse(responseCode = "404", description = "Compte introuvable")
    @DeleteMapping("/supprimer")
    public ResponseEntity<AccountResponse> deleteAccount(
            @Parameter(description = "Email du compte à supprimer", required = true)
            @RequestParam @NotBlank(message = "{email.required}") @Email(message = "{email.invalid}") String email,
            @Parameter(description = "Secret d'administration", required = true)
            @RequestHeader(value = "secret") String secret,
            Locale locale) {

        try {
            if (!secretService.verifySecret(secret)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AccountResponse(email, messageService.getMessage(API_SECRET_INVALID_KEY, locale), Constants.API_RETURN_KO));
            }

            if (!accountService.deleteAccount(email)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new AccountResponse(email, messageService.getMessage(API_ACCOUNT_DELETION_FAILURE_KEY, locale), Constants.API_RETURN_KO));
            }

            return ResponseEntity.ok(new AccountResponse(email, messageService.getMessage(API_ACCOUNT_DELETION_SUCCESS_KEY, locale), Constants.API_RETURN_OK));

        } catch (Exception e) {
            LOGGER.error("Error while deleting the account for the email: {}", email, e);
            throw e;
        }
    }

    /**
     * Active le compte d'un utilisateur à partir du lien envoyé par email lors de l'inscription.
     *
     * <p>Le lien contient l'email et une clé d'activation à usage unique. Une fois le compte
     * activé, la clé est invalidée en base et ne peut plus être réutilisée.</p>
     *
     * @param email  adresse email du compte, transmise en paramètre de requête
     * @param key    clé d'activation générée à l'inscription et transmise en paramètre de requête
     * @param locale locale courante pour l'internationalisation des messages
     * @return {@code 200 OK} avec un message de confirmation si l'activation réussit ;
     * {@code 400 Bad Request} si la clé est incorrecte ou le compte déjà activé
     * @throws AccountNotFoundException si aucun compte ne correspond à l'email fourni
     */
    @Operation(summary = "Activer un compte",
            description = "Active le compte via le lien reçu par email. La clé d'activation est à usage unique.")
    @ApiResponse(responseCode = "200", description = "Compte activé avec succès")
    @ApiResponse(responseCode = "400", description = "Clé invalide ou compte déjà activé")
    @GetMapping("/activate")
    public ResponseEntity<String> activateAccount(
            @Parameter(description = "Email du compte à activer", required = true)
            @RequestParam @NotBlank(message = "{email.required}") @Email(message = "{email.invalid}") String email,
            @Parameter(description = "Clé d'activation à usage unique", required = true)
            @RequestParam @NotBlank(message = "key obligatoire") String key,
            Locale locale) throws AccountNotFoundException {

        boolean activated = accountService.activateUser(email, key);
        if (activated) {
            return ResponseEntity.ok(messageService.getMessage(API_ACCOUNT_ACTIVATION_SUCCESS_KEY, locale));
        } else {
            return ResponseEntity.badRequest().body(messageService.getMessage(API_ACCOUNT_ACTIVATION_FAILURE_KEY, locale));
        }
    }

    @GetMapping("/infos")
    public ResponseEntity<AccountInformationsResponse> getAccountInformations(Principal principal, Locale locale) {
        String email = principal.getName();
        try {
            AccountInformationDto accountInformationDto = accountService.getAccountInformationDto(email);
            return ResponseEntity.ok(new AccountInformationsResponse(accountInformationDto.getEmail(),
                    accountInformationDto.getPseudo(),
                    "Success",
                    API_RETURN_OK,
                    accountInformationDto.getLastLoginDate(),
                    accountInformationDto.getLastPasswordChangeDate()));
        } catch (AccountNotFoundException exception) {
            LOGGER.warn("The account {} was not found", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new AccountInformationsResponse(email,
                            messageService.getMessage(ACCOUNT_NOT_FOUND, locale),
                            Constants.API_RETURN_KO));
        } catch (Exception e) {
            LOGGER.error("Error when find account informations : {}", email, e);
            throw e;
        }
    }

}