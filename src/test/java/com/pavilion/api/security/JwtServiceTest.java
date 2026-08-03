package com.pavilion.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = newService("unit-test-secret-padded-to-32-bytes-min");
    }

    private static JwtService newService(String secret) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.invokeMethod(service, "init");
        return service;
    }

    @Test
    void signedTokenRoundTripsToTheSameUserId() {
        String token = jwtService.signSessionToken(42L);

        Optional<Long> userId = jwtService.verifySessionToken(token);

        assertThat(userId).contains(42L);
    }

    @Test
    void garbageTokenFailsVerification() {
        assertThat(jwtService.verifySessionToken("not-a-real-token")).isEmpty();
    }

    @Test
    void emptyTokenFailsVerification() {
        assertThat(jwtService.verifySessionToken("")).isEmpty();
    }

    @Test
    void tokenSignedWithADifferentSecretFailsVerification() {
        JwtService otherService = newService("a-completely-different-secret-also-32-bytes");

        String tokenFromOtherService = otherService.signSessionToken(1L);

        assertThat(jwtService.verifySessionToken(tokenFromOtherService)).isEmpty();
    }

    @Test
    void shortDevSecretIsPaddedRatherThanRejected() {
        // JwtService pads any secret under 32 bytes so a short JWT_SECRET doesn't crash the app
        // on startup — it just gets a startup warning instead (see JwtService.init).
        JwtService shortSecretService = newService("short");

        String token = shortSecretService.signSessionToken(7L);

        assertThat(shortSecretService.verifySessionToken(token)).contains(7L);
    }

    @Test
    void sessionMaxAgeIsSevenDaysInSeconds() {
        assertThat(jwtService.sessionMaxAgeSeconds()).isEqualTo(7 * 24 * 60 * 60L);
    }
}
