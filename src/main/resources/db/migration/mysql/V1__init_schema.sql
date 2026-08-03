CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    flat_number VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL DEFAULT 'resident',
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE pending_signups (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    flat_number VARCHAR(255) NOT NULL,
    otp_code VARCHAR(255) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE visits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    visit_type VARCHAR(255) NOT NULL,
    visitor_name VARCHAR(255) NOT NULL,
    visitor_phone VARCHAR(255),
    visitor_email VARCHAR(255),
    otp_code VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'pending',
    approved_by BIGINT,
    responded_at DATETIME(6),
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_visits_resident FOREIGN KEY (resident_id) REFERENCES users (id),
    CONSTRAINT fk_visits_approved_by FOREIGN KEY (approved_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE emergency_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'active',
    resolved_by BIGINT,
    resolved_at DATETIME(6),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_alerts_resident FOREIGN KEY (resident_id) REFERENCES users (id),
    CONSTRAINT fk_alerts_resolved_by FOREIGN KEY (resolved_by) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE TABLE chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(255) NOT NULL,
    user_id BIGINT,
    role VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_chat_messages_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_visits_resident_id ON visits (resident_id);
CREATE INDEX idx_visits_otp_status ON visits (otp_code, status);
CREATE INDEX idx_emergency_alerts_resident_status ON emergency_alerts (resident_id, status);
CREATE INDEX idx_emergency_alerts_status ON emergency_alerts (status);
CREATE INDEX idx_chat_messages_session_id ON chat_messages (session_id);
