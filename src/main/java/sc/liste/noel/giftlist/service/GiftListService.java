package sc.liste.noel.giftlist.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sc.liste.noel.account.db.repo.AccountRepo;
import sc.liste.noel.common.exception.ForbiddenModificationException;
import sc.liste.noel.common.exception.GiftListNotFoundException;
import sc.liste.noel.common.service.EmailTemplateService;
import sc.liste.noel.common.service.MailService;
import sc.liste.noel.gift.mapper.GiftMapper;
import sc.liste.noel.giftlist.db.entity.FavoriteEntity;
import sc.liste.noel.giftlist.db.entity.GiftListEntity;
import sc.liste.noel.giftlist.db.repo.FavoriteRepo;
import sc.liste.noel.giftlist.db.repo.GiftListRepo;
import sc.liste.noel.giftlist.dto.GiftListContextDto;
import sc.liste.noel.giftlist.dto.GiftListDto;
import sc.liste.noel.giftlist.mapper.GiftListMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class GiftListService {

    private final GiftListRepo giftListRepo;

    private final FavoriteRepo favoriteRepo;

    private final AccountRepo accountRepo;

    @Value("${base_url}")
    private String baseUrl;

    private final MailService mailService;

    @Value("${send_email_active}")
    private Boolean mailServiceEnabled;

    private final EmailTemplateService emailTemplateService;

    public GiftListService(GiftListRepo giftListRepo, FavoriteRepo favoriteRepo, AccountRepo accountRepo, MailService mailService, EmailTemplateService emailTemplateService) {
        this.giftListRepo = giftListRepo;
        this.favoriteRepo = favoriteRepo;
        this.accountRepo = accountRepo;
        this.mailService = mailService;
        this.emailTemplateService = emailTemplateService;
    }


    public void createGiftList(String owner, String name, boolean isPublic) {

        GiftListEntity giftListEntity = new GiftListEntity();
        giftListEntity.setName(name);
        giftListEntity.setOwner(owner);
        giftListEntity.setIsPublic(isPublic);

        giftListRepo.save(giftListEntity);
    }


    public List<GiftListDto> getGiftListsOfEmail(String email) {
        List<GiftListEntity> giftListEntityList = giftListRepo.findByOwner(email);
        return GiftListMapper.entitiesToDtosWithoutGifts(giftListEntityList);
    }


    public GiftListDto getGiftListById(Long id) throws GiftListNotFoundException {
        GiftListEntity giftListEntity = giftListRepo.findByGiftListId(id);
        GiftListDto giftListDto = GiftListMapper.entityToDto(giftListEntity);
        if (giftListDto != null) {
            giftListDto.setShareUrl(GiftListMapper.buildShareUrl(baseUrl, id));
            return giftListDto;
        } else {
            throw new GiftListNotFoundException("Gift list not found: " + id);
        }
    }

    // FIXME create a GiftListSimpleEntity repo without the gifts (perf)
    public List<GiftListDto> getGiftLists(boolean isPublic, String name) throws GiftListNotFoundException {
        List<GiftListEntity> giftListEntities;
        boolean isSearchByName = name != null && !name.isBlank();
        if (isSearchByName) {
            giftListEntities = giftListRepo.findByIsPublicAndNameContainingIgnoreCase(isPublic, name);
        } else {
            giftListEntities = giftListRepo.findByIsPublic(isPublic);
        }
        List<GiftListDto> giftListDtos = GiftListMapper.entitiesToDtosWithoutGifts(giftListEntities);

        if (giftListDtos != null) {
            giftListDtos.forEach(giftListDto -> {
                        giftListDto.setShareUrl(GiftListMapper.buildShareUrl(baseUrl, giftListDto.getGiftListId()));
                        this.replaceEmailsWithPseudo(giftListDto);
                    }
            );
        } else {
            throw new GiftListNotFoundException("No gift list found" + (isSearchByName ? " " + name : ""));
        }
        return giftListDtos;
    }

    @Transactional

    public void updatePublic(Long giftListId, boolean isPublic, String email) throws ForbiddenModificationException, GiftListNotFoundException {
        GiftListEntity giftListEntity = giftListRepo.findByGiftListId(giftListId);
        if (giftListEntity == null) {
            throw new GiftListNotFoundException("Gift list not found");
        }
        if (giftListEntity.getOwner().equals(email)) {
            giftListEntity.setIsPublic(isPublic);
            giftListRepo.save(giftListEntity);
        } else {
            throw new ForbiddenModificationException("The gift list does not belong to the user");
        }

    }


    public List<GiftListDto> getFavoriteGiftListsOfEmail(String email) {
        List<FavoriteEntity> favoriteEntityList = favoriteRepo.findByEmail(email);

        if (favoriteEntityList == null) {
            return null;
        }
        List<GiftListEntity> list = new ArrayList<>();

        for (FavoriteEntity favoriteEntity : favoriteEntityList) {
            GiftListEntity giftListEntity = giftListRepo.findByGiftListId(favoriteEntity.getGiftListId());
            if (giftListEntity != null) {
                list.add(giftListEntity);
            }
        }

        return replaceEmailsWithPseudo(GiftListMapper.entitiesToDtosWithoutGifts(list));
    }

    private List<GiftListDto> replaceEmailsWithPseudo(List<GiftListDto> list) {
        for (GiftListDto giftListDto : list) {
            giftListDto.setOwner(accountRepo.findByEmail(giftListDto.getOwner()).getPseudo());
        }
        return list;
    }

    private void replaceEmailsWithPseudo(GiftListDto giftListDto) {
        giftListDto.setOwner(accountRepo.findByEmail(giftListDto.getOwner()).getPseudo());
    }

    @Transactional
    public void addFavorite(Long giftListId, String email) {
        FavoriteEntity existingFavorite = favoriteRepo.findByEmailAndGiftListId(email, giftListId);
        if (existingFavorite == null) {
            FavoriteEntity favoriteEntity = new FavoriteEntity();
            favoriteEntity.setEmail(email);
            favoriteEntity.setGiftListId(giftListId);
            favoriteRepo.save(favoriteEntity);
        }
    }

    @Transactional

    public void toggleFavorite(Long giftListId, String email) {
        FavoriteEntity existingFavorite = favoriteRepo.findByEmailAndGiftListId(email, giftListId);
        if (existingFavorite != null) {
            favoriteRepo.delete(existingFavorite);
        } else {
            this.addFavorite(giftListId, email);
        }
    }

    public void notifyGiftDeletionToFavorites(Long giftListId, String giftTitle, String giftDescription, String giftUrl) {

        GiftListEntity giftListEntity = giftListRepo.findByGiftListId(giftListId);

        if (mailServiceEnabled) {
            String emailBody = emailTemplateService.generateGiftDeletionBody(giftTitle,
                    giftDescription,
                    giftUrl,
                    giftListEntity.getName(),
                    GiftListMapper.buildShareUrl(baseUrl, giftListEntity.getGiftListId()));
            String emailSubject = "Objet supprimé de la liste : " + giftListEntity.getName();

            List<FavoriteEntity> favoriteEntityList = favoriteRepo.findByGiftListId(giftListEntity.getGiftListId());

            mailService.sendEmailToRecipients(getEmailsFromFavorites(favoriteEntityList), emailBody, emailSubject);
        }
    }

    public void notifyGiftModificationToFavorites(Long giftListId,
                                                  String currentGiftTitle,
                                                  String currentGiftDescription,
                                                  String currentGiftUrl,
                                                  Integer currentPriority,
                                                  String titleUpdate,
                                                  String descriptionUpdate,
                                                  String urlUpdate,
                                                  Integer priorityUpdate) {
        if (mailServiceEnabled) {
            GiftListEntity giftListEntity = giftListRepo.findByGiftListId(giftListId);

            String emailBody = emailTemplateService.generateGiftModificationBody(currentGiftTitle,
                    currentGiftDescription,
                    currentGiftUrl,
                    titleUpdate,
                    GiftMapper.mapPriorityLabel(currentPriority),
                    descriptionUpdate,
                    urlUpdate,
                    GiftMapper.mapPriorityLabel(priorityUpdate),
                    giftListEntity.getName(),
                    GiftListMapper.buildShareUrl(baseUrl, giftListEntity.getGiftListId())
            );
            String emailSubject = "Objet modifié de la liste : " + giftListEntity.getName();

            List<FavoriteEntity> favoriteEntityList = favoriteRepo.findByGiftListId(giftListEntity.getGiftListId());

            mailService.sendEmailToRecipients(getEmailsFromFavorites(favoriteEntityList), emailBody, emailSubject);
        }
    }


    @Transactional

    public String deleteGiftList(Long giftListId, String email) throws ForbiddenModificationException, GiftListNotFoundException {
        GiftListEntity giftListEntity = giftListRepo.findByGiftListId(giftListId);
        if (giftListEntity != null) {
            if (giftListEntity.getOwner().equals(email)) {
                List<FavoriteEntity> favoriteEntityList = favoriteRepo.findByGiftListId(giftListEntity.getGiftListId());
                favoriteRepo.deleteAll(favoriteEntityList);
                giftListRepo.delete(giftListEntity);
                return "La liste " + giftListEntity.getName() + " à bien été supprimé";
            } else {
                throw new ForbiddenModificationException("You cannot delete a gift list that does not belong to you");
            }
        } else {
            throw new GiftListNotFoundException("Gift list not found");
        }
    }


    public GiftListContextDto getGiftListWithContext(Long id, String email) throws GiftListNotFoundException {
        GiftListDto giftList = this.getGiftListById(id);

        GiftListContextDto giftListContext = new GiftListContextDto(giftList);

        giftListContext.setOwnedByCurrentUser(giftList.getOwner().equals(email));

        this.replaceEmailsWithPseudo(giftListContext);

        if (email != null) {
            if (!giftListContext.isOwnedByCurrentUser()) {
                giftListContext.setFavorite(
                        this.getFavoriteGiftListsOfEmail(email)
                                .stream()
                                .anyMatch(f -> Objects.equals(f.getGiftListId(), id))
                );
            }
        } else {
            // If not logged in, anonymize the information
            giftListContext.setFavorite(false);
            giftListContext.getGifts()
                    .forEach(giftDto -> {
                        giftDto.setHolder(null);
                        giftDto.setTaken(null);
                        giftDto.setHolderPseudo(null);
                    });
        }

        return giftListContext;
    }

    private List<String> getEmailsFromFavorites(List<FavoriteEntity> favoriteEntityList) {
        return Optional.ofNullable(favoriteEntityList)
                .orElse(new ArrayList<>())
                .stream()
                .map(FavoriteEntity::getEmail)
                .toList();
    }

}
