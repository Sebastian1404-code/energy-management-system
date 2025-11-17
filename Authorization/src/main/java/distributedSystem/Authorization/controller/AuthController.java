package distributedSystem.Authorization.controller;

import distributedSystem.Authorization.dto.*;
import distributedSystem.Authorization.model.Credential;
import distributedSystem.Authorization.service.CredentialService;
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

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CredentialService credentialService;


    public AuthController(PasswordEncoder passwordEncoder,
                          JwtService jwtService, CredentialService credentialService) {
        this.credentialService = credentialService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Registers CLIENT users.
     * Flow:
     * 1) Call User Service to create/get user (idempotent on username).
     * 2) Store credentials with BCrypt hash and returned userId.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest req) {


        try {
            var maybeCred = credentialService.register(req);

            if (maybeCred.isEmpty()) {
                return ResponseEntity.ok("Already registered");
            }

            var cred = maybeCred.get();
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of(
                            "userId", cred.getUserId(),
                            "username", cred.getUsername(),
                            "message", "Registered successfully"
                    ));
        } catch (IllegalStateException e) {
            // e.g., user service didn’t return an id or other expected condition failed
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/credential")
    public ResponseEntity<?> createCredentialByAdmin(@Valid @RequestBody CredentialRequest req) {

        Credential c = new Credential();
        c.setUserId(req.getUserId());
        c.setUsername(req.getUsername());
        c.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        c.setRole(req.getRole());
        credentialService.save(c);

        return ResponseEntity.ok("Credentials for user created in administrator are registered");

    }

    /**
     * Login: validates password, returns JWT with {sub=userId, username, role}.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        var cred = credentialService.findByUsername(req.getUsername()).orElse(null);

        if (cred == null || !passwordEncoder.matches(req.getPassword(), cred.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        System.out.println(cred.getRole());
        String token = jwtService.createToken(cred.getUserId(), cred.getUsername(), cred.getRole().toString());
        return ResponseEntity.ok(new TokenResponse(token));
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<?> deleteCredentialByUserId(@PathVariable Long userId) {
        credentialService.deleteByUserId(userId);
//        if (!deleted) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(Map.of("message", "Credential not found for userId=" + userId));
//        }
        return ResponseEntity.noContent().build();
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
