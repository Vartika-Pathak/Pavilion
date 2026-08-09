CREATE TABLE uploaded_files (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    data LONGBLOB NOT NULL,
    size BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;
