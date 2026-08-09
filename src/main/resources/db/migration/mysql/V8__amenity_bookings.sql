CREATE TABLE amenity_bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resident_id BIGINT NOT NULL,
    amenity_id VARCHAR(255) NOT NULL,
    booking_date VARCHAR(10) NOT NULL,
    slot VARCHAR(50) NOT NULL,
    amount_paid_cents INT NOT NULL DEFAULT 0,
    stripe_session_id VARCHAR(255),
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE INDEX idx_amenity_bookings_resident_id ON amenity_bookings (resident_id);
CREATE INDEX idx_amenity_bookings_slot_lookup ON amenity_bookings (amenity_id, booking_date, slot);
CREATE INDEX idx_amenity_bookings_stripe_session_id ON amenity_bookings (stripe_session_id);
