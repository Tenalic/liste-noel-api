package sc.liste.noel.gift.db.repo;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import sc.liste.noel.gift.db.entity.GiftEntity;

import java.util.Optional;

public interface GiftRepo extends CrudRepository<GiftEntity, Long> {

    Optional<GiftEntity> findByGiftId(Long giftId);

    @Query("""
    SELECT COUNT(g) > 0
    FROM GiftEntity g
    JOIN GiftListEntity l ON g.giftListId = l.giftListId
    WHERE g.giftId = :giftId
    AND l.owner = :email
    """)
    boolean existsByGiftIdAndListOwner(@Param("giftId") Long giftId,
                                       @Param("email") String email);

}
