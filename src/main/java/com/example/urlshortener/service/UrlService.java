package com.example.urlshortener.service;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.urlshortener.cache.UrlCacheService;
import com.example.urlshortener.concurrency.UrlHashGenerator;
import com.example.urlshortener.dto.ShortenUrlRequest;
import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlRepository;

@Service
public class UrlService {

    private static final int MAX_GENERATION_ATTEMPTS = 5;

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;
    private final UrlHashGenerator urlHashGenerator;
    private final UrlCacheService urlCacheService;
    private final int expirationHours;

    public UrlService(
            UrlRepository urlRepository,
            ShortCodeGenerator shortCodeGenerator,
            UrlHashGenerator urlHashGenerator,
            UrlCacheService urlCacheService,
            @Value("${app.url-expiration-hours}") int expirationHours) {

        this.urlRepository = urlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.urlHashGenerator = urlHashGenerator;
        this.urlCacheService = urlCacheService;
        this.expirationHours = expirationHours;
    }

    @Transactional
    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {

        String originalUrl = request.getOriginalUrl();

        String originalUrlHash =
                urlHashGenerator.generateHash(originalUrl);

        /*
         * First check whether this URL already exists.
         */
        var existingMapping =
                urlRepository.findByOriginalUrlHash(originalUrlHash);

        if (existingMapping.isPresent()) {

            UrlMapping existing = existingMapping.get();

            return new ShortenUrlResponse(
                    existing.getShortCode(),
                    existing.getOriginalUrl(),
                    "URL already exists. Returning existing short URL."
            );
        }

        /*
         * Generate a new short code.
         */
        for (int attempt = 0;
             attempt < MAX_GENERATION_ATTEMPTS;
             attempt++) {

            String shortCode =
                    shortCodeGenerator.generateShortCode();

            UrlMapping urlMapping =
                    new UrlMapping(originalUrl, shortCode);

            urlMapping.setOriginalUrlHash(originalUrlHash);

            urlMapping.setExpiresAt(
                    LocalDateTime.now().plusHours(expirationHours)
            );

            try {

                UrlMapping saved =
                        urlRepository.saveAndFlush(urlMapping);

                return new ShortenUrlResponse(
                        saved.getShortCode(),
                        saved.getOriginalUrl(),
                        "URL shortened successfully."
                );

            } catch (DataIntegrityViolationException e) {

                /*
                 * Another request may have inserted the same
                 * original URL between our SELECT and INSERT.
                 *
                 * The database UNIQUE constraint on
                 * original_url_hash protects us here.
                 */
                var concurrentMapping =
                        urlRepository.findByOriginalUrlHash(
                                originalUrlHash
                        );

                if (concurrentMapping.isPresent()) {

                    UrlMapping existing =
                            concurrentMapping.get();

                    return new ShortenUrlResponse(
                            existing.getShortCode(),
                            existing.getOriginalUrl(),
                            "URL was created by another request. Returning existing short URL."
                    );
                }

                /*
                 * If the DataIntegrityViolationException was
                 * caused by a short-code collision instead,
                 * generate another short code.
                 */
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique short code"
        );
    }

    public String getOriginalUrl(String shortCode) {

        /*
         * 1. Check Redis cache first.
         */
        String cachedUrl =
                urlCacheService.get(shortCode);

        if (cachedUrl != null) {

            return cachedUrl;
        }

        /*
         * 2. Cache miss.
         *    Query MySQL.
         */
        UrlMapping urlMapping =
                urlRepository.findByShortCode(shortCode)
                        .orElseThrow(
                                () -> new UrlNotFoundException(
                                        "Short URL not found"
                                )
                        );

        /*
         * 3. Store the URL in Redis.
         */
        urlCacheService.put(
                shortCode,
                urlMapping.getOriginalUrl(),
                Duration.ofHours(expirationHours)
        );

        /*
         * 4. Return the original URL.
         */
        return urlMapping.getOriginalUrl();
    }
}