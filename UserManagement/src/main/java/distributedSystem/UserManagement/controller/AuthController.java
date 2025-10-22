package distributedSystem.UserManagement.controller;

import distributedSystem.UserManagement.dto.LoginRequest;
import distributedSystem.UserManagement.dto.LoginResponse;
import distributedSystem.UserManagement.model.UserEntity;
import distributedSystem.UserManagement.repository.UserRepository;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.Optional;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

import javax.crypto.SecretKey;
import java.util.Date;

@RestController
@RequestMapping("/auth")
public class AuthController {



    @Autowired
    private UserRepository userRepository;

    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);



    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<UserEntity> userOpt = userRepository.findByUsername(request.getUsername());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        UserEntity user = userOpt.get();
        if (!Objects.equals(user.getPassword(), request.getPassword())) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        // In a real app, generate a JWT here

        return ResponseEntity.ok(new LoginResponse(generateToken(user.getUsername(),user.getRole().name(),"mydevsecret123")));
    }

    public String generateToken(String username, String role, String secretKey) {
        long expirationMillis = 3600_000; // 1 hour
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", role) // single role as string
                .setExpiration(new Date(System.currentTimeMillis() + expirationMillis))
                .signWith(SECRET_KEY)
                .compact();
    }



}


