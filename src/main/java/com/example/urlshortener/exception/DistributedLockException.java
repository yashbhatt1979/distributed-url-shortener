package com.example.urlshortener.exception;

public class DistributedLockException extends RuntimeException {

    public DistributedLockException(String message) {
        super(message);
    }
}