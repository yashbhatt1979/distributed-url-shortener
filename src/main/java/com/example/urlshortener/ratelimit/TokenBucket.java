package com.example.urlshortener.ratelimit;

public class TokenBucket {

    private final long capacity;
    private final long refillTokens;
    private final long refillDurationSeconds;

    public TokenBucket(
            long capacity,
            long refillTokens,
            long refillDurationSeconds) {

        this.capacity = capacity;
        this.refillTokens = refillTokens;
        this.refillDurationSeconds = refillDurationSeconds;
    }

    public long getCapacity() {
        return capacity;
    }

    public long getRefillTokens() {
        return refillTokens;
    }

    public long getRefillDurationSeconds() {
        return refillDurationSeconds;
    }
}