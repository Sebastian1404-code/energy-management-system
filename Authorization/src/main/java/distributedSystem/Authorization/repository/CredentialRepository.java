package distributedSystem.Authorization.repository;

import distributedSystem.Authorization.model.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;



public interface CredentialRepository extends JpaRepository<Credential, Long> {
    Optional<Credential> findByUsername(String username);
}
