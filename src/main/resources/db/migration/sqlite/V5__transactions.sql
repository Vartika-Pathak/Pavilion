-- Transactions: the financial activity that runs against Masters — recurring per-flat-type
-- rates and billing settings, one-off discounts and special contributions, vendor bills and
-- what's been paid against them, and resident maintenance payments.
CREATE TABLE maintenance_rates (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flat_type TEXT NOT NULL UNIQUE,
    monthly_amount_paise INTEGER NOT NULL DEFAULT 0
);

CREATE TABLE maintenance_settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    due_day INTEGER NOT NULL DEFAULT 10,
    late_fee_percent INTEGER NOT NULL DEFAULT 0,
    opening_balance_note TEXT NOT NULL DEFAULT ''
);

CREATE TABLE maintenance_discounts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    discount_type TEXT NOT NULL,
    value INTEGER NOT NULL,
    description TEXT,
    active INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE special_contributions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT,
    amount_paise INTEGER NOT NULL,
    due_date TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE vendor_bills (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vendor_id INTEGER NOT NULL REFERENCES vendors(id),
    expense_category_id INTEGER NOT NULL REFERENCES expense_categories(id),
    bill_number TEXT NOT NULL,
    bill_date TEXT NOT NULL,
    amount_paise INTEGER NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE bill_payments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    vendor_bill_id INTEGER NOT NULL REFERENCES vendor_bills(id),
    amount_paise INTEGER NOT NULL,
    payment_date TEXT NOT NULL,
    payment_mode TEXT NOT NULL,
    reference_number TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL
);

-- flats aren't linked to resident accounts (those live in this same service's users table via
-- login, but flats are configured independently by the admin), so this is admin bookkeeping
-- keyed by flat, with the payer's name typed in at entry time.
CREATE TABLE maintenance_collections (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    flat_id INTEGER NOT NULL REFERENCES flats(id),
    payer_name TEXT NOT NULL,
    amount_paise INTEGER NOT NULL,
    payment_date TEXT NOT NULL,
    payment_mode TEXT NOT NULL,
    for_month TEXT NOT NULL,
    reference_number TEXT,
    notes TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_vendor_bills_vendor_id ON vendor_bills(vendor_id);
CREATE INDEX idx_vendor_bills_expense_category_id ON vendor_bills(expense_category_id);
CREATE INDEX idx_vendor_bills_bill_date ON vendor_bills(bill_date);
CREATE INDEX idx_bill_payments_vendor_bill_id ON bill_payments(vendor_bill_id);
CREATE INDEX idx_maintenance_collections_flat_id ON maintenance_collections(flat_id);
CREATE INDEX idx_maintenance_collections_for_month ON maintenance_collections(for_month);
