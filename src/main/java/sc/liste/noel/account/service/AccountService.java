package sc.liste.noel.account.service;

import com.fasterxml.uuid.Generators;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sc.liste.noel.account.db.entity.AccountEntity;
import sc.liste.noel.account.db.repo.AccountRepo;
import sc.liste.noel.account.dto.AccountDto;
import sc.liste.noel.account.dto.AccountInformationDto;
import sc.liste.noel.account.exception.*;
import sc.liste.noel.account.mapper.AccountInformationsMapper;
import sc.liste.noel.account.mapper.AccountMapper;
import sc.liste.noel.common.service.EmailTemplateService;
import sc.liste.noel.common.service.MailService;

import java.time.LocalDateTime;

@Service
public class AccountService {

    public static final String ACCOUNT_NOT_FOUND = "Account not found";
    @Value("${base_url}")
    private String baseUrl;

    private final AccountRepo accountRepo;

    private final MailService mailService;

    private final PasswordService passwordService;

    @Value("${send_email_active}")
    private boolean mailServiceEnabled;

    private final EmailTemplateService emailTemplateService;

    public AccountService(AccountRepo accountRepo, MailService mailService, PasswordService passwordService, EmailTemplateService emailTemplateService) {
        this.accountRepo = accountRepo;
        this.mailService = mailService;
        this.passwordService = passwordService;
        this.emailTemplateService = emailTemplateService;
    }


    public boolean accountExists(String email) {
        return accountRepo.findByEmail(email).isPresent();
    }


    public String getPseudo(String email) throws AccountNotFoundException {
        AccountEntity account = accountRepo.findByEmail(email).orElseThrow(() -> new AccountNotFoundException(ACCOUNT_NOT_FOUND));
        return account.getPseudo();
    }


    public boolean pseudoExists(String pseudo) {
        return accountRepo.findByPseudo(pseudo).isPresent();
    }


    public AccountDto login(String email, String password) throws AccountNotFoundException {
        AccountEntity account = accountRepo.findByEmail(email)
                .orElseThrow(() -> new AccountNotFoundException(ACCOUNT_NOT_FOUND));

        if (!passwordService.verifyPassword(password, account.getPassword())) {
            throw new AccountNotFoundException(ACCOUNT_NOT_FOUND);
        }

        account.setLoginCount(account.getLoginCount() + 1);
        account.setLastLoginDate(LocalDateTime.now());
        accountRepo.save(account);

        return AccountMapper.entityToDto(account);
    }


    public String createAccount(String email, String password, String confirmPassword, boolean termsAccepted, String pseudo) throws AccountAlreadyExistsException, PseudoAlreadyExistsException, TermsNotAcceptedException, PasswordNotEqualsException {
        if (accountExists(email)) {
            throw new AccountAlreadyExistsException("Account already exists");
        }

        // Check whether the pseudo already exists
        if (pseudoExists(pseudo)) {
            throw new PseudoAlreadyExistsException("Pseudo already exists");
        }
        if (!termsAccepted) {
            throw new TermsNotAcceptedException("You must accept the terms to create a account");
        }
        if (!password.equals(confirmPassword)) {
            throw new PasswordNotEqualsException("Password not equals");
        }
        String activationKey = Generators.timeBasedEpochGenerator().generate().toString();
        accountRepo.save(new AccountEntity(email, passwordService.hashPassword(password), termsAccepted, pseudo, activationKey));
        String url = baseUrl + "/compte/activate?userId=" + email + "&key=" + activationKey;

        String body = emailTemplateService.generateBodyActivationEmail(email, url);
        mailService.sendEmail(email, "Confirmation de création de compte", body);
        return email;
    }


    public boolean deleteAccount(String email) {
        if (accountRepo.findByEmail(email).isPresent()) {
            accountRepo.deleteById(email);
        } else {
            return false;
        }
        return true;
    }


    public void updatePassword(String email, String oldPassword, String newPassword, String confirmationNewPassword) throws AccountNotFoundException, PasswordException {
        AccountEntity accountEntity = accountRepo.findByEmail(email).orElseThrow(() -> new AccountNotFoundException(ACCOUNT_NOT_FOUND));

        if (!passwordService.verifyPassword(oldPassword, accountEntity.getPassword())) {
            throw new AccountNotFoundException(ACCOUNT_NOT_FOUND);
        }

        if (newPassword.equals(confirmationNewPassword)) {
            accountEntity.setPassword(passwordService.hashPassword(newPassword));
            accountEntity.setPasswordChangeCount(accountEntity.getPasswordChangeCount() + 1);
            accountEntity.setLastPasswordChangeDate(LocalDateTime.now());
            accountRepo.save(accountEntity);
        } else {
            throw new PasswordException("Passwords do not match");
        }

    }

    private boolean forceUpdatePassword(String email, String newPassword) throws AccountNotFoundException {
        AccountEntity accountEntity = accountRepo.findByEmail(email).orElseThrow(() -> new AccountNotFoundException(ACCOUNT_NOT_FOUND));
        accountEntity.setPassword(passwordService.hashPassword(newPassword));
        accountEntity.setPasswordChangeCount(accountEntity.getPasswordChangeCount() + 1);
        accountEntity.setLastPasswordChangeDate(LocalDateTime.now());
        accountRepo.save(accountEntity);
        return true;
    }


    public void generateAndSendPassword(String email) throws MailServiceDisabledException, AccountNotFoundException {
        if (mailServiceEnabled) {
            String newPassword = PasswordService.generatePassayPassword();
            forceUpdatePassword(email, newPassword);
            String body = "Votre mot de passe a été réinitialisé, voici votre nouveau mot de passe, vous pourrez le modifier une fois connecté : " + newPassword;
            mailService.sendEmail(email, "Mot de passe modifié", body);
        } else {
            throw new MailServiceDisabledException("Email sending is disabled");
        }

    }


    public boolean activateUser(String email, String activationKey) throws AccountNotFoundException {
        AccountEntity account = accountRepo.findByEmail(email).orElseThrow(() -> new AccountNotFoundException(ACCOUNT_NOT_FOUND));
        if (account.getActivationKey().equals(activationKey) && !account.getEmailVerified()) {
            account.setEmailVerified(true);
            account.setActivationKey(null);
            accountRepo.save(account);
            return true;
        }
        return false;
    }

    public AccountInformationDto getAccountInformationDto(String email) throws AccountNotFoundException {
        AccountEntity account = accountRepo.findByEmail(email).orElseThrow(() -> new AccountNotFoundException(ACCOUNT_NOT_FOUND));
        return AccountInformationsMapper.entityToDto(account);
    }
}
