-- Masters: the reference data admins configure before any financial activity happens —
-- the society's own record, its buildings, the flats within them, the expense categories
-- used to classify spend, and the vendors paid for outside work.
CREATE TABLE society (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(1000) NOT NULL,
    contact_number VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE buildings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    total_flats INT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE flats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    building_id BIGINT NOT NULL,
    flat_number VARCHAR(255) NOT NULL,
    flat_type VARCHAR(50) NOT NULL,
    occupied BOOLEAN NOT NULL DEFAULT FALSE,
    ownership_type VARCHAR(50) NOT NULL DEFAULT 'owner',
    CONSTRAINT fk_flats_building FOREIGN KEY (building_id) REFERENCES buildings (id)
) ENGINE=InnoDB;

CREATE TABLE expense_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    gst_slab_percent INT NOT NULL
) ENGINE=InnoDB;

CREATE TABLE vendors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_person_name VARCHAR(255) NOT NULL,
    contact_number VARCHAR(255) NOT NULL,
    address VARCHAR(1000),
    gst_number VARCHAR(255),
    opening_balance_paise BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE INDEX idx_flats_building_id ON flats (building_id);
