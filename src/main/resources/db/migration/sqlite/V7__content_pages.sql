CREATE TABLE gallery_photos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    image_url TEXT NOT NULL,
    title TEXT,
    description TEXT,
    uploaded_by TEXT,
    uploaded_at TIMESTAMP NOT NULL
);

CREATE TABLE contact_messages (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    subject TEXT NOT NULL,
    message TEXT NOT NULL,
    submitted_at TIMESTAMP NOT NULL
);

CREATE TABLE news_posts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    excerpt TEXT,
    author TEXT NOT NULL,
    image_url TEXT,
    published_at TIMESTAMP NOT NULL
);

-- Kept separate from events (rather than an event "category") since meetings only need to show
-- when and where, not the richer festival/celebration presentation the Events page uses.
CREATE TABLE resident_meetings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    meeting_date TIMESTAMP NOT NULL,
    location TEXT NOT NULL,
    notes TEXT
);

-- A public directory/profile listing, distinct from the login "users" table — no write API
-- existed in the Node version either, so this is read-only here too.
CREATE TABLE members (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    flat_number TEXT NOT NULL,
    bio TEXT,
    avatar_url TEXT,
    joined_at TIMESTAMP NOT NULL
);

CREATE TABLE join_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    email TEXT NOT NULL,
    flat_number TEXT NOT NULL,
    message TEXT,
    status TEXT NOT NULL DEFAULT 'pending',
    submitted_at TIMESTAMP NOT NULL
);

-- resident_id is not a foreign key to users(id) on purpose: it's just a plain reference kept
-- for ownership filtering, denormalized alongside resident_name/resident_flat_number the same
-- way the rest of this table works, rather than joined at read time.
CREATE TABLE complaints (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resident_id INTEGER NOT NULL,
    resident_name TEXT NOT NULL,
    resident_flat_number TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'open',
    resolution_note TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE maintenance_requests (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    resident_id INTEGER NOT NULL,
    resident_name TEXT NOT NULL,
    resident_flat_number TEXT NOT NULL,
    category TEXT NOT NULL,
    description TEXT NOT NULL,
    photo_urls TEXT NOT NULL DEFAULT '[]',
    status TEXT NOT NULL DEFAULT 'open',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_resident_meetings_date ON resident_meetings(meeting_date);
CREATE INDEX idx_complaints_resident_id ON complaints(resident_id);
CREATE INDEX idx_maintenance_requests_resident_id ON maintenance_requests(resident_id);
