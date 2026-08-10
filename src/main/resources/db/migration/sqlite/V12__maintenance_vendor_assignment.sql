ALTER TABLE vendors ADD COLUMN category TEXT;
ALTER TABLE maintenance_requests ADD COLUMN vendor_id INTEGER;
ALTER TABLE maintenance_requests ADD COLUMN vendor_name TEXT;
