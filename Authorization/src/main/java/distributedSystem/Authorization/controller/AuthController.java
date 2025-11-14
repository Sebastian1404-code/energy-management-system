package distributedSystem.Authorization.controller;

import distributedSystem.Authorization.dto.*;
import distributedSystem.Authorization.model.Credential;
import distributedSystem.Authorization.repository.CredentialRepository;
import distributedSystem.Authorization.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final CredentialRepository repo;
    private final PasswordEncoder passwordEncoder;
    private final WebClient userServiceClient;
    private final JwtService jwtService;

    public AuthController(CredentialRepository repo,
                          PasswordEncoder passwordEncoder,
                          WebClient userServiceClient,
                          JwtService jwtService) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
        this.userServiceClient = userServiceClient;
        this.jwtService = jwtService;
    }

    /**
     * Registers CLIENT users.
     * Flow:
     * 1) Call User Service to create/get user (idempotent on username).
     * 2) Store credentials with BCrypt hash and returned userId.
     */
    @PostMapping("/register")
    @Transactional
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {
        // 1) Create (or get) user from User Service
        CreateUserResponse created = userServiceClient.post()
                .uri("/users")
                .bodyValue(new CreateUserRequest(req.username(), req.email(), Role.ADMIN))
                .retrieve()
                .bodyToMono(CreateUserResponse.class)
                .block();

        if (created == null || created.userId() == null) {
            return ResponseEntity.internalServerError().body("User Service did not return a userId");
        }

        // 2) Upsert credentials (idempotent on username)
        Optional<Credential> existing = repo.findByUsername(req.username());
        if (existing.isPresent()) {
            // If already registered, just OK (idempotent behavior)
            return ResponseEntity.ok("Already registered");
        }

        Credential c = new Credential();
        c.setUserId(created.userId());
        c.setUsername(req.username());
        c.setPasswordHash(passwordEncoder.encode(req.password()));
        c.setRole("ADMIN");
        repo.save(c);

        return ResponseEntity.ok("Registered successfully");
    }

    /**
     * Login: validates password, returns JWT with {sub=userId, username, role}.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var cred = repo.findByUsername(req.getUsername()).orElse(null);

        if (cred == null || !passwordEncoder.matches(req.getPassword(), cred.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        System.out.println(cred.getRole());
        String token = jwtService.createToken(cred.getUserId(), cred.getUsername(), cred.getRole());
        return ResponseEntity.ok(new TokenResponse(token));
    }


    @GetMapping("/validate")
    public ResponseEntity<Void> validate(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String raw = authHeader.substring("Bearer ".length()).trim();
        try {
            Claims claims = jwtService.verifyAndGetClaims(raw);
            // Build headers for Traefik to forward to services
            HttpHeaders h = new HttpHeaders();
            h.add("X-User-Id", claims.getSubject());                 // sub = userId
            h.add("X-Username", String.valueOf(claims.get("username")));
            h.add("X-Role", String.valueOf(claims.get("role")));
            return new ResponseEntity<>(h, HttpStatus.OK);
        } catch (JwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}
