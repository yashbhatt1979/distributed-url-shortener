package com.example.urlshortener.ratelimit;

public class Bucket {

    private long tokens;
    private long lastRefillTime;

    public Bucket(long tokens, long lastRefillTime) {
        this.tokens = tokens;
        this.lastRefillTime = lastRefillTime;
    }

    public long getTokens() {
        return tokens;
    }

    public void setTokens(long tokens) {
        this.tokens = tokens;
    }

    public long getLastRefillTime() {
        return lastRefillTime;
    }

    public void setLastRefillTime(long lastRefillTime) {
        this.lastRefillTime = lastRefillTime;
    }
}