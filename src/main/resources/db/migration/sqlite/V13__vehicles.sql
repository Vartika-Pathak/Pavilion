CREATE TABLE vehicles (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resident_id INTEGER NOT NULL,
    plate_number TEXT NOT NULL,
    vehicle_type TEXT NOT NULL,
    owner_name TEXT NOT NULL,
    flat_number TEXT NOT NULL,
    owner_phone TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
