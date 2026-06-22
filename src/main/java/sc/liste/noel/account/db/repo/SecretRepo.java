package sc.liste.noel.account.db.repo;

import org.springframework.data.repository.CrudRepository;
import sc.liste.noel.account.db.entity.SecretEntity;

public interface SecretRepo extends CrudRepository<SecretEntity, String> {

	SecretEntity findBySecret(String secret);

}
