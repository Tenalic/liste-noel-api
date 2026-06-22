package sc.liste.noel.account.service;

import org.springframework.stereotype.Service;
import sc.liste.noel.account.db.repo.SecretRepo;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class SecretService {

   private final SecretRepo secretRepo;

    private final Set<String> alreadyAuthorizedSecrets = new HashSet<>();

    public SecretService(SecretRepo secretRepo) {
        this.secretRepo = secretRepo;
    }

    public boolean verifySecret(String secret) {
        if (alreadyAuthorizedSecrets.contains(secret)) {
            return true;
        }
        if (Optional.ofNullable(secretRepo.findBySecret(secret)).isPresent()) {
            alreadyAuthorizedSecrets.add(secret);
            return true;
        }
        return false;
    }

}
