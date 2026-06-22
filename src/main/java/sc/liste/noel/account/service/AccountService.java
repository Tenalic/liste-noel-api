package sc.liste.noel.account.service;

import com.fasterxml.uuid.Generators;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sc.liste.noel.account.db.entity.AccountEntity;
import sc.liste.noel.account.db.repo.AccountRepo;
import sc.liste.noel.account.exception.AccountNotFoundException;
import sc.liste.noel.account.exception.MailServiceDisabledException;
import sc.liste.noel.account.exception.PasswordException;
import sc.liste.noel.common.service.EmailTemplateService;
import sc.liste.noel.common.service.MailService;
import sc.liste.noel.account.utils.PasswordUtils;
import sc.liste.noel.account.mapper.AccountMapper;
import sc.liste.noel.account.dto.AccountDto;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AccountService {

    @Value("${base_url}")
    private String baseUrl;

    private final AccountRepo accountRepo;

    @Value("${salt}")
    private String salt;

    private final MailService mailService;

    @Value("${send_email_active}")
    private Boolean mailServiceEnabled;

    private final EmailTemplateService emailTemplateService;

    public AccountService(AccountRepo accountRepo, MailService mailService, EmailTemplateService emailTemplateService) {
        this.accountRepo = accountRepo;
        this.mailService = mailService;
        this.emailTemplateService = emailTemplateService;
    }

    
    public boolean accountExists(String email) {
        return Optional.ofNullable(accountRepo.findByEmail(email)).isPresent();
    }

    
    public String getPseudo(String email) throws AccountNotFoundException {
        AccountEntity account = accountRepo.findByEmail(email);
        if (account == null) {
            throw new AccountNotFoundException("Account not found");
        }
        return account.getPseudo();
    }

    
    public boolean pseudoExists(String pseudo) {
        return Optional.ofNullable(accountRepo.findByPseudo(pseudo)).isPresent();
    }

    
    public AccountDto login(String email, String password) throws AccountNotFoundException {
        AccountEntity account = accountRepo.findByEmailAndPassword(email, PasswordUtils.generateSecurePassword(password, salt));
        if (account != null) {
            account.setLoginCount(account.getLoginCount() + 1);
            account.setLastLoginDate(LocalDateTime.now());
            accountRepo.save(account);
            return AccountMapper.entityToDto(account);
        } else {
            throw new AccountNotFoundException("Account not found");
        }
    }

    
    public String createAccount(String email, String password, boolean termsAccepted, String pseudo) {
        String activationKey = Generators.timeBasedEpochGenerator().generate().toString();
        accountRepo.save(new AccountEntity(email, PasswordUtils.generateSecurePassword(password, salt), termsAccepted, pseudo, activationKey));
        String url = baseUrl + "/compte/activate?userId=" + email + "&key=" + activationKey;

        String body = emailTemplateService.generateBodyActivationEmail(email, url);
        mailService.sendEmail(email, "Confirmation de création de compte", body);
        return email;
    }

    
    public boolean deleteAccount(String email) {
        if (accountRepo.findByEmail(email) != null) {
            accountRepo.deleteById(email);
        } else {
            return false;
        }
        return true;
    }

    
    public void updatePassword(String email, String oldPassword, String newPassword, String confirmationNewPassword) throws AccountNotFoundException, PasswordException {
        AccountEntity accountEntity = accountRepo.findByEmailAndPassword(email,
                PasswordUtils.generateSecurePassword(oldPassword, salt));
        if (accountEntity != null) {
            if (newPassword.equals(confirmationNewPassword)) {
                accountEntity.setPassword(PasswordUtils.generateSecurePassword(newPassword, salt));
                accountEntity.setPasswordChangeCount(accountEntity.getPasswordChangeCount() + 1);
                accountEntity.setLastPasswordChangeDate(LocalDateTime.now());
                accountRepo.save(accountEntity);
            } else {
                throw new PasswordException("Passwords do not match");
            }
        } else {
            throw new AccountNotFoundException("Account not found or the password is incorrect");
        }
    }

    private boolean forceUpdatePassword(String email, String newPassword) {
        AccountEntity accountEntity = accountRepo.findByEmail(email);
        if (accountEntity != null) {
            accountEntity.setPassword(PasswordUtils.generateSecurePassword(newPassword, salt));
            accountEntity.setPasswordChangeCount(accountEntity.getPasswordChangeCount() + 1);
            accountEntity.setLastPasswordChangeDate(LocalDateTime.now());
            accountRepo.save(accountEntity);
            return true;
        }
        return false;
    }

    
    public void generateAndSendPassword(String email) throws MailServiceDisabledException {

        if (mailServiceEnabled) {
            String newPassword = PasswordService.generatePassayPassword();
            boolean isUpdated = forceUpdatePassword(email, newPassword);
            if (isUpdated) {
                String body = "Votre mot de passe a été réinitialisé, voici votre nouveau mot de passe, vous pourrez le modifier une fois connecté : " + newPassword;
                mailService.sendEmail(email, "Mot de passe modifié", body);
            }
        } else {
            throw new MailServiceDisabledException("Email sending is disabled");
        }

    }

    
    public boolean activateUser(String email, String activationKey) {
        AccountEntity account = accountRepo.findByEmail(email);
        if (account != null) {
            if (account.getActivationKey().equals(activationKey) && !account.getEmailVerified()) {
                account.setEmailVerified(true);
                account.setActivationKey(null); // Optional: prevents reuse
                accountRepo.save(account);
                return true;
            }
        }
        return false;
    }
}
