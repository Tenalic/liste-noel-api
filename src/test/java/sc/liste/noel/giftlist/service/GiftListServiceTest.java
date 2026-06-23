package sc.liste.noel.giftlist.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sc.liste.noel.account.db.entity.AccountEntity;
import sc.liste.noel.account.db.repo.AccountRepo;
import sc.liste.noel.account.exception.AccountNotFoundException;
import sc.liste.noel.common.exception.ForbiddenModificationException;
import sc.liste.noel.common.exception.GiftListNotFoundException;
import sc.liste.noel.common.service.EmailTemplateService;
import sc.liste.noel.common.service.MailService;
import sc.liste.noel.giftlist.db.entity.FavoriteEntity;
import sc.liste.noel.giftlist.db.entity.GiftListEntity;
import sc.liste.noel.giftlist.db.repo.FavoriteRepo;
import sc.liste.noel.giftlist.db.repo.GiftListRepo;
import sc.liste.noel.giftlist.dto.GiftListContextDto;
import sc.liste.noel.giftlist.dto.GiftListDto;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiftListServiceTest {

    @Mock
    private GiftListRepo giftListRepo;

    @Mock
    private FavoriteRepo favoriteRepo;

    @Mock
    private AccountRepo accountRepo;

    @Mock
    private MailService mailService;

    @Mock
    private EmailTemplateService emailTemplateService;

    @InjectMocks
    private GiftListService giftListService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(giftListService, "baseUrl", "https://test.example.com");
        ReflectionTestUtils.setField(giftListService, "mailServiceEnabled", true);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private GiftListEntity buildGiftListEntity(Long id, String owner, String name, boolean isPublic) {
        GiftListEntity entity = new GiftListEntity();
        entity.setGiftListId(id);
        entity.setOwner(owner);
        entity.setName(name);
        entity.setIsPublic(isPublic);
        return entity;
    }

    private FavoriteEntity buildFavorite(String email, Long giftListId) {
        FavoriteEntity entity = new FavoriteEntity();
        entity.setEmail(email);
        entity.setGiftListId(giftListId);
        return entity;
    }

    private AccountEntity buildAccount(String email, String pseudo) {
        AccountEntity entity = new AccountEntity();
        entity.setEmail(email);
        entity.setPseudo(pseudo);
        return entity;
    }

    // =========================================================================
    // createGiftList
    // =========================================================================

    @Nested
    @DisplayName("createGiftList()")
    class CreateGiftList {

        @Test
        @DisplayName("sauvegarde un GiftListEntity avec les bons champs")
        void savesEntity_withCorrectFields() {
            giftListService.createGiftList("owner@test.com", "Ma liste de Noël", true);

            ArgumentCaptor<GiftListEntity> captor = ArgumentCaptor.forClass(GiftListEntity.class);
            verify(giftListRepo).save(captor.capture());

            GiftListEntity saved = captor.getValue();
            assertThat(saved.getOwner()).isEqualTo("owner@test.com");
            assertThat(saved.getName()).isEqualTo("Ma liste de Noël");
            assertThat(saved.getIsPublic()).isTrue();
        }

        @Test
        @DisplayName("sauvegarde une liste privée")
        void savesPrivateList() {
            giftListService.createGiftList("owner@test.com", "Privée", false);

            ArgumentCaptor<GiftListEntity> captor = ArgumentCaptor.forClass(GiftListEntity.class);
            verify(giftListRepo).save(captor.capture());
            assertThat(captor.getValue().getIsPublic()).isFalse();
        }
    }

    // =========================================================================
    // getGiftListsOfEmail
    // =========================================================================

    @Nested
    @DisplayName("getGiftListsOfEmail()")
    class GetGiftListsOfEmail {

        @Test
        @DisplayName("retourne la liste des DTOs pour un email donné")
        void returnsDtos_forGivenEmail() {
            List<GiftListEntity> entities = List.of(
                    buildGiftListEntity(1L, "owner@test.com", "Liste 1", true),
                    buildGiftListEntity(2L, "owner@test.com", "Liste 2", false)
            );
            when(giftListRepo.findByOwner("owner@test.com")).thenReturn(entities);

            List<GiftListDto> result = giftListService.getGiftListsOfEmail("owner@test.com");

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("retourne une liste vide si aucune liste n'existe")
        void returnsEmptyList_whenNoGiftLists() {
            when(giftListRepo.findByOwner("owner@test.com")).thenReturn(Collections.emptyList());

            List<GiftListDto> result = giftListService.getGiftListsOfEmail("owner@test.com");

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // getGiftListById
    // =========================================================================

    @Nested
    @DisplayName("getGiftListById()")
    class GetGiftListById {

        @Test
        @DisplayName("retourne le DTO avec une shareUrl construite")
        void returnsDto_withShareUrl() throws GiftListNotFoundException {
            when(giftListRepo.findByGiftListId(1L))
                    .thenReturn(buildGiftListEntity(1L, "owner@test.com", "Ma liste", true));

            GiftListDto result = giftListService.getGiftListById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getShareUrl()).contains("1");
        }

        @Test
        @DisplayName("lève GiftListNotFoundException si la liste est introuvable")
        void throwsGiftListNotFoundException_whenNotFound() {
            when(giftListRepo.findByGiftListId(99L)).thenReturn(null);

            assertThatThrownBy(() -> giftListService.getGiftListById(99L))
                    .isInstanceOf(GiftListNotFoundException.class);
        }
    }

    // =========================================================================
    // getGiftLists
    // =========================================================================

    @Nested
    @DisplayName("getGiftLists()")
    class GetGiftLists {

        @Test
        @DisplayName("recherche par nom quand un nom non vide est fourni")
        void searchesByName_whenNameProvided() throws GiftListNotFoundException {
            GiftListEntity entity = buildGiftListEntity(1L, "owner@test.com", "Noël", true);
            when(giftListRepo.findByIsPublicAndNameContainingIgnoreCase(true, "noël"))
                    .thenReturn(List.of(entity));
            when(accountRepo.findByEmail("owner@test.com"))
                    .thenReturn(Optional.of(buildAccount("owner@test.com", "Pseudo")));

            List<GiftListDto> result = giftListService.getGiftLists(true, "noël");

            assertThat(result).hasSize(1);
            verify(giftListRepo).findByIsPublicAndNameContainingIgnoreCase(true, "noël");
            verify(giftListRepo, never()).findByIsPublic(anyBoolean());
        }

        @Test
        @DisplayName("recherche sans filtre de nom quand name est null")
        void searchesWithoutName_whenNameIsNull() throws GiftListNotFoundException {
            GiftListEntity entity = buildGiftListEntity(1L, "owner@test.com", "Liste", true);
            when(giftListRepo.findByIsPublic(true)).thenReturn(List.of(entity));
            when(accountRepo.findByEmail("owner@test.com"))
                    .thenReturn(Optional.of(buildAccount("owner@test.com", "Pseudo")));

            giftListService.getGiftLists(true, null);

            verify(giftListRepo).findByIsPublic(true);
            verify(giftListRepo, never()).findByIsPublicAndNameContainingIgnoreCase(anyBoolean(), anyString());
        }

        @Test
        @DisplayName("recherche sans filtre de nom quand name est blanc")
        void searchesWithoutName_whenNameIsBlank() throws GiftListNotFoundException {
            GiftListEntity entity = buildGiftListEntity(1L, "owner@test.com", "Liste", true);
            when(giftListRepo.findByIsPublic(true)).thenReturn(List.of(entity));
            when(accountRepo.findByEmail("owner@test.com"))
                    .thenReturn(Optional.of(buildAccount("owner@test.com", "Pseudo")));

            giftListService.getGiftLists(true, "   ");

            verify(giftListRepo).findByIsPublic(true);
        }

        @Test
        @DisplayName("remplace les emails par les pseudos des propriétaires")
        void replacesEmailsWithPseudos() throws GiftListNotFoundException {
            GiftListEntity entity = buildGiftListEntity(1L, "owner@test.com", "Liste", true);
            when(giftListRepo.findByIsPublic(true)).thenReturn(List.of(entity));
            when(accountRepo.findByEmail("owner@test.com"))
                    .thenReturn(Optional.of(buildAccount("owner@test.com", "MonPseudo")));

            List<GiftListDto> result = giftListService.getGiftLists(true, null);

            assertThat(result.get(0).getOwner()).isEqualTo("MonPseudo");
        }

        @Test
        @DisplayName("retourne une liste vide si le repo ne retourne aucun résultat")
        void returnsEmptyList_whenRepoReturnsEmpty() throws GiftListNotFoundException {
            when(giftListRepo.findByIsPublic(true)).thenReturn(Collections.emptyList());

            List<GiftListDto> result = giftListService.getGiftLists(true, null);

            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("lève RuntimeException si un propriétaire est introuvable en base")
        void throwsRuntimeException_whenOwnerAccountNotFound() {
            GiftListEntity entity = buildGiftListEntity(1L, "ghost@test.com", "Liste", true);
            when(giftListRepo.findByIsPublic(true)).thenReturn(List.of(entity));
            when(accountRepo.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> giftListService.getGiftLists(true, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(AccountNotFoundException.class);
        }
    }

    // =========================================================================
    // updatePublic
    // =========================================================================

    @Nested
    @DisplayName("updatePublic()")
    class UpdatePublic {

        @Test
        @DisplayName("met à jour la visibilité quand le propriétaire est correct")
        void updatesVisibility_whenOwnerMatches() throws Exception {
            GiftListEntity entity = buildGiftListEntity(1L, "owner@test.com", "Liste", false);
            when(giftListRepo.findByGiftListId(1L)).thenReturn(entity);

            giftListService.updatePublic(1L, true, "owner@test.com");

            assertThat(entity.getIsPublic()).isTrue();
            verify(giftListRepo).save(entity);
        }

        @Test
        @DisplayName("lève GiftListNotFoundException si la liste est introuvable")
        void throwsGiftListNotFoundException_whenListNotFound() {
            when(giftListRepo.findByGiftListId(99L)).thenReturn(null);

            assertThatThrownBy(() -> giftListService.updatePublic(99L, true, "owner@test.com"))
                    .isInstanceOf(GiftListNotFoundException.class);
            verify(giftListRepo, never()).save(any());
        }

        @Test
        @DisplayName("lève ForbiddenModificationException si l'email ne correspond pas au propriétaire")
        void throwsForbiddenModificationException_whenNotOwner() {
            GiftListEntity entity = buildGiftListEntity(1L, "owner@test.com", "Liste", false);
            when(giftListRepo.findByGiftListId(1L)).thenReturn(entity);

            assertThatThrownBy(() -> giftListService.updatePublic(1L, true, "other@test.com"))
                    .isInstanceOf(ForbiddenModificationException.class);
            verify(giftListRepo, never()).save(any());
        }
    }

    // =========================================================================
    // getFavoriteGiftListsOfEmail
    // =========================================================================

    @Nested
    @DisplayName("getFavoriteGiftListsOfEmail()")
    class GetFavoriteGiftListsOfEmail {

        @Test
        @DisplayName("retourne les listes favorites avec les pseudos")
        void returnsFavoriteLists_withPseudos() throws AccountNotFoundException {
            List<FavoriteEntity> favorites = List.of(buildFavorite("user@test.com", 1L));
            GiftListEntity giftList = buildGiftListEntity(1L, "owner@test.com", "Liste", true);
            when(favoriteRepo.findByEmail("user@test.com")).thenReturn(favorites);
            when(giftListRepo.findByGiftListId(1L)).thenReturn(giftList);
            when(accountRepo.findByEmail("owner@test.com"))
                    .thenReturn(Optional.of(buildAccount("owner@test.com", "OwnerPseudo")));

            List<GiftListDto> result = giftListService.getFavoriteGiftListsOfEmail("user@test.com");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOwner()).isEqualTo("OwnerPseudo");
        }

        @Test
        @DisplayName("retourne null si favoriteRepo retourne null")
        void returnsNull_whenFavoriteRepoReturnsNull() throws AccountNotFoundException {
            when(favoriteRepo.findByEmail("user@test.com")).thenReturn(null);

            List<GiftListDto> result = giftListService.getFavoriteGiftListsOfEmail("user@test.com");

            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ignore les listes introuvables en base")
        void ignoresMissingGiftLists() throws AccountNotFoundException {
            List<FavoriteEntity> favorites = List.of(
                    buildFavorite("user@test.com", 1L),
                    buildFavorite("user@test.com", 2L)
            );
            when(favoriteRepo.findByEmail("user@test.com")).thenReturn(favorites);
            when(giftListRepo.findByGiftListId(1L))
                    .thenReturn(buildGiftListEntity(1L, "owner@test.com", "Existante", true));
            when(giftListRepo.findByGiftListId(2L)).thenReturn(null);
            when(accountRepo.findByEmail("owner@test.com"))
                    .thenReturn(Optional.of(buildAccount("owner@test.com", "Pseudo")));

            List<GiftListDto> result = giftListService.getFavoriteGiftListsOfEmail("user@test.com");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("retourne une liste vide si aucun favori")
        void returnsEmptyList_whenNoFavorites() throws AccountNotFoundException {
            when(favoriteRepo.findByEmail("user@test.com")).thenReturn(Collections.emptyList());

            List<GiftListDto> result = giftListService.getFavoriteGiftListsOfEmail("user@test.com");

            assertThat(result).isEmpty();
        }
    }

    // =========================================================================
    // addFavorite
    // =========================================================================

    @Nested
    @DisplayName("addFavorite()")
    class AddFavorite {

        @Test
        @DisplayName("sauvegarde un favori s'il n'existe pas encore")
        void savesFavorite_whenNotAlreadyPresent() {
            when(favoriteRepo.findByEmailAndGiftListId("user@test.com", 1L)).thenReturn(null);

            giftListService.addFavorite(1L, "user@test.com");

            ArgumentCaptor<FavoriteEntity> captor = ArgumentCaptor.forClass(FavoriteEntity.class);
            verify(favoriteRepo).save(captor.capture());
            assertThat(captor.getValue().getEmail()).isEqualTo("user@test.com");
            assertThat(captor.getValue().getGiftListId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("ne sauvegarde pas si le favori existe déjà")
        void doesNotSave_whenFavoriteAlreadyExists() {
            when(favoriteRepo.findByEmailAndGiftListId("user@test.com", 1L))
                    .thenReturn(buildFavorite("user@test.com", 1L));

            giftListService.addFavorite(1L, "user@test.com");

            verify(favoriteRepo, never()).save(any());
        }
    }

    // =========================================================================
    // toggleFavorite
    // =========================================================================

    @Nested
    @DisplayName("toggleFavorite()")
    class ToggleFavorite {

        @Test
        @DisplayName("supprime le favori s'il existe")
        void deletesFavorite_whenExists() {
            FavoriteEntity favorite = buildFavorite("user@test.com", 1L);
            when(favoriteRepo.findByEmailAndGiftListId("user@test.com", 1L)).thenReturn(favorite);

            giftListService.toggleFavorite(1L, "user@test.com");

            verify(favoriteRepo).delete(favorite);
            verify(favoriteRepo, never()).save(any());
        }

        @Test
        @DisplayName("ajoute le favori s'il n'existe pas")
        void addsFavorite_whenNotExists() {
            when(favoriteRepo.findByEmailAndGiftListId("user@test.com", 1L)).thenReturn(null);

            giftListService.toggleFavorite(1L, "user@test.com");

            verify(favoriteRepo, never()).delete(any());
            verify(favoriteRepo).save(any(FavoriteEntity.class));
        }
    }

    // =========================================================================
    // notifyGiftDeletionToFavorites
    // =========================================================================

    @Nested
    @DisplayName("notifyGiftDeletionToFavorites()")
    class NotifyGiftDeletionToFavorites {

        @Test
        @DisplayName("envoie un email aux favoris quand le service mail est actif")
        void sendsEmail_whenMailEnabled() {
            GiftListEntity giftList = buildGiftListEntity(1L, "owner@test.com", "Ma liste", true);
            List<FavoriteEntity> favorites = List.of(
                    buildFavorite("fav1@test.com", 1L),
                    buildFavorite("fav2@test.com", 1L)
            );
            when(giftListRepo.findByGiftListId(1L)).thenReturn(giftList);
            when(favoriteRepo.findByGiftListId(1L)).thenReturn(favorites);
            when(emailTemplateService.generateGiftDeletionBody(any(), any(), any(), any(), any()))
                    .thenReturn("<html>body</html>");

            giftListService.notifyGiftDeletionToFavorites(1L, "PS5", "Console Sony", "https://sony.com");

            ArgumentCaptor<List> emailCaptor = ArgumentCaptor.forClass(List.class);
            verify(mailService).sendEmailToRecipients(emailCaptor.capture(), any(), any());
            assertThat(emailCaptor.getValue()).containsExactlyInAnyOrder("fav1@test.com", "fav2@test.com");
        }

        @Test
        @DisplayName("n'envoie pas d'email si le service mail est désactivé")
        void doesNotSendEmail_whenMailDisabled() {
            ReflectionTestUtils.setField(giftListService, "mailServiceEnabled", false);

            giftListService.notifyGiftDeletionToFavorites(1L, "PS5", "Console Sony", "https://sony.com");

            verifyNoInteractions(mailService, emailTemplateService);
        }

        @Test
        @DisplayName("envoie un email vide si aucun favori")
        void sendsToEmptyList_whenNoFavorites() {
            GiftListEntity giftList = buildGiftListEntity(1L, "owner@test.com", "Ma liste", true);
            when(giftListRepo.findByGiftListId(1L)).thenReturn(giftList);
            when(favoriteRepo.findByGiftListId(1L)).thenReturn(Collections.emptyList());
            when(emailTemplateService.generateGiftDeletionBody(any(), any(), any(), any(), any()))
                    .thenReturn("body");

            giftListService.notifyGiftDeletionToFavorites(1L, "PS5", "Console", "https://url.com");

            ArgumentCaptor<List> emailCaptor = ArgumentCaptor.forClass(List.class);
            verify(mailService).sendEmailToRecipients(emailCaptor.capture(), any(), any());
            assertThat(emailCaptor.getValue()).isEmpty();
        }
    }

    // =========================================================================
    // notifyGiftModificationToFavorites
    // =========================================================================

    @Nested
    @DisplayName("notifyGiftModificationToFavorites()")
    class NotifyGiftModificationToFavorites {

        @Test
        @DisplayName("envoie un email aux favoris quand le service mail est actif")
        void sendsEmail_whenMailEnabled() {
            GiftListEntity giftList = buildGiftListEntity(1L, "owner@test.com", "Ma liste", true);
            List<FavoriteEntity> favorites = List.of(buildFavorite("fav@test.com", 1L));
            when(giftListRepo.findByGiftListId(1L)).thenReturn(giftList);
            when(favoriteRepo.findByGiftListId(1L)).thenReturn(favorites);
            when(emailTemplateService.generateGiftModificationBody(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn("body");

            giftListService.notifyGiftModificationToFavorites(
                    1L, "Ancien titre", "Ancienne desc", "https://old.com", 1,
                    "Nouveau titre", "Nouvelle desc", "https://new.com", 2);

            verify(mailService).sendEmailToRecipients(anyList(), any(), contains("Ma liste"));
        }

        @Test
        @DisplayName("n'envoie pas d'email si le service mail est désactivé")
        void doesNotSendEmail_whenMailDisabled() {
            ReflectionTestUtils.setField(giftListService, "mailServiceEnabled", false);

            giftListService.notifyGiftModificationToFavorites(
                    1L, "Titre", "Desc", "https://url.com", 1,
                    "Titre modifié", "Desc modifiée", "https://new.com", 2);

            verifyNoInteractions(mailService, emailTemplateService, giftListRepo);
        }
    }

    // =========================================================================
    // deleteGiftList
    // =========================================================================

    @Nested
    @DisplayName("deleteGiftList()")
    class DeleteGiftList {

        @Test
        @DisplayName("supprime la liste et ses favoris, retourne un message de confirmation")
        void deletesListAndFavorites_andReturnsConfirmation() throws Exception {
            GiftListEntity giftList = buildGiftListEntity(1L, "owner@test.com", "Ma liste", true);
            List<FavoriteEntity> favorites = List.of(buildFavorite("fav@test.com", 1L));
            when(giftListRepo.findByGiftListId(1L)).thenReturn(giftList);
            when(favoriteRepo.findByGiftListId(1L)).thenReturn(favorites);

            String result = giftListService.deleteGiftList(1L, "owner@test.com");

            verify(favoriteRepo).deleteAll(favorites);
            verify(giftListRepo).delete(giftList);
            assertThat(result).contains("Ma liste");
        }

        @Test
        @DisplayName("lève ForbiddenModificationException si l'utilisateur n'est pas propriétaire")
        void throwsForbiddenModificationException_whenNotOwner() {
            GiftListEntity giftList = buildGiftListEntity(1L, "owner@test.com", "Ma liste", true);
            when(giftListRepo.findByGiftListId(1L)).thenReturn(giftList);

            assertThatThrownBy(() -> giftListService.deleteGiftList(1L, "other@test.com"))
                    .isInstanceOf(ForbiddenModificationException.class);
            verify(giftListRepo, never()).delete(any());
            verify(favoriteRepo, never()).deleteAll(any());
        }

        @Test
        @DisplayName("lève GiftListNotFoundException si la liste est introuvable")
        void throwsGiftListNotFoundException_whenListNotFound() {
            when(giftListRepo.findByGiftListId(99L)).thenReturn(null);

            assertThatThrownBy(() -> giftListService.deleteGiftList(99L, "owner@test.com"))
                    .isInstanceOf(GiftListNotFoundException.class);
        }
    }

    // =========================================================================
    // getGiftListWithContext
    // =========================================================================

    @Nested
    @DisplayName("getGiftListWithContext()")
    class GetGiftListWithContext {

        @Test
        @DisplayName("marque la liste comme appartenant à l'utilisateur courant")
        void setsOwnedByCurrentUser_whenOwnerMatches() throws Exception {
            GiftListEntity entity = buildGiftListEntity(1L, "owner@test.com", "Ma liste", true);
            when(giftListRepo.findByGiftListId(1L)).thenReturn(entity);
            when(accountRepo.findByEmail("owner@test.com"))
                    .thenReturn(Optional.of(buildAccount("owner@test.com", "OwnerPseudo")));

            GiftListContextDto result = giftListService.getGiftListWithContext(1L, "owner@test.com");

            assertThat(result.isOwnedByCurrentUser()).isTrue();
        }

        @Test
        @DisplayName("anonymise les cadeaux si l'utilisateur n'est pas connecté (email null)")
        void anonymizesGifts_whenEmailIsNull() throws Exception {
            GiftListEntity entity = buildGiftListEntity(1L, "owner@test.com", "Ma liste", true);
            when(giftListRepo.findByGiftListId(1L)).thenReturn(entity);
            when(accountRepo.findByEmail("owner@test.com"))
                    .thenReturn(Optional.of(buildAccount("owner@test.com", "OwnerPseudo")));

            GiftListContextDto result = giftListService.getGiftListWithContext(1L, null);

            assertThat(result.isFavorite()).isFalse();
            result.getGifts().forEach(gift -> {
                assertThat(gift.getHolder()).isNull();
                assertThat(gift.getTaken()).isNull();
                assertThat(gift.getHolderPseudo()).isNull();
            });
        }

        @Test
        @DisplayName("lève GiftListNotFoundException si la liste est introuvable")
        void throwsGiftListNotFoundException_whenListNotFound() {
            when(giftListRepo.findByGiftListId(99L)).thenReturn(null);

            assertThatThrownBy(() -> giftListService.getGiftListWithContext(99L, "user@test.com"))
                    .isInstanceOf(GiftListNotFoundException.class);
        }

        @Test
        @DisplayName("définit isFavorite selon les favoris de l'utilisateur non-propriétaire")
        void setsFavorite_basedOnUserFavorites_whenNotOwner() throws Exception {
            GiftListEntity entity = buildGiftListEntity(1L, "owner@test.com", "Ma liste", true);
            when(giftListRepo.findByGiftListId(1L)).thenReturn(entity);
            when(accountRepo.findByEmail("owner@test.com"))
                    .thenReturn(Optional.of(buildAccount("owner@test.com", "OwnerPseudo")));

            // L'utilisateur a cette liste en favori
            GiftListEntity favEntity = buildGiftListEntity(1L, "owner@test.com", "Ma liste", true);
            when(favoriteRepo.findByEmail("other@test.com"))
                    .thenReturn(List.of(buildFavorite("other@test.com", 1L)));
            when(giftListRepo.findByGiftListId(1L)).thenReturn(entity);

            GiftListContextDto result = giftListService.getGiftListWithContext(1L, "other@test.com");

            assertThat(result.isFavorite()).isTrue();
        }
    }
}