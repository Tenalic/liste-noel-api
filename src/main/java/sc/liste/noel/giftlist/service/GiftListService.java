package sc.liste.noel.giftlist.service;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import sc.liste.noel.account.db.entity.AccountEntity;
import sc.liste.noel.account.db.repo.AccountRepo;
import sc.liste.noel.account.exception.AccountNotFoundException;
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

import java.util.*;

@Service
public class GiftListService {

    private final GiftListRepo giftListRepo;

    private final FavoriteRepo favoriteRepo;

    private final AccountRepo accountRepo;

    @Value("${base_url}")
    private String baseUrl;

    private final MailService mailService;

    @Value("${send_email_active}")
    private boolean mailServiceEnabled;

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

        int attempts = 0;
        String token;
        do {
            if (attempts++ > 5) {
                throw new IllegalStateException("Impossible de générer un token unique");
            }
            token = UUID.randomUUID().toString().replace("-", "");
        } while (giftListRepo.existsByShareToken(token));

        giftListEntity.setShareToken(token);

        giftListRepo.save(giftListEntity);
    }


    public List<GiftListDto> getGiftListsOfEmail(String email) {
        List<GiftListEntity> giftListEntityList = giftListRepo.findByOwner(email);
        return GiftListMapper.entitiesToDtosWithoutGifts(giftListEntityList);
    }


    public GiftListDto getGiftListByShareToken(String shareToken) throws GiftListNotFoundException {
        GiftListEntity giftListEntity = giftListRepo.findByShareToken(shareToken);
        GiftListDto giftListDto = GiftListMapper.entityToDto(giftListEntity);
        if (giftListDto != null) {
            giftListDto.setShareUrl(GiftListMapper.buildShareUrl(baseUrl, shareToken));
            return giftListDto;
        } else {
            throw new GiftListNotFoundException("Gift list not found: " + shareToken);
        }
    }

    // FIXME create a GiftListSimpleEntity repo without the gifts (perf)
    public List<GiftListDto> getGiftLists(boolean isPublic, String name) {
        List<GiftListEntity> giftListEntities;
        boolean isSearchByName = name != null && !name.isBlank();
        if (isSearchByName) {
            giftListEntities = giftListRepo.findByIsPublicAndNameContainingIgnoreCase(isPublic, name);
        } else {
            giftListEntities = giftListRepo.findByIsPublic(isPublic);
        }
        List<GiftListDto> giftListDtos = GiftListMapper.entitiesToDtosWithoutGifts(giftListEntities);

        giftListDtos.forEach(giftListDto -> {
                    giftListDto.setShareUrl(GiftListMapper.buildShareUrl(baseUrl, giftListDto.getShareToken()));
                    try {
                        this.replaceEmailsWithPseudo(giftListDto);
                    } catch (AccountNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        return giftListDtos;
    }

    @Transactional
    public void updatePublic(String shareToken, boolean isPublic, String email) throws ForbiddenModificationException, GiftListNotFoundException {
        GiftListEntity giftListEntity = giftListRepo.findByShareToken(shareToken);
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

    @Transactional
    public void updateName(String  shareToken, String name, String email) throws ForbiddenModificationException, GiftListNotFoundException {
        GiftListEntity giftListEntity = giftListRepo.findByShareToken(shareToken);
        if (giftListEntity == null) {
            throw new GiftListNotFoundException("Gift list not found");
        }
        if (giftListEntity.getOwner().equals(email)) {
            giftListEntity.setName(name);
            giftListRepo.save(giftListEntity);
        } else {
            throw new ForbiddenModificationException("The gift list does not belong to the user");
        }
    }


    public List<GiftListDto> getFavoriteGiftListsOfEmail(String email) throws AccountNotFoundException {
        List<FavoriteEntity> favoriteEntityList = favoriteRepo.findByEmail(email);

        if (favoriteEntityList == null) {
            return List.of();
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

    private List<GiftListDto> replaceEmailsWithPseudo(List<GiftListDto> list) throws AccountNotFoundException {
        for (GiftListDto giftListDto : list) {
            giftListDto.setOwner(accountRepo.findByEmail(giftListDto.getOwner()).map(AccountEntity::getPseudo).orElseThrow(() -> new AccountNotFoundException("Account not found")));
        }
        return list;
    }

    private void replaceEmailsWithPseudo(GiftListDto giftListDto) throws AccountNotFoundException {
        giftListDto.setOwner(accountRepo.findByEmail(giftListDto.getOwner()).map(AccountEntity::getPseudo).orElseThrow(() -> new AccountNotFoundException("Account not found")));
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
    public void toggleFavorite(String shareToken, String email) {
        Long giftListId = giftListRepo.findByShareToken(shareToken).getGiftListId();
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
                    GiftListMapper.buildShareUrl(baseUrl, giftListEntity.getShareToken()));
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
                    GiftListMapper.buildShareUrl(baseUrl, giftListEntity.getShareToken())
            );
            String emailSubject = "Objet modifié de la liste : " + giftListEntity.getName();

            List<FavoriteEntity> favoriteEntityList = favoriteRepo.findByGiftListId(giftListEntity.getGiftListId());

            mailService.sendEmailToRecipients(getEmailsFromFavorites(favoriteEntityList), emailBody, emailSubject);
        }
    }


    @Transactional

    public String deleteGiftList(String shareToken, String email) throws ForbiddenModificationException, GiftListNotFoundException {
        GiftListEntity giftListEntity = giftListRepo.findByShareToken(shareToken);
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


    public GiftListContextDto getGiftListWithContext(String shareToken, String email) throws GiftListNotFoundException, AccountNotFoundException {
        GiftListDto giftList = this.getGiftListByShareToken(shareToken);

        GiftListContextDto giftListContext = new GiftListContextDto(giftList);

        giftListContext.setOwnedByCurrentUser(giftList.getOwner().equals(email));

        this.replaceEmailsWithPseudo(giftListContext);

        if (email != null) {
            if (!giftListContext.isOwnedByCurrentUser()) {
                giftListContext.setFavorite(
                        this.getFavoriteGiftListsOfEmail(email)
                                .stream()
                                .anyMatch(f -> Objects.equals(f.getShareToken(), shareToken))
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
