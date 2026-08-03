package com.pavilion.api.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatRateLimiterTest {

    @Test
    void allowsUpToEightRequestsThenBlocksTheNinth() {
        ChatRateLimiter limiter = new ChatRateLimiter();

        for (int i = 0; i < 8; i++) {
            assertThat(limiter.allow("client-a")).isTrue();
        }
        assertThat(limiter.allow("client-a")).isFalse();
    }

    @Test
    void differentKeysHaveIndependentBudgets() {
        ChatRateLimiter limiter = new ChatRateLimiter();

        for (int i = 0; i < 8; i++) {
            limiter.allow("client-a");
        }

        assertThat(limiter.allow("client-b")).isTrue();
    }

    @Test
    void blockedKeyStaysBlockedUntilItsWindowFrees() {
        ChatRateLimiter limiter = new ChatRateLimiter();

        for (int i = 0; i < 8; i++) {
            limiter.allow("client-a");
        }

        assertThat(limiter.allow("client-a")).isFalse();
        assertThat(limiter.allow("client-a")).isFalse();
    }
}
