package sc.liste.noel.account.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sc.liste.noel.account.db.entity.AccountEntity;
import sc.liste.noel.account.db.repo.AccountRepo;
import sc.liste.noel.account.dto.AccountDto;
import sc.liste.noel.account.exception.*;
import sc.liste.noel.common.service.EmailTemplateService;
import sc.liste.noel.common.service.MailService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountService")
class AccountServiceTest {

    @Mock
    private AccountRepo accountRepo;

    @Mock
    private MailService mailService;

    @Mock
    private PasswordService passwordService;

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(accountService, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(accountService, "mailServiceEnabled", true);
    }

    // -------------------------------------------------------------------------
    // accountExists
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("accountExists()")
    class AccountExists {

        @Test
        @DisplayName("retourne true si le compte existe")
        void shouldReturnTrueWhenAccountFound() {
            when(accountRepo.findByEmail("user@test.com"))
                    .thenReturn(Optional.of(new AccountEntity()));

            assertThat(accountService.accountExists("user@test.com")).isTrue();
        }

        @Test
        @DisplayName("retourne false si le compte n'existe pas")
        void shouldReturnFalseWhenAccountNotFound() {
            when(accountRepo.findByEmail("unknown@test.com"))
                    .thenReturn(Optional.empty());

            assertThat(accountService.accountExists("unknown@test.com")).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // getPseudo
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getPseudo()")
    class GetPseudo {

        @Test
        @DisplayName("retourne le pseudo du compte existant")
        void shouldReturnPseudoWhenAccountExists() throws AccountNotFoundException {
            AccountEntity account = buildAccount("user@test.com", "Steph");
            when(accountRepo.findByEmail("user@test.com")).thenReturn(Optional.of(account));

            assertThat(accountService.getPseudo("user@test.com")).isEqualTo("Steph");
        }

        @Test
        @DisplayName("lève AccountNotFoundException si le compte est introuvable")
        void shouldThrowWhenAccountNotFound() {
            when(accountRepo.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.getPseudo("ghost@test.com"))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // pseudoExists
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("pseudoExists()")
    class PseudoExists {

        @Test
        @DisplayName("retourne true si le pseudo existe")
        void shouldReturnTrueWhenPseudoFound() {
            when(accountRepo.findByPseudo("Steph")).thenReturn(Optional.of(new AccountEntity()));

            assertThat(accountService.pseudoExists("Steph")).isTrue();
        }

        @Test
        @DisplayName("retourne false si le pseudo n'existe pas")
        void shouldReturnFalseWhenPseudoNotFound() {
            when(accountRepo.findByPseudo("Unknown")).thenReturn(Optional.empty());

            assertThat(accountService.pseudoExists("Unknown")).isFalse();
        }
    }

    // -------------------------------------------------------------------------
    // login
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("retourne un AccountDto et incrémente loginCount sur succès")
        void shouldReturnDtoAndIncrementLoginCount() throws AccountNotFoundException {
            AccountEntity account = buildAccount("user@test.com", "Steph");
            account.setLoginCount(2);
            when(accountRepo.findByEmail("user@test.com")).thenReturn(Optional.of(account));
            when(passwordService.verifyPassword("pass", account.getPassword())).thenReturn(true);

            AccountDto result = accountService.login("user@test.com", "pass");

            assertThat(result).isNotNull();
            assertThat(account.getLoginCount()).isEqualTo(3);
            assertThat(account.getLastLoginDate()).isNotNull();
            verify(accountRepo).save(account);
        }

        @Test
        @DisplayName("lève AccountNotFoundException si le compte est introuvable")
        void shouldThrowWhenAccountNotFound() {
            when(accountRepo.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.login("ghost@test.com", "pass"))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("lève AccountNotFoundException si le mot de passe est incorrect")
        void shouldThrowWhenPasswordInvalid() {
            AccountEntity account = buildAccount("user@test.com", "Steph");
            when(accountRepo.findByEmail("user@test.com")).thenReturn(Optional.of(account));
            when(passwordService.verifyPassword("wrong", account.getPassword())).thenReturn(false);

            assertThatThrownBy(() -> accountService.login("user@test.com", "wrong"))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepo, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // createAccount
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("createAccount()")
    class CreateAccount {

        @Test
        @DisplayName("crée le compte et envoie l'email d'activation")
        void shouldCreateAccountAndSendEmail() throws Exception {
            when(accountRepo.findByEmail("new@test.com")).thenReturn(Optional.empty());
            when(accountRepo.findByPseudo("Steph")).thenReturn(Optional.empty());
            when(passwordService.hashPassword("pass123")).thenReturn("hashed");
            when(emailTemplateService.generateBodyActivationEmail(anyString(), anyString()))
                    .thenReturn("<html>activation</html>");

            String result = accountService.createAccount(
                    "new@test.com", "pass123", "pass123", true, "Steph");

            assertThat(result).isEqualTo("new@test.com");
            verify(accountRepo).save(any(AccountEntity.class));
            verify(mailService).sendEmail(eq("new@test.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("l'URL d'activation contient l'email et la clé")
        void shouldPassCorrectActivationUrlToEmailTemplate() throws Exception {
            when(accountRepo.findByEmail("new@test.com")).thenReturn(Optional.empty());
            when(accountRepo.findByPseudo("Steph")).thenReturn(Optional.empty());
            when(passwordService.hashPassword(any())).thenReturn("hashed");

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            when(emailTemplateService.generateBodyActivationEmail(anyString(), urlCaptor.capture()))
                    .thenReturn("body");

            accountService.createAccount("new@test.com", "pass123", "pass123", true, "Steph");

            assertThat(urlCaptor.getValue())
                    .startsWith("http://localhost:8080/compte/activate")
                    .contains("userId=new@test.com")
                    .contains("key=");
        }

        @Test
        @DisplayName("lève AccountAlreadyExistsException si l'email est déjà utilisé")
        void shouldThrowWhenEmailAlreadyExists() {
            when(accountRepo.findByEmail("existing@test.com"))
                    .thenReturn(Optional.of(new AccountEntity()));

            assertThatThrownBy(() -> accountService.createAccount(
                    "existing@test.com", "pass", "pass", true, "Steph"))
                    .isInstanceOf(AccountAlreadyExistsException.class);

            verify(accountRepo, never()).save(any());
        }

        @Test
        @DisplayName("lève PseudoAlreadyExistsException si le pseudo est déjà pris")
        void shouldThrowWhenPseudoAlreadyExists() {
            when(accountRepo.findByEmail("new@test.com")).thenReturn(Optional.empty());
            when(accountRepo.findByPseudo("Steph")).thenReturn(Optional.of(new AccountEntity()));

            assertThatThrownBy(() -> accountService.createAccount(
                    "new@test.com", "pass", "pass", true, "Steph"))
                    .isInstanceOf(PseudoAlreadyExistsException.class);

            verify(accountRepo, never()).save(any());
        }

        @Test
        @DisplayName("lève TermsNotAcceptedException si les CGU ne sont pas acceptées")
        void shouldThrowWhenTermsNotAccepted() {
            when(accountRepo.findByEmail("new@test.com")).thenReturn(Optional.empty());
            when(accountRepo.findByPseudo("Steph")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.createAccount(
                    "new@test.com", "pass", "pass", false, "Steph"))
                    .isInstanceOf(TermsNotAcceptedException.class);
        }

        @Test
        @DisplayName("lève PasswordNotEqualsException si les mots de passe ne correspondent pas")
        void shouldThrowWhenPasswordsDoNotMatch() {
            when(accountRepo.findByEmail("new@test.com")).thenReturn(Optional.empty());
            when(accountRepo.findByPseudo("Steph")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.createAccount(
                    "new@test.com", "pass123", "different", true, "Steph"))
                    .isInstanceOf(PasswordNotEqualsException.class);
        }
    }

    // -------------------------------------------------------------------------
    // deleteAccount
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("deleteAccount()")
    class DeleteAccount {

        @Test
        @DisplayName("supprime le compte et retourne true")
        void shouldDeleteAndReturnTrue() {
            when(accountRepo.findByEmail("user@test.com"))
                    .thenReturn(Optional.of(new AccountEntity()));

            assertThat(accountService.deleteAccount("user@test.com")).isTrue();
            verify(accountRepo).deleteById("user@test.com");
        }

        @Test
        @DisplayName("retourne false si le compte est introuvable")
        void shouldReturnFalseWhenNotFound() {
            when(accountRepo.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThat(accountService.deleteAccount("ghost@test.com")).isFalse();
            verify(accountRepo, never()).deleteById(any());
        }
    }

    // -------------------------------------------------------------------------
    // updatePassword
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("updatePassword()")
    class UpdatePassword {

        @Test
        @DisplayName("met à jour le mot de passe avec succès")
        void shouldUpdatePasswordSuccessfully() throws Exception {
            AccountEntity account = buildAccount("user@test.com", "Steph");
            account.setPassword("oldHashed");
            account.setPasswordChangeCount(1);
            when(accountRepo.findByEmail("user@test.com")).thenReturn(Optional.of(account));
            when(passwordService.verifyPassword("oldPass", "oldHashed")).thenReturn(true);
            when(passwordService.hashPassword("newPass")).thenReturn("newHashed");

            accountService.updatePassword("user@test.com", "oldPass", "newPass", "newPass");

            assertThat(account.getPassword()).isEqualTo("newHashed");
            assertThat(account.getPasswordChangeCount()).isEqualTo(2);
            assertThat(account.getLastPasswordChangeDate()).isNotNull();
            verify(accountRepo).save(account);
        }

        @Test
        @DisplayName("lève AccountNotFoundException si le compte est introuvable")
        void shouldThrowWhenAccountNotFound() {
            when(accountRepo.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.updatePassword(
                    "ghost@test.com", "old", "new", "new"))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("lève AccountNotFoundException si l'ancien mot de passe est incorrect")
        void shouldThrowWhenOldPasswordInvalid() {
            AccountEntity account = buildAccount("user@test.com", "Steph");
            account.setPassword("oldHashed");
            when(accountRepo.findByEmail("user@test.com")).thenReturn(Optional.of(account));
            when(passwordService.verifyPassword("wrong", "oldHashed")).thenReturn(false);

            assertThatThrownBy(() -> accountService.updatePassword(
                    "user@test.com", "wrong", "newPass", "newPass"))
                    .isInstanceOf(AccountNotFoundException.class);

            verify(accountRepo, never()).save(any());
        }

        @Test
        @DisplayName("lève PasswordException si les nouveaux mots de passe ne correspondent pas")
        void shouldThrowWhenNewPasswordsDoNotMatch() {
            AccountEntity account = buildAccount("user@test.com", "Steph");
            account.setPassword("oldHashed");
            when(accountRepo.findByEmail("user@test.com")).thenReturn(Optional.of(account));
            when(passwordService.verifyPassword("oldPass", "oldHashed")).thenReturn(true);

            assertThatThrownBy(() -> accountService.updatePassword(
                    "user@test.com", "oldPass", "newPass", "different"))
                    .isInstanceOf(PasswordException.class);

            verify(accountRepo, never()).save(any());
        }
    }

    // -------------------------------------------------------------------------
    // generateAndSendPassword
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("generateAndSendPassword()")
    class GenerateAndSendPassword {

        @Test
        @DisplayName("génère un nouveau mot de passe et envoie l'email quand le service mail est actif")
        void shouldGenerateAndSendPasswordWhenMailEnabled() throws Exception {
            AccountEntity account = buildAccount("user@test.com", "Steph");
            account.setPasswordChangeCount(0);
            when(accountRepo.findByEmail("user@test.com")).thenReturn(Optional.of(account));
            when(passwordService.hashPassword(anyString())).thenReturn("hashed");

            try (var mock = mockStatic(PasswordService.class)) {
                mock.when(PasswordService::generatePassayPassword).thenReturn("NewPass123!");

                accountService.generateAndSendPassword("user@test.com");

                verify(mailService).sendEmail(eq("user@test.com"), anyString(), contains("NewPass123!"));
            }
        }

        @Test
        @DisplayName("lève MailServiceDisabledException si le service mail est désactivé")
        void shouldThrowWhenMailServiceDisabled() {
            ReflectionTestUtils.setField(accountService, "mailServiceEnabled", false);

            assertThatThrownBy(() -> accountService.generateAndSendPassword("user@test.com"))
                    .isInstanceOf(MailServiceDisabledException.class);

            verify(mailService, never()).sendEmail(any(), any(), any());
        }
    }

    // -------------------------------------------------------------------------
    // activateUser
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("activateUser()")
    class ActivateUser {

        @Test
        @DisplayName("active le compte et retourne true si la clé est valide")
        void shouldActivateAccountAndReturnTrue() throws AccountNotFoundException {
            AccountEntity account = buildAccount("user@test.com", "Steph");
            account.setActivationKey("valid-key");
            account.setEmailVerified(false);
            when(accountRepo.findByEmail("user@test.com")).thenReturn(Optional.of(account));

            boolean result = accountService.activateUser("user@test.com", "valid-key");

            assertThat(result).isTrue();
            assertThat(account.getEmailVerified()).isTrue();
            assertThat(account.getActivationKey()).isNull();
            verify(accountRepo).save(account);
        }

        @Test
        @DisplayName("retourne false si la clé d'activation est incorrecte")
        void shouldReturnFalseWhenKeyInvalid() throws AccountNotFoundException {
            AccountEntity account = buildAccount("user@test.com", "Steph");
            account.setActivationKey("valid-key");
            account.setEmailVerified(false);
            when(accountRepo.findByEmail("user@test.com")).thenReturn(Optional.of(account));

            boolean result = accountService.activateUser("user@test.com", "wrong-key");

            assertThat(result).isFalse();
            verify(accountRepo, never()).save(any());
        }

        @Test
        @DisplayName("retourne false si le compte est déjà activé")
        void shouldReturnFalseWhenAlreadyActivated() throws AccountNotFoundException {
            AccountEntity account = buildAccount("user@test.com", "Steph");
            account.setActivationKey("valid-key");
            account.setEmailVerified(true);
            when(accountRepo.findByEmail("user@test.com")).thenReturn(Optional.of(account));

            boolean result = accountService.activateUser("user@test.com", "valid-key");

            assertThat(result).isFalse();
            verify(accountRepo, never()).save(any());
        }

        @Test
        @DisplayName("lève AccountNotFoundException si le compte est introuvable")
        void shouldThrowWhenAccountNotFound() {
            when(accountRepo.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.activateUser("ghost@test.com", "any-key"))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private AccountEntity buildAccount(String email, String pseudo) {
        AccountEntity account = new AccountEntity();
        account.setEmail(email);
        account.setPseudo(pseudo);
        account.setPassword("hashed-password");
        account.setLoginCount(0);
        account.setPasswordChangeCount(0);
        account.setEmailVerified(false);
        return account;
    }
}