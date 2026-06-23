package sc.liste.noel.account.controller;


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
import sc.liste.noel.account.dto.request.ChangePasswordRequest;
import sc.liste.noel.account.dto.request.ForgotPasswordRequest;
import sc.liste.noel.account.dto.request.LoginRequest;
import sc.liste.noel.account.dto.request.RegistrationRequest;
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

@RestController
@RequestMapping("/api/compte")
public class AccountResource {

    private static final Logger LOGGER = LogManager.getLogger(AccountResource.class);

    private final AccountService accountService;

    private final SecretService secretService;

    private final MessageService messageService;

    private final JwtService jwtService;

    public AccountResource(AccountService accountService, SecretService secretService, MessageService messageService, JwtService jwtService) {
        this.accountService = accountService;
        this.secretService = secretService;
        this.messageService = messageService;
        this.jwtService = jwtService;
    }

    /**
     * API to create a new account.
     *
     * @param locale Locale for the language of the messages (optional, default = fr).
     * @return ResponseEntity<AccountResponse> with the return code (0 if OK) and a message.
     */
    @PostMapping("/inscription")
    public ResponseEntity<AccountResponse> register(
            @RequestBody @Valid RegistrationRequest registrationRequest,
            @RequestHeader(value = "Accept-Language", required = false, defaultValue = "fr")
            Locale locale) {
        try {
            String email = accountService.createAccount(registrationRequest.email(), registrationRequest.password(), registrationRequest.confirmPassword(), registrationRequest.acceptTerms(), registrationRequest.pseudo());

            String token = jwtService.generateToken(email);

            // HTTP-only cookie: JavaScript CANNOT read it → protected against XSS
            ResponseCookie cookie = ResponseCookie.from("auth-token", token)
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

    @PostMapping("/connexion")
    public ResponseEntity<AccountResponse> login(@RequestBody @Valid LoginRequest loginRequest,
                                                 Locale locale) {
        String email = loginRequest.email();

        LOGGER.info("Entering login service: " + email);

        try {
            AccountDto account = accountService.login(email, loginRequest.password());

            String token = jwtService.generateToken(account.getEmail());

            // HTTP-only cookie: JavaScript CANNOT read it → protected against XSS
            ResponseCookie cookie = ResponseCookie.from("auth-token", token)
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
            LOGGER.error("Error during login: " + email, e);
            return ResponseEntity.internalServerError()
                    .body(new AccountResponse(email, messageService.getMessage(API_ERROR_GENERIC_KEY, locale), Constants.API_RETURN_KO));
        }
    }

    @PostMapping("/deconnexion")
    public ResponseEntity<Void> logout() {
        // Overwrite the cookie with maxAge=0 → the browser deletes it immediately
        ResponseCookie cookie = ResponseCookie.from("auth-token", "")
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
     * API to delete an account
     *
     * @param email   : user email
     * @param secret: secret of the calling application to authenticate itself
     * @return ResponseEntity<AccountResponse> containing the email, the return code (0
     * if ok) and the return message
     */
    @DeleteMapping("/supprimer")
    public ResponseEntity<AccountResponse> deleteAccount(
            @RequestParam @NotBlank(message = "{email.required}") @Email(message = "{email.invalid}") String email,
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

    @GetMapping("/activate")
    public ResponseEntity<String> activateAccount(
            @RequestParam @NotBlank(message = "{email.required}") @Email(message = "{email.invalid}") String email,
            @RequestParam @NotBlank(message = "key obligatoire") String key,
            Locale locale) throws AccountNotFoundException {

        boolean activated = accountService.activateUser(email, key);
        if (activated) {
            return ResponseEntity.ok(messageService.getMessage(API_ACCOUNT_ACTIVATION_SUCCESS_KEY, locale));
        } else {
            return ResponseEntity.badRequest().body(messageService.getMessage(API_ACCOUNT_ACTIVATION_FAILURE_KEY, locale));
        }
    }

}
