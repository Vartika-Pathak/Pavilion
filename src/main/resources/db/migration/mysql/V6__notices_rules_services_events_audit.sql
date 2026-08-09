CREATE TABLE notices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'general',
    priority VARCHAR(50) NOT NULL DEFAULT 'normal',
    pinned BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at VARCHAR(255),
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE society_rules (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE services (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    category VARCHAR(255) NOT NULL,
    contact_number VARCHAR(255) NOT NULL,
    notes VARCHAR(2000),
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE app_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(4000),
    event_date DATETIME(6) NOT NULL,
    location VARCHAR(255) NOT NULL,
    organizer VARCHAR(255),
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT NOT NULL,
    admin_name VARCHAR(255) NOT NULL,
    method VARCHAR(20) NOT NULL,
    path VARCHAR(500) NOT NULL,
    status_code INT NOT NULL,
    summary VARCHAR(1000) NOT NULL,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE INDEX idx_app_events_event_date ON app_events (event_date);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
