package sc.liste.noel.gift.service;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import sc.liste.noel.account.exception.AccountNotFoundException;
import sc.liste.noel.account.service.AccountService;
import sc.liste.noel.common.exception.ForbiddenModificationException;
import sc.liste.noel.common.exception.GiftListNotFoundException;
import sc.liste.noel.common.service.MailService;
import sc.liste.noel.gift.db.entity.GiftEntity;
import sc.liste.noel.gift.db.repo.GiftRepo;
import sc.liste.noel.giftlist.service.GiftListService;

@Service
public class GiftService {

    private final GiftRepo giftRepo;
    private final GiftListService giftListService;
    private final AccountService accountService;

    public GiftService(GiftRepo giftRepo, MailService mailService, GiftListService giftListService, AccountService accountService) {
        this.giftRepo = giftRepo;
        this.giftListService = giftListService;
        this.accountService = accountService;
    }

    public void addGiftToGiftList(String title, String url, String description, String shareToken, int priority) throws GiftListNotFoundException {
        Long giftListId = giftListService.getIdListEntityByShareToken(shareToken);
        GiftEntity giftEntity = new GiftEntity();
        giftEntity.setDescription(description);
        giftEntity.setGiftListId(giftListId);
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

        giftListService.notifyGiftModificationToFavorites(giftEntity.getGiftListId(), giftEntity.getTitle(), giftEntity.getDescription(), giftEntity.getUrl(), giftEntity.getPriorityValue(), titleUpdate, descriptionUpdate, urlUpdate, priorityUpdate);

        giftEntity.setTitle(titleUpdate);
        giftEntity.setDescription(descriptionUpdate);
        giftEntity.setUrl(urlUpdate);
        giftEntity.setPriorityValue(priorityUpdate);

        giftRepo.save(giftEntity);
    }

    /**
     * Bascule l'état « offert » d'un cadeau pour l'utilisateur courant.
     *
     * <p>Si le cadeau n'est pas encore pris, l'utilisateur courant le prend.
     * Si le cadeau est déjà pris par l'utilisateur courant, il le relâche.
     * Si le cadeau est pris par quelqu'un d'autre, une exception est levée.</p>
     *
     * @param giftId identifiant du cadeau
     * @param email  email de l'utilisateur courant
     * @throws ForbiddenModificationException si le cadeau est pris par un autre utilisateur
     *                                        ou si l'utilisateur est le propriétaire de la liste
     */
    @Transactional
    public void toggleOfferGift(Long giftId, String email) throws ForbiddenModificationException {
        GiftEntity giftEntity = giftRepo.findByGiftId(giftId).orElseThrow();

        // Le propriétaire de la liste ne peut pas offrir ses propres cadeaux
        if (giftRepo.existsByGiftIdAndListOwner(giftId, email)) {
            throw new ForbiddenModificationException("You cannot offer a gift from your own list.");
        }

        if (Boolean.TRUE.equals(giftEntity.getTaken())) {
            // Si c'est l'utilisateur courant qui avait pris le cadeau, il le relâche
            if (email.equals(giftEntity.getHolder())) {
                giftEntity.setTaken(false);
                giftEntity.setHolder(null);
                giftEntity.setHolderPseudo(null);
            } else {
                throw new ForbiddenModificationException("This gift is already taken by someone else.");
            }
        } else {
            // Le cadeau est libre, l'utilisateur courant le prend
            String pseudo;
            try {
                pseudo = accountService.getPseudo(email);
            } catch (AccountNotFoundException e) {
                pseudo = email;
            }
            giftEntity.setTaken(true);
            giftEntity.setHolder(email);
            giftEntity.setHolderPseudo(pseudo);
        }

        giftRepo.save(giftEntity);
    }
}
