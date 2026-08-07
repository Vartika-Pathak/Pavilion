package com.pavilion.api.security;

import com.pavilion.api.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final Duration SESSION_DURATION = Duration.ofDays(7);

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    void init() {
        if ("dev-only-insecure-secret-change-me".equals(secret)) {
            log.warn("JWT_SECRET is not set — using an insecure development default. "
                    + "Set JWT_SECRET before deploying anywhere real.");
        }
        // HS256 needs a key of at least 256 bits; pad a short dev secret out so
        // this doesn't throw before anyone gets a chance to see the warning above.
        String padded = secret.length() < 32 ? (secret + "0".repeat(32 - secret.length())) : secret;
        this.key = Keys.hmacShaKeyFor(padded.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * The name/email/flatNumber/role claims exist for the separate Node API server, which shares
     * this JWT secret and the "session" cookie name but keeps its own disconnected user table —
     * it has no way to look up those fields itself, so we hand them over in the token instead.
     */
    public String signSessionToken(User user) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + SESSION_DURATION.toMillis());
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("name", user.getName())
                .claim("email", user.getEmail())
                .claim("flatNumber", user.getFlatNumber())
                .claim("role", user.getRole())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Optional<Long> verifySessionToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            // IllegalArgumentException covers jjwt's own input-validation rejections (e.g. an
            // empty token), which aren't JwtExceptions but are just as much "not a valid
            // session" as an expired or tampered one — NumberFormatException from a corrupt
            // subject claim is an IllegalArgumentException too, so this also subsumes that case.
            return Optional.empty();
        }
    }

    public long sessionMaxAgeSeconds() {
        return SESSION_DURATION.toSeconds();
    }
}
