package com.pavilion.api.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

// In-memory sliding-window limiter, keyed by client IP. The chat endpoint is
// open to signed-out visitors (see ChatController), so without this a single
// visitor could exhaust Gemini's shared free-tier daily quota for everyone.
// Deliberately simple (no external library) — resets on redeploy, which is
// fine for its purpose here.
@Component
public class ChatRateLimiter {

    private static final int MAX_REQUESTS = 8;
    private static final long WINDOW_SECONDS = 60;

    private final ConcurrentHashMap<String, Deque<Instant>> requestsByKey = new ConcurrentHashMap<>();

    /** Returns true if the request is allowed, recording it if so. */
    public synchronized boolean allow(String key) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(WINDOW_SECONDS);

        Deque<Instant> timestamps = requestsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!timestamps.isEmpty() && timestamps.peekFirst().isBefore(windowStart)) {
            timestamps.pollFirst();
        }

        if (timestamps.size() >= MAX_REQUESTS) {
            return false;
        }
        timestamps.addLast(now);
        return true;
    }
}
