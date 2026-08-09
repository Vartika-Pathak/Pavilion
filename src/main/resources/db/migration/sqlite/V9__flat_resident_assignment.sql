ALTER TABLE flats ADD COLUMN resident_id INTEGER;

CREATE INDEX idx_flats_resident_id ON flats(resident_id);

-- A resident asking the admin to correct their flat's details, rather than editing it directly.
CREATE TABLE flat_change_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flat_id INTEGER NOT NULL,
    resident_id INTEGER NOT NULL,
    resident_name TEXT NOT NULL,
    message TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_flat_change_requests_flat_id ON flat_change_requests(flat_id);
