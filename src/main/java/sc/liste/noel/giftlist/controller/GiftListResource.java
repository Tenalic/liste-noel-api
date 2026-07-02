package sc.liste.noel.giftlist.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sc.liste.noel.account.exception.AccountNotFoundException;
import sc.liste.noel.common.constant.Constants;
import sc.liste.noel.common.dto.response.GenericResponse;
import sc.liste.noel.common.exception.ForbiddenModificationException;
import sc.liste.noel.common.exception.GiftListNotFoundException;
import sc.liste.noel.common.service.MessageService;
import sc.liste.noel.gift.dto.GiftDto;
import sc.liste.noel.gift.service.GiftService;
import sc.liste.noel.giftlist.dto.GiftListContextDto;
import sc.liste.noel.giftlist.dto.GiftListDto;
import sc.liste.noel.giftlist.dto.request.CreateGiftListRequest;
import sc.liste.noel.giftlist.dto.request.NameRequest;
import sc.liste.noel.giftlist.dto.request.PublicRequest;
import sc.liste.noel.giftlist.dto.response.GiftListResponse;
import sc.liste.noel.giftlist.dto.response.GiftListsResponse;
import sc.liste.noel.giftlist.dto.response.MyGiftListsResponse;
import sc.liste.noel.giftlist.service.GiftListService;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

import static sc.liste.noel.common.constant.Constants.*;

/**
 * Contrôleur REST gérant les opérations sur les listes de cadeaux.
 *
 * <p>Expose les endpoints sous le préfixe {@code /api/liste} et couvre :
 * la création, la consultation, la suppression, la gestion des favoris,
 * l'ajout de cadeaux et la modification de la visibilité d'une liste.</p>
 *
 * <p>La plupart des opérations nécessitent une authentification : l'email
 * de l'utilisateur est extrait du {@link Principal} fourni par Spring Security.
 * La consultation d'une liste ({@code GET /{shareToken}}) est accessible sans
 * authentification, avec une vue anonymisée dans ce cas.</p>
 */
@Tag(name = "Listes de cadeaux", description = "Opérations sur les listes de cadeaux et leurs favoris")
@RestController
@RequestMapping("/api/liste")
public class GiftListResource {

    private static final Logger LOGGER = LogManager.getLogger(GiftListResource.class);

    private final GiftListService giftListService;

    private final MessageService messageService;

    private final GiftService giftService;

    /**
     * Construit le contrôleur avec ses dépendances.
     *
     * @param giftListService service métier de gestion des listes de cadeaux
     * @param messageService  service d'internationalisation des messages
     * @param giftService     service métier de gestion des cadeaux
     */
    public GiftListResource(GiftListService giftListService,
                            MessageService messageService, GiftService giftService) {
        this.giftListService = giftListService;
        this.messageService = messageService;
        this.giftService = giftService;
    }

    /**
     * Retourne les listes de cadeaux et les favoris de l'utilisateur connecté.
     *
     * @param principal principal Spring Security représentant l'utilisateur connecté
     * @param locale    locale courante pour l'internationalisation des messages d'erreur
     * @return {@code 200 OK} avec les listes personnelles et favorites ;
     * {@code 404 Not Found} si le compte est introuvable
     */
    @Operation(summary = "Récupérer mes listes et mes favoris",
            description = "Retourne les listes de cadeaux appartenant à l'utilisateur connecté ainsi que ses listes favorites.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Listes récupérées avec succès"),
            @ApiResponse(responseCode = "404", description = "Compte introuvable")
    })
    @GetMapping("/mes-listes")
    public ResponseEntity<MyGiftListsResponse> getMyGiftLists(Principal principal, Locale locale) {
        String email = null;
        try {
            email = principal.getName();
            List<GiftListDto> giftLists = giftListService.getGiftListsOfEmail(email);
            List<GiftListDto> favorites = giftListService.getFavoriteGiftListsOfEmail(email);
            return ResponseEntity.ok(new MyGiftListsResponse(giftLists, favorites));
        } catch (AccountNotFoundException e) {
            LOGGER.warn("[mot-de-passe-oublie] account not found {}", email);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new MyGiftListsResponse(messageService.getMessage(ACCOUNT_NOT_FOUND, locale), Constants.API_RETURN_KO));
        } catch (Exception e) {
            LOGGER.error("Error while retrieving the gift lists for {}", email, e);
            throw e;
        }
    }

    /**
     * Retourne le détail d'une liste de cadeaux avec son contexte utilisateur.
     *
     * <p>Si l'utilisateur n'est pas authentifié ({@link Principal} absent), les
     * informations sensibles des cadeaux (titulaire, statut pris) sont anonymisées.
     * Dans le cas contraire, le contexte indique si la liste appartient à l'utilisateur
     * et s'il l'a mise en favori.</p>
     *
     * @param principal  principal Spring Security, ou {@code null} si non authentifié
     * @param shareToken identifiant de la liste, transmis dans le chemin de la requête
     * @param locale     locale courante pour l'internationalisation des messages d'erreur
     * @return {@code 200 OK} avec le détail de la liste et son contexte ;
     * {@code 404 Not Found} si la liste est introuvable ;
     * {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Récupérer une liste de cadeaux",
            description = "Retourne le détail d'une liste avec son contexte : propriété, favori, et anonymisation si non connecté.")
    @ApiResponse(responseCode = "200", description = "Liste récupérée avec succès")
    @ApiResponse(responseCode = "404", description = "Liste introuvable")
    @ApiResponse(responseCode = "500", description = "Erreur interne")
    @GetMapping("/{shareToken}")
    public ResponseEntity<GiftListResponse> getGiftList(
            Principal principal,
            @Parameter(description = "Identifiant de la liste de cadeaux") @PathVariable String shareToken,
            Locale locale) {
        String email = principal != null ? principal.getName() : null;
        try {
            GiftListContextDto giftList = giftListService.getGiftListWithContext(shareToken, email);
            return ResponseEntity.ok(new GiftListResponse("Succes", Constants.API_RETURN_OK, giftList, giftList.isOwnedByCurrentUser(), giftList.isFavorite()));
        } catch (GiftListNotFoundException e) {
            LOGGER.warn("The gift list {} was not found in the database", shareToken);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new GiftListResponse(messageService.getMessage(GIFTLIST_NOT_FOUND, locale), Constants.API_RETURN_KO));
        } catch (Exception e) {
            LOGGER.error("Error while retrieving the gift list {}", shareToken, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Bascule l'état favori d'une liste pour l'utilisateur connecté.
     *
     * <p>Si la liste est déjà en favori, elle est retirée. Sinon, elle est ajoutée.
     * Cette opération est idempotente du point de vue du résultat final.</p>
     *
     * @param principal  principal Spring Security représentant l'utilisateur connecté
     * @param shareToken identifiant de la liste, transmis dans le chemin de la requête
     * @return {@code 200 OK} si la bascule a réussi ;
     * {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Ajouter ou retirer une liste des favoris",
            description = "Bascule l'état favori d'une liste : l'ajoute si absente, la retire si présente.")
    @ApiResponse(responseCode = "200", description = "Favori mis à jour avec succès")
    @ApiResponse(responseCode = "500", description = "Erreur interne")
    @PostMapping("/{shareToken}/favoris")
    public ResponseEntity<GenericResponse> addFavorite(
            Principal principal,
            @Parameter(description = "Identifiant de la liste de cadeaux") @PathVariable String shareToken) {
        String email = principal.getName();
        try {
            giftListService.toggleFavorite(shareToken, email);
            return ResponseEntity.ok(new GenericResponse("Succes", Constants.API_RETURN_OK));
        } catch (Exception e) {
            LOGGER.error("Error while updating favorite {} {}", shareToken, email, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Crée une nouvelle liste de cadeaux pour l'utilisateur connecté.
     *
     * @param principal       principal Spring Security représentant l'utilisateur connecté
     * @param giftListRequest corps de la requête contenant le nom et la visibilité de la liste
     * @return {@code 200 OK} si la liste a été créée avec succès
     */
    @Operation(summary = "Créer une liste de cadeaux",
            description = "Crée une nouvelle liste de cadeaux associée à l'utilisateur connecté.")
    @ApiResponse(responseCode = "200", description = "Liste créée avec succès")
    @PostMapping("/creer")
    public ResponseEntity<GenericResponse> createGiftList(
            Principal principal,
            @RequestBody @Valid CreateGiftListRequest giftListRequest) {
        String email = principal.getName();
        giftListService.createGiftList(email, giftListRequest.name(), giftListRequest.isPublic());
        return ResponseEntity.ok(new GenericResponse("Succes", Constants.API_RETURN_OK));
    }

    /**
     * Supprime définitivement une liste de cadeaux et ses favoris associés.
     *
     * <p>Seul le propriétaire de la liste est autorisé à la supprimer.
     * Les entrées favoris référençant cette liste sont également supprimées.</p>
     *
     * @param locale     locale courante pour l'internationalisation des messages
     * @param shareToken identifiant de la liste à supprimer, transmis dans le chemin de la requête
     * @param principal  principal Spring Security représentant l'utilisateur connecté
     * @return {@code 200 OK} avec un message de confirmation ;
     * {@code 403 Forbidden} si l'utilisateur n'est pas propriétaire de la liste ;
     * {@code 404 Not Found} si la liste est introuvable ;
     * {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Supprimer une liste de cadeaux",
            description = "Supprime la liste et toutes ses entrées favoris. Réservé au propriétaire de la liste.")
    @ApiResponse(responseCode = "200", description = "Liste supprimée avec succès")
    @ApiResponse(responseCode = "403", description = "Suppression interdite : l'utilisateur n'est pas propriétaire")
    @ApiResponse(responseCode = "404", description = "Liste introuvable")
    @ApiResponse(responseCode = "500", description = "Erreur interne")
    @DeleteMapping("/{shareToken}")
    public ResponseEntity<GenericResponse> deleteGiftList(
            Locale locale,
            @Parameter(description = "Identifiant de la liste de cadeaux") @PathVariable String shareToken,
            Principal principal) {
        String email = principal.getName();
        try {
            String response = giftListService.deleteGiftList(shareToken, email);
            return ResponseEntity.ok(new GenericResponse(response, Constants.API_RETURN_OK));
        } catch (ForbiddenModificationException e) {
            LOGGER.warn("warning while deleting the gift list {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new GenericResponse(messageService.getMessage(DELETION_FORBIDDEN, locale), Constants.API_RETURN_KO));
        } catch (GiftListNotFoundException e) {
            LOGGER.warn("warning while deleting the gift list {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new GenericResponse(messageService.getMessage(GIFTLIST_NOT_FOUND, locale), Constants.API_RETURN_KO));
        } catch (Exception e) {
            LOGGER.error("Error while deleting the gift list {} for the email {}", shareToken, email, e);
            return ResponseEntity.internalServerError().body(new GenericResponse(messageService.getMessage(API_GIFTLIST_ERROR_KEY, locale), Constants.API_RETURN_KO));
        }
    }

    /**
     * Ajoute un cadeau à une liste de cadeaux existante.
     *
     * <p>L'utilisateur connecté est enregistré comme référence du propriétaire lors
     * de l'ajout, mais aucune vérification de propriété n'est effectuée à ce stade —
     * celle-ci intervient lors des opérations de modification ou suppression.</p>
     *
     * @param principal  principal Spring Security représentant l'utilisateur connecté
     * @param gift       corps de la requête contenant les informations du cadeau
     *                   (titre, URL, description, priorité)
     * @param shareToken identifiant de la liste cible, transmis dans le chemin de la requête
     * @param locale     locale courante pour l'internationalisation des messages d'erreur
     * @return {@code 200 OK} si le cadeau a été ajouté avec succès ;
     * {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Ajouter un cadeau à une liste",
            description = "Ajoute un nouveau cadeau à la liste identifiée par shareToken.")
    @ApiResponse(responseCode = "200", description = "Cadeau ajouté avec succès")
    @ApiResponse(responseCode = "500", description = "Erreur interne")
    @PostMapping("/{shareToken}/cadeau")
    public ResponseEntity<GenericResponse> addGift(
            Principal principal,
            @RequestBody @Valid GiftDto gift,
            @Parameter(description = "Identifiant de la liste de cadeaux") @PathVariable String shareToken,
            Locale locale) {
        String email = principal.getName();
        LOGGER.info("Adding gift {} by user {}", gift.getTitle(), email);
        try {
            giftService.addGiftToGiftList(gift.getTitle(), gift.getUrl(), gift.getDescription(), shareToken, gift.getPriorityValue());
            return ResponseEntity.ok(new GenericResponse("Succes", Constants.API_RETURN_OK));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new GenericResponse(messageService.getMessage(API_GIFTLIST_ERROR_KEY, locale), Constants.API_RETURN_KO));
        }
    }

    /**
     * Retourne toutes les listes de cadeaux publiques, avec filtrage optionnel par nom.
     *
     * <p>Cet endpoint est accessible sans authentification. Les emails des propriétaires
     * sont remplacés par leurs pseudos dans la réponse.</p>
     *
     * @param search terme de recherche sur le nom de la liste ; chaîne vide par défaut
     *               pour retourner toutes les listes publiques
     * @return {@code 200 OK} avec la liste des listes publiques correspondantes
     */
    @Operation(summary = "Récupérer les listes publiques",
            description = "Retourne toutes les listes publiques, filtrables par nom via le paramètre 'recherche'. Les emails sont remplacés par les pseudos.")
    @ApiResponse(responseCode = "200", description = "Listes publiques récupérées avec succès")
    @GetMapping("/publiques")
    public ResponseEntity<GiftListsResponse> getPublicGiftLists(
            @Parameter(description = "Terme de recherche sur le nom de la liste (optionnel)")
            @RequestParam(name = "recherche", defaultValue = "") String search) {
        List<GiftListDto> giftLists = giftListService.getGiftLists(true, search);
        GiftListsResponse response = new GiftListsResponse("Succes", Constants.API_RETURN_OK, giftLists);
        return ResponseEntity.ok(response);
    }

    /**
     * Met à jour la visibilité publique ou privée d'une liste de cadeaux.
     *
     * <p>Seul le propriétaire de la liste est autorisé à modifier sa visibilité.</p>
     *
     * @param shareToken    identifiant de la liste, transmis dans le chemin de la requête
     * @param publicRequest corps de la requête contenant le nouveau statut de visibilité
     * @param locale        locale courante pour l'internationalisation des messages d'erreur
     * @param principal     principal Spring Security représentant l'utilisateur connecté
     * @return {@code 200 OK} si la visibilité a été mise à jour ;
     * {@code 404 Not Found} si la liste est introuvable ;
     * {@code 403 Forbidden} si l'utilisateur n'est pas propriétaire de la liste
     */
    @Operation(summary = "Modifier la visibilité d'une liste",
            description = "Passe la liste en public ou en privé. Réservé au propriétaire de la liste.")
    @ApiResponse(responseCode = "200", description = "Visibilité mise à jour avec succès")
    @ApiResponse(responseCode = "403", description = "Modification interdite : l'utilisateur n'est pas propriétaire")
    @ApiResponse(responseCode = "404", description = "Liste introuvable")
    @PutMapping("/{shareToken}/publique")
    public ResponseEntity<GenericResponse> updatePublic(
            @Parameter(description = "Identifiant de la liste de cadeaux") @PathVariable String shareToken,
            @RequestBody @Valid PublicRequest publicRequest,
            Locale locale,
            Principal principal) {
        String email = principal.getName();
        try {
            giftListService.updatePublic(shareToken, publicRequest.isPublic(), email);
            return ResponseEntity.ok(new GenericResponse("Succes", Constants.API_RETURN_OK));
        } catch (GiftListNotFoundException e) {
            LOGGER.warn(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new GenericResponse(messageService.getMessage(GIFTLIST_NOT_FOUND, locale), Constants.API_RETURN_KO));
        } catch (ForbiddenModificationException e) {
            LOGGER.warn(e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new GenericResponse(messageService.getMessage(MODIFICATION_FORBIDDEN, locale), Constants.API_RETURN_KO));
        }
    }

    @PutMapping("/{shareToken}/nom")
    public ResponseEntity<GenericResponse> updateName(
            @Parameter(description = "Identifiant de la liste de cadeaux") @PathVariable String shareToken,
            @RequestBody @Valid NameRequest nameRequest,
            Locale locale,
            Principal principal) {
        String email = principal.getName();
        try {
            giftListService.updateName(shareToken, nameRequest.listName(), email);
            return ResponseEntity.ok(new GenericResponse("Succes", Constants.API_RETURN_OK));
        } catch (GiftListNotFoundException e) {
            LOGGER.warn(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new GenericResponse(messageService.getMessage(GIFTLIST_NOT_FOUND, locale), Constants.API_RETURN_KO));
        } catch (ForbiddenModificationException e) {
            LOGGER.warn(e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new GenericResponse(messageService.getMessage(MODIFICATION_FORBIDDEN, locale), Constants.API_RETURN_KO));
        }
    }
}