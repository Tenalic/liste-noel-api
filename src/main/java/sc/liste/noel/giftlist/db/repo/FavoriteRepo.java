package sc.liste.noel.giftlist.db.repo;

import org.springframework.data.repository.CrudRepository;
import sc.liste.noel.giftlist.db.entity.FavoriteEntity;

import java.util.List;

public interface FavoriteRepo extends CrudRepository<FavoriteEntity, Long> {

    List<FavoriteEntity> findByEmail(String email);
    FavoriteEntity findByEmailAndGiftListId(String email, Long giftListId);

    List<FavoriteEntity> findByGiftListId(Long giftListId);

}
