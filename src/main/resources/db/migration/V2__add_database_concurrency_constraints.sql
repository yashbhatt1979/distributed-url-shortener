ALTER TABLE url_mapping
ADD COLUMN original_url_hash VARCHAR(64);

UPDATE url_mapping
SET original_url_hash = SHA2(original_url, 256);

ALTER TABLE url_mapping
MODIFY COLUMN original_url_hash VARCHAR(64) NOT NULL;

ALTER TABLE url_mapping
ADD CONSTRAINT uk_url_mapping_original_url_hash
UNIQUE (original_url_hash);