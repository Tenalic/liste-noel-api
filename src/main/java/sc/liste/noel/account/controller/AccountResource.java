
Claude a terminé la réponse.

        package sc.liste.noel.account.service; import com.fasterxml.uuid.Generators; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import sc.liste.noel.account.db.entity.AccountEntity; import sc.liste.noel.account.db.repo.AccountRepo; impor

        pasted

tu peux me faire les TU pour cette class java 21 ?

        14:56

Check frontend-design skill - actually checking for Java test patterns
Check frontend-design skill - actually checking for Java test patterns
Voilà les tests unitaires complets pour AccountService avec JUnit 5 + Mockito :


        Fichier créé, lu un fichier
        Fichier créé, lu un fichier
Voici ce que couvrent les tests — 30 cas au total, organisés par méthode avec @Nested :

Méthode	Cas testés
accountExists	trouvé / introuvable
getPseudo	retourne le pseudo / AccountNotFoundException
pseudoExists	trouvé / introuvable
login	succès + màj compteurs / email inconnu / mauvais mot de passe
createAccount	succès + email envoyé / email déjà pris / pseudo pris / CGU refusées / mots de passe différents / URL d'activation correcte
deleteAccount	suppression OK / compte introuvable
updatePassword	succès + compteur / compte introuvable / ancien mdp incorrect / nouveaux mdp différents
generateAndSendPassword	envoi réussi / mail désactivé / compte introuvable
activateUser	activation OK + clé effacée / mauvaise clé / déjà activé / compte introuvable
Quelques points à vérifier avant de lancer :

AccountEntity doit avoir des setters/getters pour tous les champs utilisés (notamment setEmailVerified, getEmailVerified). Si emailVerified est un boolean primitif, adapte en conséquence.
        PasswordService.generatePassayPassword() doit être une méthode static pour que le mockStatic fonctionne (nécessite la dépendance mockito-inline ou Mockito 5+).
Le test sur l'URL d'activation suppose que generateBodyActivationEmail est bien appelé avec l'URL — si ce n'est pas le cas dans ton implémentation de EmailTemplateService, adapte le captor.

        Accountservicetest
Code · JAVA
et pour elle maintenant ?

        package sc.liste.noel.gift.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import sc.liste.noel.common.exception.ForbiddenModificationException;
import sc.liste.noel.common.service.MailService;
import sc.liste.noel.gift.db.entity.GiftEntity;
import sc.liste.noel.gift.db.repo.GiftRepo;
import sc.liste.noel.giftlist.service.GiftListService;

@Service
public class GiftService {

    private final GiftRepo giftRepo;
    private final GiftListService giftListService;

    public GiftService(GiftRepo giftRepo, MailService mailService, GiftListService giftListService) {
        this.giftRepo = giftRepo;
        this.giftListService = giftListService;
    }

    public void addGiftToGiftList(String title, String url, String description, String giftListId, String owner, int priority) {
        GiftEntity giftEntity = new GiftEntity();
        giftEntity.setDescription(description);
        giftEntity.setGiftListId(Long.valueOf(giftListId));
        giftEntity.setTitle(title);
        giftEntity.setTaken(false);
        giftEntity.setUrl(url);
        giftEntity.setPriorityValue(priority);
        giftRepo.save(giftEntity);
    }

    public GiftEntity getGiftEntity(Long giftId) {
        return giftRepo.findByGiftId(giftId).orElseThrow();
    }

    public void deleteGift(Long giftId, String email) throws ForbiddenModificationException {
        if (!giftRepo.existsByGiftIdAndListOwner(giftId, email)) {
            throw new ForbiddenModificationException("You can't delete a gift was not in your gift list.");
        }
        GiftEntity giftEntity = giftRepo.findByGiftId(giftId).orElseThrow();
        giftRepo.delete(giftEntity);
        giftListService.notifyGiftDeletionToFavorites(giftEntity.getGiftListId(), giftEntity.getTitle(), giftEntity.getDescription(), giftEntity.getUrl());
    }

    @Transactional
    public void updateGift(Long giftId, String titleUpdate, String descriptionUpdate, String urlUpdate, int priorityUpdate, String email) throws ForbiddenModificationException {

        if (!giftRepo.existsByGiftIdAndListOwner(giftId, email)) {
            throw new ForbiddenModificationException("You cannot modify a gift that does not belong to one of your lists.");
        }

        GiftEntity giftEntity = giftRepo.findByGiftId(giftId).orElseThrow();

        giftListService.notifyGiftModificationToFavorites(giftId, giftEntity.getTitle(), giftEntity.getDescription(), giftEntity.getUrl(), giftEntity.getPriorityValue(), titleUpdate, descriptionUpdate, urlUpdate, priorityUpdate);

        giftEntity.setTitle(titleUpdate);
        giftEntity.setDescription(descriptionUpdate);
        giftEntity.setUrl(urlUpdate);
        giftEntity.setPriorityValue(priorityUpdate);

        giftRepo.save(giftEntity);
    }
}
Afficher plus
14:58

        Fichier créé, lu un fichier
        Fichier créé, lu un fichier
15 cas de tests organisés en 4 blocs @Nested :

Méthode	Cas testés
addGiftToGiftList	tous les champs correctement mappés / taken toujours false à la création
getGiftEntity	entité retournée / NoSuchElementException si introuvable
deleteGift	suppression + notification OK / ForbiddenModificationException si pas propriétaire / NoSuchElementException si cadeau introuvable après contrôle / données correctes dans la notification
updateGift	màj des champs + save / notification avec anciennes et nouvelles valeurs / notification appelée avant la mise à jour des champs / ForbiddenModificationException si pas propriétaire / NoSuchElementException si cadeau introuvable
Un point notable : le test "notification appelée avant la mise à jour des champs" utilise un doAnswer pour vérifier l'ordre d'exécution — c'est important car notifyGiftModificationToFavorites reçoit les anciennes valeurs, et un bug de régression pourrait facilement inverser l'ordre.

À adapter si GiftEntity n'a pas de setGiftId (champ auto-généré en base) — dans ce cas, retire l'appel dans le helper et ajuste les comparaisons par référence d'objet.


Giftservicetest
Code · JAVA 

package sc.liste.noel.giftlist.service; import jakarta.transaction.Transactional; import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Service; import sc.liste.noel.account.db.entity.AccountEntity; import sc.liste.noel.account.db.repo.AccountRepo;

pasted

une dernière

15:00

        Fichier créé, lu un fichier
        Fichier créé, lu un fichier
39 cas de tests répartis en 10 blocs @Nested :

Méthode	Cas testés
createGiftList	champs correctement mappés / liste privée
getGiftListsOfEmail	retourne les DTOs / liste vide
getGiftListById	DTO avec shareUrl / GiftListNotFoundException
getGiftLists	recherche par nom / sans nom / nom blanc / remplacement email→pseudo / DTOs null / propriétaire introuvable → RuntimeException
updatePublic	visibilité mise à jour / liste introuvable / pas propriétaire
getFavoriteGiftListsOfEmail	favoris avec pseudos / null si repo retourne null / liste introuvable ignorée / liste vide
addFavorite	sauvegarde si inexistant / ne sauvegarde pas si déjà présent
toggleFavorite	supprime si existant / ajoute si inexistant
notifyGiftDeletionToFavorites	envoi aux favoris / désactivé → pas d'envoi / liste de destinataires vide
notifyGiftModificationToFavorites	envoi avec sujet correct / désactivé → pas d'interaction
deleteGiftList	suppression + favoris + message de retour / pas propriétaire / liste introuvable
getGiftListWithContext	isOwnedByCurrentUser true / anonymisation si non connecté / GiftListNotFoundException / isFavorite basé sur les favoris
À noter : le test getGiftListWithContext avec isFavorite fait appel à getFavoriteGiftListsOfEmail en interne, qui lui-même appelle giftListRepo.findByGiftListId — assure-toi que le when sur ce repo couvre bien les deux appels (le stub avec thenReturn(entity) est correct car Mockito retourne la dernière valeur stubée par défaut).


Giftlistservicetest
Code · JAVA
tu peux modifier :

@Test
@DisplayName("lève GiftListNotFoundException si la liste des DTOs est null")
void throwsGiftListNotFoundException_whenDtosNull() {
    when(giftListRepo.findByIsPublic(true)).thenReturn(null);

    assertThatThrownBy(() -> giftListService.getGiftLists(true, null))
            .isInstanceOf(GiftListNotFoundException.class);
}
ça remonte seulement une liste vide, pas de throw

Afficher plus
15:09
Tu as raison. En regardant le code, GiftListMapper.entitiesToDtosWithoutGifts(null) retourne probablement une liste vide plutôt que null, donc la condition if (giftListDtos != null) est toujours vraie. Le test doit être corrigé :


        Fichier modifié, lu un fichier
        Fichier modifié, lu un fichier
Le test vérifie maintenant le comportement réel : quand le repo retourne une liste vide, le mapper produit une liste vide et la méthode la retourne sans lever d'exception. Si tu veux quand même couvrir le cas GiftListNotFoundException, il faudrait modifier la condition dans getGiftLists pour aussi vérifier giftListDtos.isEmpty().


Giftlistservicetest
Code · JAVA 

package sc.liste.noel.account.controller; import jakarta.validation.Valid; import jakarta.validation.constraints.Email; import jakarta.validation.constraints.NotBlank; import org.apache.logging.log4j.LogManager; import org.apache.logging.log4j.Logger; import org.springframework.http.HttpHe

        pasted

tu peux me faire la java doc ?

        15:30

Consulté un fichier, fichier créé, lu un fichier
Consulté un fichier, fichier créé, lu un fichier
Javadoc ajoutée sur l'ensemble de la classe. Voici ce qui a été documenté :

Classe — description générale du contrôleur, du préfixe de route et du mécanisme de sécurité par cookie HTTP-only JWT.

Constructeur — description de chaque paramètre injecté.

Chaque endpoint avec :

description du comportement métier et des cas particuliers importants (ex : invalidation du cookie côté navigateur pour /deconnexion, extraction de l'email depuis le Principal pour /update-password)
        @param pour chaque paramètre (corps, header, principal, locale)
@return listant tous les codes HTTP possibles et leur condition
        @throws quand une exception peut remonter jusqu'à l'appelant (cas de /activate)

Accountresource
Code · JAVA
java doc stp :

        package sc.liste.noel.gift.controller;

import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import sc.liste.noel.common.constant.Constants;
import sc.liste.noel.common.dto.response.GenericResponse;
import sc.liste.noel.common.exception.ForbiddenModificationException;
import sc.liste.noel.common.service.MessageService;
import sc.liste.noel.gift.dto.GiftDto;
import sc.liste.noel.gift.service.GiftService;

import java.security.Principal;
import java.util.Locale;
import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static sc.liste.noel.common.constant.Constants.API_GIFTLIST_ERROR_KEY;

@RestController
@RequestMapping("/api/cadeau")
public class GiftResource {

    private static final Logger LOGGER = LogManager.getLogger(GiftResource.class);

    private final GiftService giftService;

    private final MessageService messageService;

    public GiftResource(GiftService giftService,
                        MessageService messageService) {
        this.giftService = giftService;
        this.messageService = messageService;
    }


    @PutMapping("/{giftId}")
    public ResponseEntity<GenericResponse> updateGift(Principal principal,
                                                      @RequestBody @Valid GiftDto gift,
                                                      @PathVariable String giftId,
                                                      Locale locale) {
        String email = principal.getName();
        LOGGER.info("Updating gift {} by user {}", giftId, email);
        try {
            giftService.updateGift(Long.valueOf(giftId), gift.getTitle(), gift.getDescription(), gift.getUrl(), gift.getPriorityValue(), email);
            return ResponseEntity.ok(new GenericResponse("Succes", Constants.API_RETURN_OK));
        } catch (ForbiddenModificationException exception) {
            LOGGER.warn("Forbidden attempt to modify gift {} by person {}", giftId, email);
            return ResponseEntity.status(FORBIDDEN).body(new GenericResponse(exception.getMessage(), Constants.API_RETURN_KO));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new GenericResponse(messageService.getMessage(API_GIFTLIST_ERROR_KEY, locale), Constants.API_RETURN_KO));
        }
    }

    @DeleteMapping("/{giftId}")
    public ResponseEntity<GenericResponse> deleteGift(Principal principal,
                                                      @PathVariable String giftId,
                                                      Locale locale) {
        String email = principal.getName();
        try {
            giftService.deleteGift(Long.valueOf(giftId), email);
            return ResponseEntity.ok().body(new GenericResponse("Succes", Constants.API_RETURN_OK));
        } catch (NoSuchElementException e) {
            LOGGER.warn("Gift not found {} by person {}", giftId, email);
            return ResponseEntity.status(FORBIDDEN).body(new GenericResponse(e.getMessage(), Constants.API_RETURN_KO));
        }
        catch (ForbiddenModificationException e) {
            LOGGER.warn("Forbidden attempt to delete gift {} by person {}", giftId, email);
            return ResponseEntity.status(FORBIDDEN).body(new GenericResponse(e.getMessage(), Constants.API_RETURN_KO));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new GenericResponse(messageService.getMessage(API_GIFTLIST_ERROR_KEY, locale), Constants.API_RETURN_KO));
        }

    }
}
Afficher plus
15:39

        Fichier créé, lu un fichier
        Fichier créé, lu un fichier
Javadoc ajoutée sur les 4 éléments de la classe :

Classe — description du rôle du contrôleur, du préfixe de route et du prérequis d'authentification.

Constructeur — @param pour chaque dépendance injectée.

updateGift — comportement, restriction au propriétaire, résolution de l'email depuis le Principal, et tous les codes HTTP de retour.

deleteGift — idem, avec en plus la mention de la notification aux favoris par email. J'ai aussi noté que NoSuchElementException et ForbiddenModificationException retournent tous deux un 403 — si ce n'est pas intentionnel (un 404 serait plus sémantique pour un cadeau introuvable), c'est un point à revoir dans le code.


Giftresource
Code · JAVA 

package sc.liste.noel.giftlist.controller; import jakarta.validation.Valid; import org.apache.logging.log4j.LogManager; import org.apache.logging.log4j.Logger; import org.springframework.http.HttpStatus; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.anno

        pasted

une dernière. D'ailleurs, la doc que tu ajoute est compatible avec springdoc-openapi-starter-webmvc-ui ? Si non, fait en sorte que ça soit compatible.

        15:44
Bonne question — la Javadoc standard (/** */) est lue par springdoc-openapi mais pour une documentation OpenAPI/Swagger complète et précise, il faut y ajouter les annotations @Operation, @ApiResponse, @Parameter, etc. de la librairie io.swagger.v3.oas.annotations. Les deux coexistent sans problème.


        Fichier créé, lu un fichier
        Fichier créé, lu un fichier
Pour cette classe et pour les deux précédentes (AccountResource et GiftResource), voici ce qui a été utilisé pour la compatibilité avec springdoc-openapi :

Annotations Swagger ajoutées (package io.swagger.v3.oas.annotations) :

Annotation	Rôle
@Tag	Regroupe les endpoints sous un label dans l'UI Swagger
@Operation	Titre et description de chaque endpoint
@ApiResponse / @ApiResponses	Documente chaque code HTTP de retour
@Parameter	Décrit les paramètres de chemin (@PathVariable) et de requête (@RequestParam)
Ce qu'il faudrait rétroactivement ajouter aux deux fichiers précédents (AccountResource.java et GiftResource.java) pour qu'ils soient également compatibles : les mêmes annotations. Je peux les regénérer si tu le souhaites.

Dépendance Maven à avoir dans ton pom.xml si ce n'est pas déjà le cas :

xml
        <dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.x.x</version>
</dependency>

Giftlistresource
Code · JAVA 

package sc.liste.noel.gift.controller; import jakarta.validation.Valid; import org.apache.logging.log4j.LogManager; import org.apache.logging.log4j.Logger; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import sc.liste.noel.common.constant.C

        pasted


package sc.liste.noel.account.controller; import jakarta.validation.Valid; import jakarta.validation.constraints.Email; import jakarta.validation.constraints.NotBlank; import org.apache.logging.log4j.LogManager; import org.apache.logging.log4j.Logger; import org.springframework.http.HttpHe

        pasted

tu peux me refaire le travail pour les 2 autres controller ?

        15:48

Exécuté 2 commandes, lu un fichier
Exécuté 2 commandes, lu un fichier
Les deux fichiers sont mis à jour avec la même approche que GiftListResource. Voici ce qui a été ajouté par rapport aux versions précédentes :

AccountResource — 8 endpoints annotés, plus @Parameter sur les @RequestParam et @RequestHeader des endpoints /supprimer et /activate (email, secret, clé d'activation).

                                                                                                                                              GiftResource — 2 endpoints annotés, plus @Parameter sur les @PathVariable giftId.

                                                                                                                                              Récapitulatif des annotations utilisées dans les 3 controllers :

                                                                                                                                              Annotation	Où
                                                                                                                                              @Tag	classe — regroupe les endpoints dans l'UI Swagger
                                                                                                                                              @Operation	méthode — titre + description narrative
                                                                                                                                              @ApiResponse / @ApiResponses	méthode — tous les codes HTTP documentés
                                                                                                                                              @Parameter	paramètre — @PathVariable, @RequestParam, @RequestHeader significatifs

                                                                                                                                              Accountresource
Code · JAVA

                                                                                                                                              Giftresource
                                                                                                                                              Code · JAVA





                                                                                                                                              Claude est une IA et peut faire des erreurs. Veuillez vérifier les réponses.
        Accountresource · JAVA
                                                                                                                                              package sc.liste.noel.account.controller;
 
 
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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

    /** Nom du cookie d'authentification posé et lu par le navigateur. */
    private final static String AUT_TOKEN_KEY = "auth-token";

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
     *         {@code 409 Conflict} si l'email ou le pseudo est déjà utilisé ;
     *         {@code 403 Forbidden} si les CGU ne sont pas acceptées ou si les mots de passe
     *         ne correspondent pas
     */
    @Operation(summary = "Inscrire un nouvel utilisateur",
            description = "Crée un compte, pose un cookie JWT HTTP-only et envoie un email d'activation.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte créé, cookie posé"),
            @ApiResponse(responseCode = "403", description = "CGU non acceptées ou mots de passe différents"),
            @ApiResponse(responseCode = "409", description = "Email ou pseudo déjà utilisé")
    })
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
     *         {@code 404 Not Found} si les identifiants sont incorrects ;
     *         {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Connecter un utilisateur",
            description = "Vérifie les identifiants et pose un cookie JWT HTTP-only valide 24 heures.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Connexion réussie, cookie posé"),
            @ApiResponse(responseCode = "404", description = "Identifiants incorrects"),
            @ApiResponse(responseCode = "500", description = "Erreur interne")
    })
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
     *         {@code 403 Forbidden} si le service mail est désactivé ;
     *         {@code 404 Not Found} si aucun compte ne correspond à l'email ;
     *         {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Mot de passe oublié",
            description = "Génère un nouveau mot de passe et l'envoie par email. Nécessite que le service mail soit actif.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouveau mot de passe envoyé par email"),
            @ApiResponse(responseCode = "403", description = "Service mail désactivé"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable"),
            @ApiResponse(responseCode = "500", description = "Erreur interne")
    })
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
     *         {@code 401 Unauthorized} si l'ancien mot de passe est incorrect ;
     *         {@code 400 Bad Request} si le nouveau mot de passe et sa confirmation
     *         ne correspondent pas ;
     *         {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Modifier le mot de passe",
            description = "Met à jour le mot de passe de l'utilisateur connecté après vérification de l'ancien.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Mot de passe mis à jour"),
            @ApiResponse(responseCode = "400", description = "Nouveau mot de passe et confirmation différents"),
            @ApiResponse(responseCode = "401", description = "Ancien mot de passe incorrect"),
            @ApiResponse(responseCode = "500", description = "Erreur interne")
    })
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
     *         {@code 401 Unauthorized} si le principal est absent ;
     *         {@code 404 Not Found} si le compte associé au principal est introuvable ;
     *         {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Récupérer le profil de session",
            description = "Retourne l'email et le pseudo de l'utilisateur connecté. Renvoie 401 si non authentifié.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session active, profil retourné"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable"),
            @ApiResponse(responseCode = "500", description = "Erreur interne")
    })
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
     *         {@code 401 Unauthorized} si le secret est invalide ;
     *         {@code 404 Not Found} si aucun compte ne correspond à l'email
     */
    @Operation(summary = "Supprimer un compte utilisateur",
            description = "Suppression définitive protégée par un secret d'administration transmis dans l'en-tête 'secret'.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte supprimé"),
            @ApiResponse(responseCode = "401", description = "Secret invalide"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable")
    })
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
     *         {@code 400 Bad Request} si la clé est incorrecte ou le compte déjà activé
     * @throws AccountNotFoundException si aucun compte ne correspond à l'email fourni
     */
    @Operation(summary = "Activer un compte",
            description = "Active le compte via le lien reçu par email. La clé d'activation est à usage unique.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte activé avec succès"),
            @ApiResponse(responseCode = "400", description = "Clé invalide ou compte déjà activé")
    })
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

}
 
