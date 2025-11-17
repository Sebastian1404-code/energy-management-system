package distributedSystem.Authorization.controller;

import distributedSystem.Authorization.dto.*;
import distributedSystem.Authorization.model.Credential;
import distributedSystem.Authorization.service.CredentialService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final CredentialService credentialService;


    public AuthController(PasswordEncoder passwordEncoder,
                        CredentialService credentialService) {
        this.credentialService = credentialService;
        this.passwordEncoder = passwordEncoder;
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
            Optional<Credential> credential = credentialService.register(req);

            if (credential.isEmpty()) {
                return ResponseEntity.ok("Already registered");
            }

            Credential cred = credential.get();
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
        return credentialService.login(req)
                .<ResponseEntity<?>>map(ResponseEntity::ok) // 200 + TokenResponse
                .orElseGet(() -> ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid credentials")));
    }

    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<?> deleteCredentialByUserId(@PathVariable Long userId) {
        credentialService.deleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/validate")
    public ResponseEntity<Void> validate(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return credentialService.buildForwardHeaders(authHeader)
                .map(headers -> new ResponseEntity<Void>(headers, HttpStatus.OK))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }


}
