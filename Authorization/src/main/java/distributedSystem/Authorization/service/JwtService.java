package distributedSystem.Authorization.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;
import io.jsonwebtoken.*;


@Service
public class JwtService {

    private final SecretKey key;
    private final String issuer;
    private final String audience;
    private final long ttlMinutes;

    public JwtService(
            @Value("${JWT_SECRET}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience,
            @Value("${jwt.ttl-minutes}") long ttlMinutes
    ) {
        // HS256 requires a reasonably long key; make sure the env value is long/random
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.audience = audience;
        this.ttlMinutes = ttlMinutes;
    }

    public String createToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttlMinutes, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setIssuer(issuer)
                .setAudience(audience)
                .setSubject(String.valueOf(userId))
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        "username", username,
                        "role", role
                ))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // distributedSystem/Authorization/security/JwtService.java

    public Claims verifyAndGetClaims(String token) {
        // token without "Bearer "
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireAudience(audience)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

}
