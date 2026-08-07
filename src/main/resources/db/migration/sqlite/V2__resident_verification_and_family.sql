ALTER TABLE pending_signups ADD COLUMN family_members_json TEXT;

CREATE TABLE approved_residents (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flat_number TEXT NOT NULL,
    name TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE family_members (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id),
    name TEXT NOT NULL,
    relation TEXT NOT NULL,
    age INTEGER,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_approved_residents_flat_number ON approved_residents(flat_number);
CREATE INDEX idx_family_members_user_id ON family_members(user_id);

-- A small demo roster so the pre-signup verification step has something to check against
-- without an admin UI to manage it yet — the committee adds real entries the same way
-- (a row in this table) until one exists.
INSERT INTO approved_residents (flat_number, name, created_at) VALUES
    ('A-101', 'Alex Sharma', CURRENT_TIMESTAMP),
    ('B-201', 'Priya Mehta', CURRENT_TIMESTAMP),
    ('C-301', 'Rohan Verma', CURRENT_TIMESTAMP);
