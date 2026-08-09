ALTER TABLE flats ADD COLUMN resident_id BIGINT;

CREATE INDEX idx_flats_resident_id ON flats (resident_id);

CREATE TABLE flat_change_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flat_id BIGINT NOT NULL,
    resident_id BIGINT NOT NULL,
    resident_name VARCHAR(255) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE INDEX idx_flat_change_requests_flat_id ON flat_change_requests (flat_id);
