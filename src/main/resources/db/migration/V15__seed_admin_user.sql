-- Seed a default administrator account for local/bootstrap access.
-- Login: admin@mentorai.local
-- Initial password: Admin@123456
INSERT INTO users (
    full_name,
    email,
    password_hash,
    role,
    enabled,
    created_at,
    updated_at
)
VALUES (
    'Administrator',
    'admin@mentorai.local',
    '$2a$10$DCLXdPRX1bw85U0SU/yRLe84hnb0J2TdEKATRckNwOOr5ZQi0fkQy',
    'ADMIN',
    TRUE,
    now(),
    now()
)
ON CONFLICT (email) DO UPDATE
SET role = 'ADMIN',
    enabled = TRUE,
    updated_at = now();
