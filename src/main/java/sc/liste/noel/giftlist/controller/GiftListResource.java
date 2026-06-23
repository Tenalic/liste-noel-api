package sc.liste.noel.giftlist.controller;

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
import sc.liste.noel.giftlist.dto.request.PublicRequest;
import sc.liste.noel.giftlist.dto.response.GiftListResponse;
import sc.liste.noel.giftlist.dto.response.GiftListsResponse;
import sc.liste.noel.giftlist.dto.response.MyGiftListsResponse;
import sc.liste.noel.giftlist.service.GiftListService;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

import static sc.liste.noel.common.constant.Constants.*;

@RestController
@RequestMapping("/api/liste")
public class GiftListResource {

    private static final Logger LOGGER = LogManager.getLogger(GiftListResource.class);

    private final GiftListService giftListService;

    private final MessageService messageService;

    private final GiftService giftService;

    public GiftListResource(GiftListService giftListService,
                            MessageService messageService, GiftService giftService) {
        this.giftListService = giftListService;
        this.messageService = messageService;
        this.giftService = giftService;
    }

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

    @GetMapping("/{giftListId}")
    public ResponseEntity<GiftListResponse> getGiftList(Principal principal,
                                                        @PathVariable String giftListId,
                                                        Locale locale) {
        String email = principal != null ? principal.getName() : null;
        try {
            GiftListContextDto giftList = giftListService.getGiftListWithContext(Long.valueOf(giftListId), email);
            return ResponseEntity.ok(new GiftListResponse("Succes", Constants.API_RETURN_OK, giftList, giftList.isOwnedByCurrentUser(), giftList.isFavorite()));
        } catch (GiftListNotFoundException e) {
            LOGGER.warn("The gift list {} was not found in the database", giftListId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new GiftListResponse(messageService.getMessage(GIFTLIST_NOT_FOUND, locale), Constants.API_RETURN_KO));
        } catch (Exception e) {
            LOGGER.error("Error while retrieving the gift list " + giftListId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/{giftListId}/favoris")
    public ResponseEntity<GenericResponse> addFavorite(Principal principal,
                                                       @PathVariable String giftListId) {
        String email = principal.getName();
        try {
            giftListService.toggleFavorite(Long.valueOf(giftListId), email);
            return ResponseEntity.ok(new GenericResponse("Succes", Constants.API_RETURN_OK));
        } catch (Exception e) {
            LOGGER.error("Error while updating favorite {} {}", giftListId, email, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/creer")
    public ResponseEntity<GenericResponse> createGiftList(Principal principal,
                                                          @RequestBody CreateGiftListRequest giftListRequest) {
        String email = principal.getName();
        giftListService.createGiftList(email, giftListRequest.name(), giftListRequest.isPublic());
        return ResponseEntity.ok(new GenericResponse("Succes", Constants.API_RETURN_OK));
    }

    @DeleteMapping("/{giftListId}")
    public ResponseEntity<GenericResponse> deleteGiftList(
            Locale locale, @PathVariable String giftListId,
            Principal principal) {
        String email = principal.getName();
        try {
            String response = giftListService.deleteGiftList(Long.valueOf(giftListId), email);
            return ResponseEntity.ok(new GenericResponse(response, Constants.API_RETURN_OK));
        } catch (ForbiddenModificationException e) {
            LOGGER.warn("warning while deleting the gift list {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new GenericResponse(messageService.getMessage(DELETION_FORBIDDEN, locale), Constants.API_RETURN_KO));
        } catch (GiftListNotFoundException e) {
            LOGGER.warn("warning while deleting the gift list {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new GenericResponse(messageService.getMessage(GIFTLIST_NOT_FOUND, locale), Constants.API_RETURN_KO));
        } catch (Exception e) {
            LOGGER.error("Error while deleting the gift list " + giftListId + " for the email " + email, e);
            return ResponseEntity.internalServerError().body(new GenericResponse(messageService.getMessage(API_GIFTLIST_ERROR_KEY, locale), Constants.API_RETURN_KO));
        }
    }

    @PostMapping("/{giftListId}/cadeau")
    public ResponseEntity<GenericResponse> addGift(Principal principal,
                                                   @RequestBody GiftDto gift,
                                                   @PathVariable String giftListId,
                                                   Locale locale) {
        String email = principal.getName();
        LOGGER.info("Adding gift {} by user {}", gift.getTitle(), email);
        try {
            giftService.addGiftToGiftList(gift.getTitle(), gift.getUrl(), gift.getDescription(), giftListId, email, gift.getPriorityValue());
            return ResponseEntity.ok(new GenericResponse("Succes", Constants.API_RETURN_OK));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(new GenericResponse(messageService.getMessage(API_GIFTLIST_ERROR_KEY, locale), Constants.API_RETURN_KO));
        }
    }

    @GetMapping("/publiques")
    public ResponseEntity<GiftListsResponse> getPublicGiftLists(@RequestParam(name = "recherche", defaultValue = "") String search, Locale locale) {
        try {
            List<GiftListDto> giftLists = giftListService.getGiftLists(true, search);

            GiftListsResponse response = new GiftListsResponse("Succes", Constants.API_RETURN_OK, giftLists);

            return ResponseEntity.ok(response);
        } catch (GiftListNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new GiftListsResponse(messageService.getMessage(GIFTLIST_NOT_FOUND, locale), Constants.API_RETURN_KO));
        }
    }

    @PutMapping("/{giftListId}/publique")
    public ResponseEntity<GenericResponse> updatePublic(@PathVariable String giftListId,
                                                        @RequestBody PublicRequest publicRequest,
                                                        Locale locale,
                                                        Principal principal) {
        String email = principal.getName();
        try {
            giftListService.updatePublic(Long.valueOf(giftListId), publicRequest.isPublic(), email);
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
