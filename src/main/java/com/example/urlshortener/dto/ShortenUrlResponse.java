package com.example.urlshortener.dto;

public class ShortenUrlResponse {

    private String shortCode;
    private String originalUrl;
    private String message;

    public ShortenUrlResponse(
            String shortCode,
            String originalUrl,
            String message) {

        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.message = message;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}