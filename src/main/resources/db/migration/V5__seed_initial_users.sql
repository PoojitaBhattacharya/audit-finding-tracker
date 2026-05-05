-- =========================
-- SEED INITIAL USERS WITH ROLES
-- =========================

-- Delete existing test users (for idempotency)
DELETE FROM users WHERE username IN ('admin', 'manager', 'viewer');

-- Insert ADMIN user (password: admin123 - hashed with BCrypt)
INSERT INTO users (username, email, password, role, status, created_at, updated_at)
VALUES ('admin', 'admin@example.com', '$2a$10$slYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy990qu', 'ADMIN', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert MANAGER user (password: manager123 - hashed with BCrypt)
INSERT INTO users (username, email, password, role, status, created_at, updated_at)
VALUES ('manager', 'manager@example.com', '$2a$10$slYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy990qu', 'MANAGER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert VIEWER user (password: viewer123 - hashed with BCrypt)
INSERT INTO users (username, email, password, role, status, created_at, updated_at)
VALUES ('viewer', 'viewer@example.com', '$2a$10$slYQmyNdGzin7olVN3p5Be7DlH.PKZbv5H8KnzzVgXXbVxzy990qu', 'VIEWER', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
