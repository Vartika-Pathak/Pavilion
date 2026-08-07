ALTER TABLE pending_signups ADD COLUMN family_members_json TEXT;

CREATE TABLE approved_residents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flat_number VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE family_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    relation VARCHAR(255) NOT NULL,
    age INT,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_family_members_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB;

CREATE INDEX idx_approved_residents_flat_number ON approved_residents (flat_number);
CREATE INDEX idx_family_members_user_id ON family_members (user_id);

-- A small demo roster so the pre-signup verification step has something to check against
-- without an admin UI to manage it yet — the committee adds real entries the same way
-- (a row in this table) until one exists.
INSERT INTO approved_residents (flat_number, name, created_at) VALUES
    ('A-101', 'Alex Sharma', NOW()),
    ('B-201', 'Priya Mehta', NOW()),
    ('C-301', 'Rohan Verma', NOW());
