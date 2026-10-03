package in.mak.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    /*
     * DEVELOPMENT KEY ONLY.
     *
     * Before deployment, this must be moved to an environment
     * variable / secret manager and never committed to Git.
     */
    private static final String SECRET =
            "FamilyTrackerDevelopmentSecretKeyChangeBeforeProduction123456";

    private final SecretKey key =
            Keys.hmacShaKeyFor(
                    SECRET.getBytes(StandardCharsets.UTF_8)
            );

    private final long expirationMillis =
            1000L * 60 * 60 * 24;

    public String generateToken(Long userId, String email, String role) {

        Date now = new Date();

        Date expiration =
                new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    public Long extractUserId(String token) {

        String subject =
                Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject();

        return Long.valueOf(subject);
    }
    public String extractRole(String token) {

        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
    }
}