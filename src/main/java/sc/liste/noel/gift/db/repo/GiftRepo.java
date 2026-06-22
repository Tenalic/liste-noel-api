package sc.liste.noel.gift.db.repo;

import org.springframework.data.repository.CrudRepository;
import sc.liste.noel.gift.db.entity.GiftEntity;

public interface GiftRepo extends CrudRepository<GiftEntity, Long> {

    GiftEntity findByGiftId(Long giftId);

}
