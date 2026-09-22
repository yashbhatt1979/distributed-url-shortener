package com.example.urlshortener.service;

public interface DistributedLockService {

    boolean tryLock(String lockKey, String lockValue, long expirationSeconds);

    void unlock(String lockKey, String lockValue);
}