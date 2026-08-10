CREATE TABLE vehicles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    plate_number VARCHAR(50) NOT NULL,
    vehicle_type VARCHAR(50) NOT NULL,
    owner_name VARCHAR(255) NOT NULL,
    flat_number VARCHAR(50) NOT NULL,
    owner_phone VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;
