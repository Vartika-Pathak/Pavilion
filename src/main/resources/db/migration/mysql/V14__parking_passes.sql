CREATE TABLE parking_passes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flat_number VARCHAR(50) NOT NULL UNIQUE,
    purchased_by_resident_id BIGINT NOT NULL,
    purchased_by_name VARCHAR(255) NOT NULL,
    amount_paid_cents INT NOT NULL,
    stripe_session_id VARCHAR(255),
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;
