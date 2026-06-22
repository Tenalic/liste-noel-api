package sc.liste.noel.account.db.entity;

import jakarta.persistence.*;
import sc.liste.noel.giftlist.db.entity.GiftListEntity;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "compte")
public class AccountEntity {

    @Id
    @Column(name = "email")
    private String email;
    @Column(name = "password")
    private String password;
    @Column(name = "nb_connexion")
    private Integer loginCount;
    @Column(name = "nb_deconnexion")
    private Integer logoutCount;
    @Column(name = "nb_modification_mdp")
    private Integer passwordChangeCount;
    @Column(name = "date_derniere_connexion")
    private LocalDateTime lastLoginDate;
    @Column(name = "date_derniere_deconnexion")
    private LocalDateTime lastLogoutDate;
    @Column(name = "date_derniere_modification_mdp")
    private LocalDateTime lastPasswordChangeDate;

    @OneToMany(mappedBy = "owner")
    private List<GiftListEntity> giftLists;

    @Column(name = "cgu_accepted")
    private Boolean termsAccepted;

    @Column(name = "pseudo")
    private String pseudo;
    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "activation_key")
    private String activationKey;

    public AccountEntity() {
        super();
    }

    public AccountEntity(String email, String password, boolean termsAccepted, String pseudo, String activationKey) {
        super();
        this.email = email;
        this.password = password;
        this.loginCount = 1;
        this.logoutCount = 0;
        this.passwordChangeCount = 0;
        this.setLastLoginDate(LocalDateTime.now());
        this.termsAccepted = termsAccepted;
        this.pseudo = pseudo;
        this.emailVerified = false;
        this.activationKey = activationKey;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getLoginCount() {
        return loginCount;
    }

    public void setLoginCount(Integer loginCount) {
        this.loginCount = loginCount;
    }

    public Integer getLogoutCount() {
        return logoutCount;
    }

    public void setLogoutCount(Integer logoutCount) {
        this.logoutCount = logoutCount;
    }

    public Integer getPasswordChangeCount() {
        return passwordChangeCount;
    }

    public void setPasswordChangeCount(Integer passwordChangeCount) {
        this.passwordChangeCount = passwordChangeCount;
    }

    public LocalDateTime getLastLoginDate() {
        return lastLoginDate;
    }

    public void setLastLoginDate(LocalDateTime lastLoginDate) {
        this.lastLoginDate = lastLoginDate;
    }

    public LocalDateTime getLastLogoutDate() {
        return lastLogoutDate;
    }

    public void setLastLogoutDate(LocalDateTime lastLogoutDate) {
        this.lastLogoutDate = lastLogoutDate;
    }

    public LocalDateTime getLastPasswordChangeDate() {
        return lastPasswordChangeDate;
    }

    public void setLastPasswordChangeDate(LocalDateTime lastPasswordChangeDate) {
        this.lastPasswordChangeDate = lastPasswordChangeDate;
    }

    public Boolean getTermsAccepted() {
        return termsAccepted;
    }

    public void setTermsAccepted(Boolean termsAccepted) {
        this.termsAccepted = termsAccepted;
    }

    public List<GiftListEntity> getGiftLists() {
        return giftLists;
    }

    public void setGiftLists(List<GiftListEntity> giftLists) {
        this.giftLists = giftLists;
    }

    public String getPseudo() {
        return pseudo;
    }

    public void setPseudo(String pseudo) {
        this.pseudo = pseudo;
    }

    public Boolean getEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(Boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getActivationKey() {
        return activationKey;
    }

    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }
}
