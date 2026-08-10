CREATE TABLE parking_passes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flat_number TEXT NOT NULL UNIQUE,
    purchased_by_resident_id INTEGER NOT NULL,
    purchased_by_name TEXT NOT NULL,
    amount_paid_cents INTEGER NOT NULL,
    stripe_session_id TEXT,
    created_at TIMESTAMP NOT NULL
);
