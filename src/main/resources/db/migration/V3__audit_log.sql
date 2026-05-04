CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,

    entity_type VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,

    action VARCHAR(50),
    old_value TEXT,
    new_value TEXT,

    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Composite index for fast lookup by entity
CREATE INDEX idx_audit_entity 
ON audit_log(entity_type, entity_id);

-- Index for filtering by time (reports, history)
CREATE INDEX idx_audit_changed_at 
ON audit_log(changed_at);