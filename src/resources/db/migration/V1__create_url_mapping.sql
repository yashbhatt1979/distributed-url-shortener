CREATE TABLE url_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    short_code VARCHAR(20) NOT NULL,

    original_url VARCHAR(2048) NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    expires_at TIMESTAMP NULL,

    CONSTRAINT uk_url_mapping_short_code UNIQUE (short_code)
);