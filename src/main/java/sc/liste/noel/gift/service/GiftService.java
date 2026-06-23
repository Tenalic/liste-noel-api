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
