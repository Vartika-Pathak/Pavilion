ALTER TABLE maintenance_collections ADD COLUMN stripe_session_id VARCHAR(255);

CREATE INDEX idx_maintenance_collections_stripe_session_id ON maintenance_collections (stripe_session_id);
