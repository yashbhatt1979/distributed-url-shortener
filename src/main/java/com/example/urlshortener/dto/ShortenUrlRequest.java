package com.example.urlshortener.dto;

public class ShortenUrlRequest {

    private String originalUrl;

    public ShortenUrlRequest() {
    }

    public ShortenUrlRequest(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }
}