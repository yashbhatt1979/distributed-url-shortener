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
                        savedUrl.getOriginalUrl()
                );

            } catch (DataIntegrityViolationException ex) {

                /*
                 * Another concurrent request may have generated
                 * the same short code.
                 *
                 * Generate a new code and try again.
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
