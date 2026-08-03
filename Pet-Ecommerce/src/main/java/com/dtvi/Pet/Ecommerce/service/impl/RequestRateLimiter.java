package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RequestRateLimiter {

    private final Map<String, Window> windows =
            new ConcurrentHashMap<>();

    public void check(
            String scope,
            String identity,
            int maximumRequests,
            Duration duration
    ) {
        String key = scope + ":"
                + (identity == null || identity.isBlank()
                ? "unknown"
                : identity.trim().toLowerCase());

        long now = System.currentTimeMillis();
        Window window = windows.computeIfAbsent(
                key,
                ignored -> new Window(now)
        );

        synchronized (window) {
            if (now - window.startedAt >= duration.toMillis()) {
                window.startedAt = now;
                window.count = 0;
            }

            if (window.count >= maximumRequests) {
                throw BusinessException.tooManyRequests(
                        "RATE_LIMIT_EXCEEDED",
                        "Too many requests. Please try again later."
                );
            }

            window.count++;
        }
    }

    @Scheduled(fixedDelay = 600_000)
    public void cleanup() {
        long expiredBefore =
                System.currentTimeMillis()
                        - Duration.ofHours(1).toMillis();

        windows.entrySet().removeIf(
                entry -> entry.getValue().startedAt < expiredBefore
        );
    }

    private static final class Window {
        private long startedAt;
        private int count;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}