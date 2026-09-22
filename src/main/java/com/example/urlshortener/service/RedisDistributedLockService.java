package com.example.urlshortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

@Service
public class RedisDistributedLockService implements DistributedLockService {

    private final StringRedisTemplate redisTemplate;

    private static final String UNLOCK_SCRIPT = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """;

    public RedisDistributedLockService(
            StringRedisTemplate redisTemplate) {

        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean tryLock(
            String lockKey,
            String lockValue,
            long expirationSeconds) {

        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                lockKey,
                lockValue,
                expirationSeconds,
                TimeUnit.SECONDS
        );

        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public void unlock(
            String lockKey,
            String lockValue) {

        DefaultRedisScript<Long> script =
                new DefaultRedisScript<>();

        script.setScriptText(UNLOCK_SCRIPT);
        script.setResultType(Long.class);

        redisTemplate.execute(
                script,
                Collections.singletonList(lockKey),
                lockValue
        );
    }
}