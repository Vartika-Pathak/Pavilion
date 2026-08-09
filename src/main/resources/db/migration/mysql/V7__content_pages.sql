CREATE TABLE gallery_photos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    image_url VARCHAR(2000) NOT NULL,
    title VARCHAR(255),
    description VARCHAR(2000),
    uploaded_by VARCHAR(255),
    uploaded_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE contact_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    submitted_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE news_posts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(8000) NOT NULL,
    excerpt VARCHAR(1000),
    author VARCHAR(255) NOT NULL,
    image_url VARCHAR(2000),
    published_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE resident_meetings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    meeting_date DATETIME(6) NOT NULL,
    location VARCHAR(255) NOT NULL,
    notes VARCHAR(2000)
) ENGINE=InnoDB;

CREATE TABLE members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    flat_number VARCHAR(255) NOT NULL,
    bio VARCHAR(2000),
    avatar_url VARCHAR(2000),
    joined_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE join_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    flat_number VARCHAR(255) NOT NULL,
    message VARCHAR(2000),
    status VARCHAR(50) NOT NULL DEFAULT 'pending',
    submitted_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE complaints (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    resident_name VARCHAR(255) NOT NULL,
    resident_flat_number VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'open',
    resolution_note VARCHAR(2000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE maintenance_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    resident_name VARCHAR(255) NOT NULL,
    resident_flat_number VARCHAR(255) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    photo_urls VARCHAR(4000) NOT NULL DEFAULT '[]',
    status VARCHAR(50) NOT NULL DEFAULT 'open',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE INDEX idx_resident_meetings_date ON resident_meetings (meeting_date);
CREATE INDEX idx_complaints_resident_id ON complaints (resident_id);
CREATE INDEX idx_maintenance_requests_resident_id ON maintenance_requests (resident_id);
