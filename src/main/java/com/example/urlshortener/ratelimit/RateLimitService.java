package com.example.urlshortener.ratelimit;

import java.util.Collections;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.example.urlshortener.config.RateLimitConfig;

@Service
public class RateLimitService implements RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;
    private final RateLimitConfig rateLimitConfig;

    private static final String TOKEN_BUCKET_SCRIPT = """
            local key = KEYS[1]

            local capacity = tonumber(ARGV[1])
            local refillTokens = tonumber(ARGV[2])
            local refillDuration = tonumber(ARGV[3])
            local currentTime = tonumber(ARGV[4])

            local tokens = tonumber(redis.call('HGET', key, 'tokens'))
            local lastRefillTime = tonumber(redis.call('HGET', key, 'lastRefillTime'))

            if tokens == nil then
                tokens = capacity
                lastRefillTime = currentTime
            end

            local elapsedTime = currentTime - lastRefillTime

            local refillCount = math.floor(
                elapsedTime / (refillDuration * 1000)
            )

            if refillCount > 0 then

                tokens = math.min(
                    capacity,
                    tokens + (refillCount * refillTokens)
                )

                lastRefillTime = lastRefillTime
                        + (refillCount * refillDuration * 1000)
            end

            if tokens > 0 then

                tokens = tokens - 1

                redis.call(
                    'HSET',
                    key,
                    'tokens',
                    tokens,
                    'lastRefillTime',
                    lastRefillTime
                )

                redis.call(
                    'EXPIRE',
                    key,
                    60
                )

                return 1
            end

            redis.call(
                'HSET',
                key,
                'tokens',
                tokens,
                'lastRefillTime',
                lastRefillTime
            )

            redis.call(
                'EXPIRE',
                key,
                60
            )

            return 0
            """;

    private final DefaultRedisScript<Long> script =
            new DefaultRedisScript<>(
                    TOKEN_BUCKET_SCRIPT,
                    Long.class
            );

    public RateLimitService(
            RedisTemplate<String, String> redisTemplate,
            RateLimitConfig rateLimitConfig) {

        this.redisTemplate = redisTemplate;
        this.rateLimitConfig = rateLimitConfig;
    }

    @Override
    public boolean allowRequest(String key) {
            System.out.println("RATE LIMIT CHECK - key = " + key);

        String redisKey = "rate_limit:" + key;

        Long result = redisTemplate.execute(
                script,
                Collections.singletonList(redisKey),

                String.valueOf(
                        rateLimitConfig.getCapacity()
                ),

                String.valueOf(
                        rateLimitConfig.getRefillTokens()
                ),

                String.valueOf(
                        rateLimitConfig.getRefillDurationSeconds()
                ),

                String.valueOf(
                        System.currentTimeMillis()
                )
        );

        return result != null && result == 1;
    }
}