package sc.liste.noel.giftlist.db.repo;

import org.springframework.data.repository.CrudRepository;
import sc.liste.noel.giftlist.db.entity.GiftListEntity;

import java.util.List;

public interface GiftListRepo extends CrudRepository<GiftListEntity, Long> {

	List<GiftListEntity> findByOwner(String email);

	List<GiftListEntity> findByIsPublic(boolean isPublic);

	List<GiftListEntity> findByIsPublicAndNameContainingIgnoreCase(boolean isPublic, String name);

	GiftListEntity findByGiftListId(Long giftListId);


}
