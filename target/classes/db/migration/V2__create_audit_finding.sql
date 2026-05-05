-- =========================
-- AUDIT_FINDING TABLE (core business entity)
-- =========================
CREATE TABLE audit_finding (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    severity VARCHAR(50),
    status VARCHAR(50) DEFAULT 'OPEN',
    due_date DATE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================
-- INDEXES for audit_finding
-- =========================
CREATE INDEX idx_audit_finding_status ON audit_finding(status);
CREATE INDEX idx_audit_finding_severity ON audit_finding(severity);
CREATE INDEX idx_audit_finding_due_date ON audit_finding(due_date);
CREATE INDEX idx_audit_finding_deleted_at ON audit_finding(deleted_at);
CREATE INDEX idx_audit_finding_created_at ON audit_finding(created_at);