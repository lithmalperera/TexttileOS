-- Seeds one local administrator so the application has a bootstrap identity.
-- The account is activated with a documented local-only password. Production
-- deployments must replace or remove this seed before deployment.

INSERT INTO app_user (
    id,
    email,
    email_normalized,
    display_name,
    password_hash,
    status,
    version
) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    'admin@textile.test',
    'admin@textile.test',
    'Local Administrator',
    '$2y$10$Ik6hI9.1Hu7ZQKjFsaqwFuXoSGLNxsPL1b1ZGOk6vzK0ikSSanYcS',
    'ACTIVE',
    0
);

INSERT INTO user_role (user_id, role_id) VALUES (
    'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    '11111111-1111-1111-1111-111111111111'
);
