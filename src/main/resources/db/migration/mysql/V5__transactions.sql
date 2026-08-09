-- Transactions: the financial activity that runs against Masters — recurring per-flat-type
-- rates and billing settings, one-off discounts and special contributions, vendor bills and
-- what's been paid against them, and resident maintenance payments.
CREATE TABLE maintenance_rates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flat_type VARCHAR(50) NOT NULL UNIQUE,
    monthly_amount_paise BIGINT NOT NULL DEFAULT 0
) ENGINE=InnoDB;

CREATE TABLE maintenance_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    due_day INT NOT NULL DEFAULT 10,
    late_fee_percent INT NOT NULL DEFAULT 0,
    opening_balance_note VARCHAR(2000) NOT NULL DEFAULT ''
) ENGINE=InnoDB;

CREATE TABLE maintenance_discounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    value BIGINT NOT NULL,
    description VARCHAR(2000),
    active BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB;

CREATE TABLE special_contributions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    amount_paise BIGINT NOT NULL,
    due_date VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE vendor_bills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_id BIGINT NOT NULL,
    expense_category_id BIGINT NOT NULL,
    bill_number VARCHAR(255) NOT NULL,
    bill_date VARCHAR(255) NOT NULL,
    amount_paise BIGINT NOT NULL,
    description VARCHAR(2000),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_vendor_bills_vendor FOREIGN KEY (vendor_id) REFERENCES vendors (id),
    CONSTRAINT fk_vendor_bills_expense_category FOREIGN KEY (expense_category_id) REFERENCES expense_categories (id)
) ENGINE=InnoDB;

CREATE TABLE bill_payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    vendor_bill_id BIGINT NOT NULL,
    amount_paise BIGINT NOT NULL,
    payment_date VARCHAR(255) NOT NULL,
    payment_mode VARCHAR(50) NOT NULL,
    reference_number VARCHAR(255),
    notes VARCHAR(2000),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_bill_payments_vendor_bill FOREIGN KEY (vendor_bill_id) REFERENCES vendor_bills (id)
) ENGINE=InnoDB;

CREATE TABLE maintenance_collections (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    flat_id BIGINT NOT NULL,
    payer_name VARCHAR(255) NOT NULL,
    amount_paise BIGINT NOT NULL,
    payment_date VARCHAR(255) NOT NULL,
    payment_mode VARCHAR(50) NOT NULL,
    for_month VARCHAR(255) NOT NULL,
    reference_number VARCHAR(255),
    notes VARCHAR(2000),
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_maintenance_collections_flat FOREIGN KEY (flat_id) REFERENCES flats (id)
) ENGINE=InnoDB;

CREATE INDEX idx_vendor_bills_vendor_id ON vendor_bills (vendor_id);
CREATE INDEX idx_vendor_bills_expense_category_id ON vendor_bills (expense_category_id);
CREATE INDEX idx_vendor_bills_bill_date ON vendor_bills (bill_date);
CREATE INDEX idx_bill_payments_vendor_bill_id ON bill_payments (vendor_bill_id);
CREATE INDEX idx_maintenance_collections_flat_id ON maintenance_collections (flat_id);
CREATE INDEX idx_maintenance_collections_for_month ON maintenance_collections (for_month);
