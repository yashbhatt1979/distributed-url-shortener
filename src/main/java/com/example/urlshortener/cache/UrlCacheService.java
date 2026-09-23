package com.example.urlshortener.cache;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class UrlCacheService {

    private static final String CACHE_PREFIX = "url:";

    private final RedisTemplate<String, String> redisTemplate;

    public UrlCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String get(String shortCode) {

        String key = CACHE_PREFIX + shortCode;

        return redisTemplate.opsForValue().get(key);
    }

    public void put(String shortCode, String originalUrl, Duration ttl) {

        String key = CACHE_PREFIX + shortCode;

        redisTemplate.opsForValue().set(key, originalUrl, ttl);
    }

    public void evict(String shortCode) {

        String key = CACHE_PREFIX + shortCode;

        redisTemplate.delete(key);
    }
}