-- Masters: the reference data admins configure before any financial activity happens —
-- the society's own record, its buildings, the flats within them, the expense categories
-- used to classify spend, and the vendors paid for outside work.
CREATE TABLE society (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    address TEXT NOT NULL,
    contact_number TEXT NOT NULL,
    email TEXT NOT NULL
);

CREATE TABLE buildings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    total_flats INTEGER NOT NULL
);

CREATE TABLE flats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    building_id INTEGER NOT NULL REFERENCES buildings(id),
    flat_number TEXT NOT NULL,
    flat_type TEXT NOT NULL,
    occupied INTEGER NOT NULL DEFAULT 0,
    ownership_type TEXT NOT NULL DEFAULT 'owner'
);

CREATE TABLE expense_categories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    gst_slab_percent INTEGER NOT NULL
);

CREATE TABLE vendors (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    contact_person_name TEXT NOT NULL,
    contact_number TEXT NOT NULL,
    address TEXT,
    gst_number TEXT,
    opening_balance_paise INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_flats_building_id ON flats(building_id);
