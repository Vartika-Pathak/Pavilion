CREATE TABLE notices (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    category TEXT NOT NULL DEFAULT 'general',
    priority TEXT NOT NULL DEFAULT 'normal',
    pinned INTEGER NOT NULL DEFAULT 0,
    expires_at TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE society_rules (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    active INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL
);

-- A directory of local services (plumber, electrician, milkman, etc.) residents can call
-- directly — not tied to a vendor bill/contract, just contact info to look up.
CREATE TABLE services (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    category TEXT NOT NULL,
    contact_number TEXT NOT NULL,
    notes TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE app_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT,
    event_date TIMESTAMP NOT NULL,
    location TEXT NOT NULL,
    organizer TEXT,
    created_at TIMESTAMP NOT NULL
);

-- One row per successful admin write (create/update/delete) across the app — populated by
-- a filter, not by each controller, so new admin resources get audited automatically.
CREATE TABLE audit_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    admin_id INTEGER NOT NULL,
    admin_name TEXT NOT NULL,
    method TEXT NOT NULL,
    path TEXT NOT NULL,
    status_code INTEGER NOT NULL,
    summary TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_app_events_event_date ON app_events(event_date);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
