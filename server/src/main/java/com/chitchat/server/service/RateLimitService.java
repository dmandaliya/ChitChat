package com.chitchat.server.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory rate limiter: max 30 requests per user per minute.
 */
@Service
public class RateLimitService {

    private static final int MAX_REQUESTS = 30;
    private static final long WINDOW_MS = 60_000; // 1 minute

    // username -> [windowStartMs, count]
    private final ConcurrentHashMap<String, long[]> counters = new ConcurrentHashMap<>();

    public boolean isAllowed(String username) {
        long now = System.currentTimeMillis();
        long[] state = counters.compute(username, (k, v) -> {
            if (v == null || now - v[0] > WINDOW_MS) {
                return new long[]{now, 1};
            }
            v[1]++;
            return v;
        });
        return state[1] <= MAX_REQUESTS;
    }
}
