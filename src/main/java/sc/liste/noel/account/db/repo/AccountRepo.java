package sc.liste.noel.account.db.repo;

import org.springframework.data.repository.CrudRepository;
import sc.liste.noel.account.db.entity.AccountEntity;

public interface AccountRepo extends CrudRepository<AccountEntity, String> {

    AccountEntity findByEmail(String email);

    AccountEntity findByPseudo(String pseudo);

    AccountEntity findByEmailAndPassword(String email, String password);


}
