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
import sc.liste.noel.giftlist.service.GiftListService;

import java.security.Principal;
import java.util.Locale;
import java.util.NoSuchElementException;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static sc.liste.noel.common.constant.Constants.API_GIFTLIST_ERROR_KEY;

@RestController
@RequestMapping("/api/cadeau")
public class GiftResource {

    private static final Logger LOGGER = LogManager.getLogger(GiftResource.class);

    private final GiftListService giftListService;
    private final GiftService giftService;

    private final MessageService messageService;

    public GiftResource(GiftListService giftListService, GiftService giftService,
                        MessageService messageService) {
        this.giftListService = giftListService;
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
