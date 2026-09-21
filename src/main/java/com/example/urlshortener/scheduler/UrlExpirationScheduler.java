
package com.example.urlshortener.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.urlshortener.repository.UrlRepository;

@Component
public class UrlExpirationScheduler {

    private final UrlRepository urlRepository;

    public UrlExpirationScheduler(UrlRepository urlRepository) {
        this.urlRepository = urlRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteExpiredUrls() {

        LocalDateTime now = LocalDateTime.now();

        urlRepository.deleteByExpiresAtBefore(now);
    }
}
