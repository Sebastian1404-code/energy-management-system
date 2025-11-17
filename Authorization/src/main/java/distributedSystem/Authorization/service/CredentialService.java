package distributedSystem.Authorization.service;

import distributedSystem.Authorization.dto.CreateUserRequest;
import distributedSystem.Authorization.dto.CreateUserResponse;
import distributedSystem.Authorization.dto.RegisterRequest;
import distributedSystem.Authorization.dto.Role;
import distributedSystem.Authorization.model.Credential;
import distributedSystem.Authorization.repository.CredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CredentialService {

    private final CredentialRepository credentialRepository;
    private final WebClient userServiceClient;
    private final PasswordEncoder passwordEncoder;




    @Transactional
    public Optional<Credential> register(RegisterRequest req) {
        if (credentialRepository.existsByUsername(req.username())) {
            return Optional.empty();
        }


        Long userId = createOrGetUserId(req.username(), req.email(), Role.ADMIN);
        if (userId == null) {
            // Convert to an unchecked exception for controller to map to 502/500
            throw new IllegalStateException("User Service did not return a userId");
        }


        Credential c = new Credential();
        c.setUserId(userId);
        c.setUsername(req.username());
        c.setPasswordHash(passwordEncoder.encode(req.password()));
        c.setRole(Role.ADMIN);
        Credential saved = credentialRepository.save(c);

        return Optional.of(saved);


    }

    private Long createOrGetUserId(String username, String email, Role role) {
        // Try to create
        CreateUserResponse created = userServiceClient.post()
                .uri("/users")
                .bodyValue(new CreateUserRequest(username, email, role))
                .retrieve()
                .onStatus(s -> s.value() == 409, resp -> Mono.empty()) // treat 409 as "already exists"
                .bodyToMono(CreateUserResponse.class)
                .block();

        if (created != null && created.userId() != null) {
            return created.userId();
        }

        // Already exists → fetch id by username
        // If your endpoint is POST /users/id-by-username with body {"username": "..."}
        Long existingId = userServiceClient.post()
                .uri("/users/id-by-username")
                .bodyValue(Map.of("username", username))
                .retrieve()
                .bodyToMono(Long.class)
                .block();

        return existingId; // may be null; controller maps this via IllegalStateException above
    }


    @Transactional(readOnly = true)
    public Optional<Credential> findByUsername(String username) {
        return credentialRepository.findByUsername(username);
    }

    @Transactional
    public void save(Credential credential) {
        credentialRepository.save(credential);
    }



    @Transactional
    public void deleteByUserId(Long userId) {
        credentialRepository.deleteByUserId(userId);
    }

}
