package com.example.urlshortener.service;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

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
    private final long expirationHours;

    public UrlService(
            UrlRepository urlRepository,
            ShortCodeGenerator shortCodeGenerator,
            @Value("${app.url-expiration-hours}") long expirationHours) {

        this.urlRepository = urlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
        this.expirationHours = expirationHours;
    }

    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {

        /*
         * First check whether this original URL already exists.
         */
        var existingUrl = urlRepository.findByOriginalUrl(
                request.getOriginalUrl()
        );

        if (existingUrl.isPresent()) {

            UrlMapping urlMapping = existingUrl.get();

            /*
             * If the existing short URL has not expired,
             * return the existing short code.
             *
             * No new database row is created.
             */
            if (urlMapping.getExpiresAt() != null &&
                    LocalDateTime.now().isBefore(urlMapping.getExpiresAt())) {

                return new ShortenUrlResponse(
                        urlMapping.getShortCode(),
                        urlMapping.getOriginalUrl(),
                        "URL already shortened. Returning existing short code."
                );
            }

            /*
             * If the existing URL has expired, remove it.
             * A new short code can then be generated.
             */
            urlRepository.delete(urlMapping);
        }

        /*
         * Generate a new short code.
         */
        for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {

            String shortCode = shortCodeGenerator.generateShortCode();

            LocalDateTime expiresAt =
                    LocalDateTime.now().plusHours(expirationHours);

            UrlMapping urlMapping = new UrlMapping();

            urlMapping.setOriginalUrl(request.getOriginalUrl());
            urlMapping.setShortCode(shortCode);
            urlMapping.setExpiresAt(expiresAt);

            try {

                UrlMapping savedUrl = urlRepository.save(urlMapping);

                return new ShortenUrlResponse(
                        savedUrl.getShortCode(),
                        savedUrl.getOriginalUrl(),
                        "URL shortened successfully."
                );

            } catch (DataIntegrityViolationException ex) {

                /*
                 * Another concurrent request may have inserted
                 * the same original URL or the same short code.
                 *
                 * Check whether the original URL now exists.
                 */
                var concurrentUrl = urlRepository.findByOriginalUrl(
                        request.getOriginalUrl()
                );

                if (concurrentUrl.isPresent()) {

                    return new ShortenUrlResponse(
                            concurrentUrl.get().getShortCode(),
                            concurrentUrl.get().getOriginalUrl(),
                            "URL already shortened. Returning existing short code."
                    );
                }

                /*
                 * If the failure was caused by a short-code collision,
                 * generate another short code and try again.
                 */
                if (attempt == MAX_GENERATION_ATTEMPTS) {

                    throw new IllegalStateException(
                            "Unable to generate a unique short code",
                            ex
                    );
                }
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique short code"
        );
    }

    public String getOriginalUrl(String shortCode) {

        UrlMapping urlMapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException(
                                "Short URL not found: " + shortCode
                        )
                );

        if (urlMapping.getExpiresAt() != null &&
                !LocalDateTime.now().isBefore(urlMapping.getExpiresAt())) {

            throw new UrlNotFoundException(
                    "Short URL has expired: " + shortCode
            );
        }

        return urlMapping.getOriginalUrl();
    }
}