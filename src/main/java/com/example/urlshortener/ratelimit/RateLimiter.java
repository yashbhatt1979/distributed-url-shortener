package com.example.urlshortener.ratelimit;

public interface RateLimiter {

    boolean allowRequest(String key);
}