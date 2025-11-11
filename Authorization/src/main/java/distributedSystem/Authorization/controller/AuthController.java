package distributedSystem.Authorization.controller;

import distributedSystem.Authorization.model.Credential;
import distributedSystem.Authorization.repository.CredentialRepository;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import java.util.Date;
import java.util.Map;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    @Autowired
    private CredentialRepository repo;

    // Register new user
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Credential newUser) {
        if (repo.findByUsername(newUser.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("User already exists");
        }
        repo.save(newUser);
        return ResponseEntity.ok("Registered successfully");
    }

    // Login existing user
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Credential request) {
        var user = repo.findByUsername(request.getUsername()).orElse(null);

        if (user == null || !user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }

        String token = Jwts.builder()
                .setSubject(user.getUsername())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600_000))
                .signWith(SECRET_KEY)
                .compact();

        return ResponseEntity.ok(Map.of("token", token));
    }
}
