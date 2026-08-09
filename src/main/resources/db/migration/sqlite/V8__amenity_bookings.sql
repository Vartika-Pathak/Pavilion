-- A booking row only exists once it's actually confirmed — free amenities insert immediately,
-- paid ones insert only after Stripe confirms payment. There is no "pending" status to manage.
-- resident_id is not a foreign key, matching the same denormalized-ownership pattern used by
-- complaints/maintenance_requests. The amenity catalog itself (id/name/price) is a static list in
-- code, not a table, so amenity_id is just a plain string key into it.
CREATE TABLE amenity_bookings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resident_id INTEGER NOT NULL,
    amenity_id TEXT NOT NULL,
    booking_date TEXT NOT NULL,
    slot TEXT NOT NULL,
    amount_paid_cents INTEGER NOT NULL DEFAULT 0,
    stripe_session_id TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_amenity_bookings_resident_id ON amenity_bookings(resident_id);
CREATE INDEX idx_amenity_bookings_slot_lookup ON amenity_bookings(amenity_id, booking_date, slot);
CREATE INDEX idx_amenity_bookings_stripe_session_id ON amenity_bookings(stripe_session_id);
