-- Replaced by an admin-reviewed request queue: residents submit a request instead of being
-- auto-checked against a static allowlist, so the old table is no longer needed.
DROP TABLE approved_residents;

CREATE TABLE resident_verification_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flat_number TEXT NOT NULL,
    name TEXT NOT NULL,
    documents_verified INTEGER NOT NULL DEFAULT 0,
    payment_received INTEGER NOT NULL DEFAULT 0,
    status TEXT NOT NULL DEFAULT 'pending',
    reviewed_by INTEGER REFERENCES users(id),
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_verification_requests_flat_name ON resident_verification_requests(flat_number, name);
CREATE INDEX idx_verification_requests_status ON resident_verification_requests(status);
