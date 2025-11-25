package distributedSystem.Authorization.service;

import distributedSystem.Authorization.dto.*;
import distributedSystem.Authorization.events.UserKafkaGateway;
import distributedSystem.Authorization.model.Credential;
import distributedSystem.Authorization.repository.CredentialRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
    private final JwtService jwtService;
    private final UserKafkaGateway userKafkaGateway; // <-- inject gateway





    @Transactional
    public Optional<Credential> register(RegisterRequest req) {
        if (credentialRepository.existsByUsername(req.username())) {
            return Optional.empty();
        }


        //Long userId = createOrGetUserId(req.username(), req.email(), Role.ADMIN);

        Long userId = userKafkaGateway.createOrGetUserId(
                req.username(),
                req.email(),
                Role.ADMIN
        );
        if (userId == null) {
            throw new IllegalStateException("User Service did not return a userId");
        }


        return createCredentialsTx(userId, req.username(), req.password(), Role.ADMIN);


    }

    @Transactional
    protected Optional<Credential> createCredentialsTx(Long userId, String username, String rawPassword, Role role) {
        if (credentialRepository.existsByUsername(username)) {
            return Optional.empty();
        }

        try {
            Credential c = new Credential();
            c.setUserId(userId);
            c.setUsername(username);
            c.setPasswordHash(passwordEncoder.encode(rawPassword));
            c.setRole(role);
            Credential saved = credentialRepository.save(c);
            return Optional.of(saved);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return Optional.empty();
        }
    }

    /*private Long createOrGetUserId(String username, String email, Role role) {
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


        return userServiceClient.post()
                .uri("/users/id-by-username")
                .bodyValue(Map.of("username", username))
                .retrieve()
                .bodyToMono(Long.class)
                .block();

    }*/



    // LOGIN

    @Transactional(readOnly = true)
    public Optional<TokenResponse> login(LoginRequest req) {
        // Find user, check password, then issue token — all in domain layer
        return credentialRepository.findByUsername(req.getUsername())
                .filter(cred -> passwordEncoder.matches(req.getPassword(), cred.getPasswordHash()))
                .map(cred -> {
                    String token = jwtService.createToken(
                            cred.getUserId(),
                            cred.getUsername(),
                            cred.getRole().name()
                    );
                    return new TokenResponse(token);
                });
    }

    public Optional<HttpHeaders> buildForwardHeaders(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String raw = authorizationHeader.substring("Bearer ".length()).trim();
        try {
            Claims claims = jwtService.verifyAndGetClaims(raw);

            HttpHeaders h = new HttpHeaders();
            h.add("X-User-Id", claims.getSubject());                   // sub = userId
            h.add("X-Username", String.valueOf(claims.get("username")));
            h.add("X-Role", String.valueOf(claims.get("role")));
            return Optional.of(h);

        } catch (JwtException ex) {
            return Optional.empty();
        }
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
