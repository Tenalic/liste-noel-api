package sc.liste.noel.liste_noel.back.ressource;

import jakarta.validation.constraints.NotBlank;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sc.liste.noel.liste_noel.back.dto.CreationListeRequest;
import sc.liste.noel.liste_noel.back.dto.GeneriqueResponse;
import sc.liste.noel.liste_noel.back.dto.ListeReponse;
import sc.liste.noel.liste_noel.back.dto.MesListesResponse;
import sc.liste.noel.liste_noel.back.service.ListeServiceInterface;
import sc.liste.noel.liste_noel.back.service.SecretServiceInterface;
import sc.liste.noel.liste_noel.common.dto.ListeDto;
import sc.liste.noel.liste_noel.common.dto.ListeContexteDto;
import sc.liste.noel.liste_noel.common.service.MessageService;
import sc.liste.noel.liste_noel.front.constante.Constantes;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

import static sc.liste.noel.liste_noel.front.constante.Constantes.API_LISTE_ERREUR_KEY;
import static sc.liste.noel.liste_noel.front.constante.Constantes.API_SECRET_INVALID_KEY;

@RestController
@RequestMapping("/api/liste")
public class ListeRessource {

    private static final Logger LOGGER = LogManager.getLogger(ListeRessource.class);

    private final ListeServiceInterface listeServiceInterface;

    private final SecretServiceInterface secretService;

    private final MessageService messageService;

    public ListeRessource(ListeServiceInterface listeServiceInterface, SecretServiceInterface secretService, MessageService messageService) {
        this.listeServiceInterface = listeServiceInterface;
        this.secretService = secretService;
        this.messageService = messageService;
    }

    @GetMapping("/mes-listes")
    public ResponseEntity<MesListesResponse> getMesListes(Principal principal) {
        String email = principal.getName();
        try {
            List<ListeDto> listes = listeServiceInterface.getListesOfEmail(email);
            List<ListeDto> favoris = listeServiceInterface.getListeFavorisOfEmail(email);
            return ResponseEntity.ok(new MesListesResponse(listes, favoris));
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la récupération des listes pour " + email, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{idListe}")
    public ResponseEntity<ListeReponse> getUneListe(Principal principal, @PathVariable String idListe) {
        String email = principal.getName();
        try {
            ListeContexteDto liste = listeServiceInterface.getListeAvecContexte(Long.valueOf(idListe), email);
            return ResponseEntity.ok(new ListeReponse("Succes", Constantes.RETOUR_API_OK, liste, liste.isEstProprietaire(), liste.isEstFavoris()));
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la récupération de la listes " + idListe, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/creer")
    public ResponseEntity<GeneriqueResponse> creerUneListe(Principal principal, @RequestBody CreationListeRequest listeRequest) {
        String email = principal.getName();
        listeServiceInterface.creerListe(email, listeRequest.getNomListe());
        return ResponseEntity.ok(new GeneriqueResponse("Succes", Constantes.RETOUR_API_OK));
    }

    @DeleteMapping("/{idListe}")
    public ResponseEntity<GeneriqueResponse> supprimerUneListe(
            Locale locale, @PathVariable String idListe, Principal principal) {
        String email = principal.getName();
        try {
            String response = listeServiceInterface.supprimerListe(Long.valueOf(idListe));
            return ResponseEntity.ok(new GeneriqueResponse(response, Constantes.RETOUR_API_OK));
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la suppression de la liste " + idListe + " pour l'email " + email, e);
            return ResponseEntity.internalServerError().body(new GeneriqueResponse(messageService.getMessage(API_LISTE_ERREUR_KEY, locale), Constantes.RETOUR_API_KO));
        }
    }

    @DeleteMapping("/supprimer-liste")
    public ResponseEntity<GeneriqueResponse> supprimerListe(
            @RequestParam @NotBlank String email,
            @RequestParam @NotBlank String nomListe,
            @RequestHeader(value = "secret") String secret,
            Locale locale) {
        try {
            if (!secretService.verifierSecret(secret)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new GeneriqueResponse(messageService.getMessage(API_SECRET_INVALID_KEY, locale), Constantes.RETOUR_API_KO));
            }
            String response = listeServiceInterface.supprimerListe(nomListe, email);
            return ResponseEntity.ok(new GeneriqueResponse(response, Constantes.RETOUR_API_OK));
        } catch (Exception e) {
            LOGGER.error("Erreur lors de la suppression de la liste " + nomListe + " pour l'email " + email, e);
            return ResponseEntity.internalServerError().body(new GeneriqueResponse(messageService.getMessage(API_LISTE_ERREUR_KEY, locale), Constantes.RETOUR_API_KO));
        }
    }
}
