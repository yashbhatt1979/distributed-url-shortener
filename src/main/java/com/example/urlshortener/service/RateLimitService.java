package com.example.urlshortener.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {

    private static final int MAX_REQUESTS = 10;

    private final ConcurrentHashMap<String, AtomicInteger> requestCounts =
            new ConcurrentHashMap<>();

    public boolean isAllowed(String clientId) {

        AtomicInteger count = requestCounts.computeIfAbsent(
                clientId,
                key -> new AtomicInteger(0)
        );

        return count.incrementAndGet() <= MAX_REQUESTS;
    }
}