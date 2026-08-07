-- Replaced by an admin-reviewed request queue: residents submit a request instead of being
-- auto-checked against a static allowlist, so the old table is no longer needed.
DROP TABLE approved_residents;

CREATE TABLE resident_verification_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flat_number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    documents_verified BOOLEAN NOT NULL DEFAULT FALSE,
    payment_received BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(255) NOT NULL DEFAULT 'pending',
    reviewed_by BIGINT,
    reviewed_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_verification_requests_reviewed_by FOREIGN KEY (reviewed_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_verification_requests_flat_name ON resident_verification_requests (flat_number, name);
CREATE INDEX idx_verification_requests_status ON resident_verification_requests (status);
