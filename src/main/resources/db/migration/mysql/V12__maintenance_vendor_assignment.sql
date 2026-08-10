ALTER TABLE vendors ADD COLUMN category VARCHAR(50);
ALTER TABLE maintenance_requests ADD COLUMN vendor_id BIGINT;
ALTER TABLE maintenance_requests ADD COLUMN vendor_name VARCHAR(255);
