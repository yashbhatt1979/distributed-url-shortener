package com.example.urlshortener.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RateLimitConfig {

    @Value("${app.rate-limit.capacity}")
    private int capacity;

    @Value("${app.rate-limit.refill-tokens}")
    private int refillTokens;

    @Value("${app.rate-limit.refill-duration-seconds}")
    private long refillDurationSeconds;

    public int getCapacity() {
        return capacity;
    }

    public int getRefillTokens() {
        return refillTokens;
    }

    public long getRefillDurationSeconds() {
        return refillDurationSeconds;
    }
}