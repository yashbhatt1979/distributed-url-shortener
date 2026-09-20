package com.example.urlshortener.service;

import org.springframework.stereotype.Service;

import com.example.urlshortener.dto.ShortenUrlRequest;
import com.example.urlshortener.dto.ShortenUrlResponse;
import com.example.urlshortener.exception.UrlNotFoundException;
import com.example.urlshortener.model.UrlMapping;
import com.example.urlshortener.repository.UrlRepository;

@Service
public class UrlService {

    private final UrlRepository urlRepository;
    private final ShortCodeGenerator shortCodeGenerator;

    public UrlService(
            UrlRepository urlRepository,
            ShortCodeGenerator shortCodeGenerator) {

        this.urlRepository = urlRepository;
        this.shortCodeGenerator = shortCodeGenerator;
    }

    public ShortenUrlResponse shortenUrl(ShortenUrlRequest request) {

        String shortCode = shortCodeGenerator.generateShortCode();

        UrlMapping urlMapping = new UrlMapping();

        urlMapping.setOriginalUrl(request.getOriginalUrl());
        urlMapping.setShortCode(shortCode);

        UrlMapping savedUrl = urlRepository.save(urlMapping);

        return new ShortenUrlResponse(
                savedUrl.getShortCode(),
                savedUrl.getOriginalUrl()
        );
    }

    public String getOriginalUrl(String shortCode) {

        UrlMapping urlMapping = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() ->
                        new UrlNotFoundException(
                                "Short URL not found: " + shortCode
                        )
                );

        return urlMapping.getOriginalUrl();
    }
}