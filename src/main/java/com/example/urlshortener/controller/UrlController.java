package com.example.urlshortener.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.urlshortener.dto.ShortenUrlRequest;
import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.ratelimit.RateLimiter;
import com.example.urlshortener.service.UrlService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/shortenUrl")
public class UrlController {

    private final UrlService urlService;
    private final RateLimiter rateLimiter;

    public UrlController(
            UrlService urlService,
            RateLimiter rateLimiter) {

        this.urlService = urlService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping
    public ResponseEntity<ShortenUrlResponse> shortenUrl(
            @RequestBody ShortenUrlRequest request,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();

        if (!rateLimiter.allowRequest(clientIp)) {
            throw new com.example.urlshortener.ratelimit.RateLimitExceededException(
                    "Too many requests. Please try again later."
            );
        }

        ShortenUrlResponse response =
                urlService.shortenUrl(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<String> redirectToOriginalUrl(
            @PathVariable String shortCode,
            HttpServletRequest httpRequest) {

        String clientIp = httpRequest.getRemoteAddr();

        if (!rateLimiter.allowRequest(clientIp)) {
            throw new com.example.urlshortener.ratelimit.RateLimitExceededException(
                    "Too many requests. Please try again later."
            );
        }

        String originalUrl =
                urlService.getOriginalUrl(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .header("Location", originalUrl)
                .body("Redirecting to: " + originalUrl);
    }
}