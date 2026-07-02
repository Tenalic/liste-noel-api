package sc.liste.noel.gift.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sc.liste.noel.common.exception.ForbiddenModificationException;
import sc.liste.noel.common.exception.GiftListNotFoundException;
import sc.liste.noel.common.service.MailService;
import sc.liste.noel.gift.db.entity.GiftEntity;
import sc.liste.noel.gift.db.repo.GiftRepo;
import sc.liste.noel.giftlist.dto.GiftListDto;
import sc.liste.noel.giftlist.service.GiftListService;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GiftServiceTest {

    @Mock
    private GiftRepo giftRepo;

    @Mock
    private MailService mailService;

    @Mock
    private GiftListService giftListService;

    @InjectMocks
    private GiftService giftService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private GiftListDto buildGiftListDto(Long id) {
        GiftListDto giftListDto = new GiftListDto();
        giftListDto.setGiftListId(id);
        return giftListDto;
    }

    private GiftEntity buildGift(Long giftId, Long giftListId, String title, String description, String url, int priority) {
        GiftEntity entity = new GiftEntity();
        entity.setGiftId(giftId);
        entity.setGiftListId(giftListId);
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setUrl(url);
        entity.setPriorityValue(priority);
        entity.setTaken(false);
        return entity;
    }

    // =========================================================================
    // addGiftToGiftList
    // =========================================================================

    @Nested
    @DisplayName("addGiftToGiftList()")
    class AddGiftToGiftList {

        @Test
        @DisplayName("sauvegarde un GiftEntity avec les bons champs")
        void savesGiftEntity_withCorrectFields() throws GiftListNotFoundException {
            GiftListDto giftListDto = buildGiftListDto(42L);
            when(giftListService.getGiftListByShareToken(anyString())).thenReturn(giftListDto);
            giftService.addGiftToGiftList("Nintendo Switch", "https://shop.com", "Console de jeu", "42", 2);

            ArgumentCaptor<GiftEntity> captor = ArgumentCaptor.forClass(GiftEntity.class);
            verify(giftRepo).save(captor.capture());

            GiftEntity saved = captor.getValue();
            assertThat(saved.getTitle()).isEqualTo("Nintendo Switch");
            assertThat(saved.getUrl()).isEqualTo("https://shop.com");
            assertThat(saved.getDescription()).isEqualTo("Console de jeu");
            assertThat(saved.getGiftListId()).isEqualTo(42L);
            assertThat(saved.getPriorityValue()).isEqualTo(2);
            assertThat(saved.getTaken()).isFalse();
        }

        @Test
        @DisplayName("le champ taken est toujours false à la création")
        void takenIsAlwaysFalse_onCreation() throws GiftListNotFoundException {
            GiftListDto giftListDto = buildGiftListDto(1L);
            when(giftListService.getGiftListByShareToken(anyString())).thenReturn(giftListDto);
            giftService.addGiftToGiftList("Livre", null, null, "1", 1);

            ArgumentCaptor<GiftEntity> captor = ArgumentCaptor.forClass(GiftEntity.class);
            verify(giftRepo).save(captor.capture());
            assertThat(captor.getValue().getTaken()).isFalse();
        }
    }

    // =========================================================================
    // getGiftEntity
    // =========================================================================

    @Nested
    @DisplayName("getGiftEntity()")
    class GetGiftEntity {

        @Test
        @DisplayName("retourne l'entité si elle existe")
        void returnsEntity_whenFound() {
            GiftEntity gift = buildGift(1L, 10L, "Livre", "Un bon livre", "https://url.com", 1);
            when(giftRepo.findByGiftId(1L)).thenReturn(Optional.of(gift));

            GiftEntity result = giftService.getGiftEntity(1L);

            assertThat(result).isEqualTo(gift);
        }

        @Test
        @DisplayName("lève NoSuchElementException si le cadeau est introuvable")
        void throwsNoSuchElementException_whenNotFound() {
            when(giftRepo.findByGiftId(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> giftService.getGiftEntity(99L))
                    .isInstanceOf(NoSuchElementException.class);
        }
    }

    // =========================================================================
    // deleteGift
    // =========================================================================

    @Nested
    @DisplayName("deleteGift()")
    class DeleteGift {

        @Test
        @DisplayName("supprime le cadeau et notifie les favoris")
        void deletesGift_andNotifiesFavorites_whenOwnerValid() throws ForbiddenModificationException {
            GiftEntity gift = buildGift(1L, 10L, "Livre", "Description", "https://url.com", 1);
            when(giftRepo.existsByGiftIdAndListOwner(1L, "owner@test.com")).thenReturn(true);
            when(giftRepo.findByGiftId(1L)).thenReturn(Optional.of(gift));

            giftService.deleteGift(1L, "owner@test.com");

            verify(giftRepo).delete(gift);
            verify(giftListService).notifyGiftDeletionToFavorites(
                    gift.getGiftListId(),
                    gift.getTitle(),
                    gift.getDescription(),
                    gift.getUrl()
            );
        }

        @Test
        @DisplayName("lève ForbiddenModificationException si l'utilisateur n'est pas propriétaire")
        void throwsForbiddenModificationException_whenNotOwner() {
            when(giftRepo.existsByGiftIdAndListOwner(1L, "other@test.com")).thenReturn(false);

            assertThatThrownBy(() -> giftService.deleteGift(1L, "other@test.com"))
                    .isInstanceOf(ForbiddenModificationException.class);

            verify(giftRepo, never()).delete(any());
            verifyNoInteractions(giftListService);
        }

        @Test
        @DisplayName("lève NoSuchElementException si le cadeau est introuvable après vérification du propriétaire")
        void throwsNoSuchElementException_whenGiftNotFound() {
            when(giftRepo.existsByGiftIdAndListOwner(1L, "owner@test.com")).thenReturn(true);
            when(giftRepo.findByGiftId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> giftService.deleteGift(1L, "owner@test.com"))
                    .isInstanceOf(NoSuchElementException.class);

            verify(giftRepo, never()).delete(any());
        }

        @Test
        @DisplayName("la notification contient bien les données du cadeau supprimé")
        void notificationContainsCorrectGiftData() throws ForbiddenModificationException {
            GiftEntity gift = buildGift(1L, 10L, "PS5", "Console Sony", "https://sony.com", 3);
            when(giftRepo.existsByGiftIdAndListOwner(1L, "owner@test.com")).thenReturn(true);
            when(giftRepo.findByGiftId(1L)).thenReturn(Optional.of(gift));

            giftService.deleteGift(1L, "owner@test.com");

            verify(giftListService).notifyGiftDeletionToFavorites(10L, "PS5", "Console Sony", "https://sony.com");
        }
    }

    // =========================================================================
    // updateGift
    // =========================================================================

    @Nested
    @DisplayName("updateGift()")
    class UpdateGift {

        @Test
        @DisplayName("met à jour tous les champs et sauvegarde")
        void updatesAllFields_andSaves_whenOwnerValid() throws ForbiddenModificationException {
            GiftEntity gift = buildGift(1L, 10L, "Ancien titre", "Ancienne desc", "https://old.com", 1);
            when(giftRepo.existsByGiftIdAndListOwner(1L, "owner@test.com")).thenReturn(true);
            when(giftRepo.findByGiftId(1L)).thenReturn(Optional.of(gift));

            giftService.updateGift(1L, "Nouveau titre", "Nouvelle desc", "https://new.com", 3, "owner@test.com");

            assertThat(gift.getTitle()).isEqualTo("Nouveau titre");
            assertThat(gift.getDescription()).isEqualTo("Nouvelle desc");
            assertThat(gift.getUrl()).isEqualTo("https://new.com");
            assertThat(gift.getPriorityValue()).isEqualTo(3);
            verify(giftRepo).save(gift);
        }

        @Test
        @DisplayName("notifie les favoris avec les anciennes et nouvelles valeurs avant la mise à jour")
        void notifiesFavorites_withOldAndNewValues_beforeUpdate() throws ForbiddenModificationException {
            GiftEntity gift = buildGift(1L, 10L, "Ancien titre", "Ancienne desc", "https://old.com", 1);
            when(giftRepo.existsByGiftIdAndListOwner(1L, "owner@test.com")).thenReturn(true);
            when(giftRepo.findByGiftId(1L)).thenReturn(Optional.of(gift));

            giftService.updateGift(1L, "Nouveau titre", "Nouvelle desc", "https://new.com", 3, "owner@test.com");

            verify(giftListService).notifyGiftModificationToFavorites(
                    1L,
                    "Ancien titre", "Ancienne desc", "https://old.com", 1,
                    "Nouveau titre", "Nouvelle desc", "https://new.com", 3
            );
        }

        @Test
        @DisplayName("la notification est appelée avant la mise à jour des champs")
        void notificationCalledBeforeFieldsUpdated() throws ForbiddenModificationException {
            GiftEntity gift = buildGift(1L, 10L, "Ancien titre", "Ancienne desc", "https://old.com", 1);
            when(giftRepo.existsByGiftIdAndListOwner(1L, "owner@test.com")).thenReturn(true);
            when(giftRepo.findByGiftId(1L)).thenReturn(Optional.of(gift));

            // On vérifie que lors de la notification, les anciens titres sont encore présents
            doAnswer(invocation -> {
                assertThat(gift.getTitle()).isEqualTo("Ancien titre");
                return null;
            }).when(giftListService).notifyGiftModificationToFavorites(
                    anyLong(), anyString(), anyString(), anyString(), anyInt(),
                    anyString(), anyString(), anyString(), anyInt()
            );

            giftService.updateGift(1L, "Nouveau titre", "Nouvelle desc", "https://new.com", 3, "owner@test.com");
        }

        @Test
        @DisplayName("lève ForbiddenModificationException si l'utilisateur n'est pas propriétaire")
        void throwsForbiddenModificationException_whenNotOwner() {
            when(giftRepo.existsByGiftIdAndListOwner(1L, "other@test.com")).thenReturn(false);

            assertThatThrownBy(() -> giftService.updateGift(1L, "titre", "desc", "url", 1, "other@test.com"))
                    .isInstanceOf(ForbiddenModificationException.class);

            verify(giftRepo, never()).save(any());
            verifyNoInteractions(giftListService);
        }

        @Test
        @DisplayName("lève NoSuchElementException si le cadeau est introuvable après vérification du propriétaire")
        void throwsNoSuchElementException_whenGiftNotFound() {
            when(giftRepo.existsByGiftIdAndListOwner(1L, "owner@test.com")).thenReturn(true);
            when(giftRepo.findByGiftId(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> giftService.updateGift(1L, "titre", "desc", "url", 1, "owner@test.com"))
                    .isInstanceOf(NoSuchElementException.class);

            verify(giftRepo, never()).save(any());
        }
    }
}