package com.example.urlshortener.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.urlshortener.model.UrlMapping;

public interface UrlRepository extends JpaRepository<UrlMapping, Long> {

    Optional<UrlMapping> findByShortCode(String shortCode);

    Optional<UrlMapping> findByOriginalUrl(String originalUrl);

    void deleteByExpiresAtBefore(LocalDateTime time);
}