package com.pavilion.api.security;

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

    public String signSessionToken(Long userId) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + SESSION_DURATION.toMillis());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public Optional<Long> verifySessionToken(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            return Optional.of(Long.parseLong(claims.getSubject()));
        } catch (JwtException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    public long sessionMaxAgeSeconds() {
        return SESSION_DURATION.toSeconds();
    }
}
