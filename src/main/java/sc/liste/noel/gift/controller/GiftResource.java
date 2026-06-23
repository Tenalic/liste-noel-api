package sc.liste.noel.gift.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Contrôleur REST gérant les opérations sur les cadeaux.
 *
 * <p>Expose les endpoints sous le préfixe {@code /api/cadeau} et couvre
 * la modification et la suppression d'un cadeau. Toutes les opérations
 * nécessitent une authentification : l'email de l'utilisateur est extrait
 * du {@link Principal} fourni par Spring Security.</p>
 */
@Tag(name = "Cadeaux", description = "Modification et suppression de cadeaux dans une liste")
@RestController
@RequestMapping("/api/cadeau")
public class GiftResource {

    private static final Logger LOGGER = LogManager.getLogger(GiftResource.class);

    private final GiftService giftService;

    private final MessageService messageService;

    /**
     * Construit le contrôleur avec ses dépendances.
     *
     * @param giftService    service métier de gestion des cadeaux
     * @param messageService service d'internationalisation des messages
     */
    public GiftResource(GiftService giftService,
                        MessageService messageService) {
        this.giftService = giftService;
        this.messageService = messageService;
    }

    /**
     * Met à jour les informations d'un cadeau existant.
     *
     * <p>Seul le propriétaire de la liste contenant le cadeau est autorisé
     * à effectuer cette modification. L'email de l'utilisateur est résolu
     * depuis le {@link Principal} — il n'est pas accepté en paramètre de la requête.</p>
     *
     * @param principal principal Spring Security représentant l'utilisateur connecté
     * @param gift      corps de la requête contenant les nouvelles valeurs du cadeau
     *                  (titre, description, URL, priorité)
     * @param giftId    identifiant du cadeau à modifier, transmis dans le chemin de la requête
     * @param locale    locale courante pour l'internationalisation des messages d'erreur
     * @return {@code 200 OK} si la mise à jour a réussi ;
     *         {@code 403 Forbidden} si l'utilisateur ne possède pas la liste du cadeau ;
     *         {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Modifier un cadeau",
            description = "Met à jour le titre, la description, l'URL et la priorité d'un cadeau. Réservé au propriétaire de la liste.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadeau mis à jour avec succès"),
            @ApiResponse(responseCode = "403", description = "Modification interdite : l'utilisateur n'est pas propriétaire"),
            @ApiResponse(responseCode = "500", description = "Erreur interne")
    })
    @PutMapping("/{giftId}")
    public ResponseEntity<GenericResponse> updateGift(
            Principal principal,
            @RequestBody @Valid GiftDto gift,
            @Parameter(description = "Identifiant du cadeau à modifier", required = true)
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

    /**
     * Supprime définitivement un cadeau d'une liste.
     *
     * <p>Seul le propriétaire de la liste contenant le cadeau est autorisé
     * à effectuer cette suppression. Les favoris de la liste concernée sont
     * notifiés par email de la suppression. L'email de l'utilisateur est résolu
     * depuis le {@link Principal} — il n'est pas accepté en paramètre de la requête.</p>
     *
     * @param principal principal Spring Security représentant l'utilisateur connecté
     * @param giftId    identifiant du cadeau à supprimer, transmis dans le chemin de la requête
     * @param locale    locale courante pour l'internationalisation des messages d'erreur
     * @return {@code 200 OK} si la suppression a réussi ;
     *         {@code 403 Forbidden} si le cadeau est introuvable ou si l'utilisateur
     *         ne possède pas la liste du cadeau ;
     *         {@code 500 Internal Server Error} en cas d'erreur inattendue
     */
    @Operation(summary = "Supprimer un cadeau",
            description = "Supprime un cadeau et notifie les favoris de la liste par email. Réservé au propriétaire de la liste.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cadeau supprimé avec succès"),
            @ApiResponse(responseCode = "403", description = "Suppression interdite : cadeau introuvable ou utilisateur non propriétaire"),
            @ApiResponse(responseCode = "500", description = "Erreur interne")
    })
    @DeleteMapping("/{giftId}")
    public ResponseEntity<GenericResponse> deleteGift(
            Principal principal,
            @Parameter(description = "Identifiant du cadeau à supprimer", required = true)
            @PathVariable String giftId,
            Locale locale) {
        String email = principal.getName();
        try {
            giftService.deleteGift(Long.valueOf(giftId), email);
            return ResponseEntity.ok().body(new GenericResponse("Succes", Constants.API_RETURN_OK));
        } catch (NoSuchElementException e) {
            LOGGER.warn("Gift not found {} by person {}", giftId, email);
            return ResponseEntity.status(FORBIDDEN).body(new GenericResponse(e.getMessage(), Constants.API_RETURN_KO));
        } catch (ForbiddenModificationException e) {
            LOGGER.warn("Forbidden attempt to delete gift {} by person {}", giftId, email);
            return ResponseEntity.status(FORBIDDEN).body(new GenericResponse(e.getMessage(), Constants.API_RETURN_KO));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new GenericResponse(messageService.getMessage(API_GIFTLIST_ERROR_KEY, locale), Constants.API_RETURN_KO));
        }
    }
}