-- Identity module: fixed roles, internal users, and user-role assignments.
-- Reference roles are seeded here because they are fixed MVP data (docs/08); the
-- application never creates or deletes roles.

CREATE TABLE role (
    id         UUID PRIMARY KEY,
    code       VARCHAR(50) NOT NULL UNIQUE,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE app_user (
    id               UUID PRIMARY KEY,
    email            VARCHAR(255) NOT NULL,
    email_normalized VARCHAR(255) NOT NULL UNIQUE,
    display_name     VARCHAR(100) NOT NULL,
    password_hash    VARCHAR(255) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    version          BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT app_user_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE user_role (
    user_id UUID NOT NULL REFERENCES app_user (id),
    role_id UUID NOT NULL REFERENCES role (id),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_role_role_id ON user_role (role_id);

INSERT INTO role (id, code, name) VALUES
    ('11111111-1111-1111-1111-111111111111', 'ADMIN',              'Administrator'),
    ('22222222-2222-2222-2222-222222222222', 'PLANNER',            'Planner'),
    ('33333333-3333-3333-3333-333333333333', 'INVENTORY_MANAGER',  'Inventory Manager'),
    ('44444444-4444-4444-4444-444444444444', 'PRODUCTION_OPERATOR','Production Operator'),
    ('55555555-5555-5555-5555-555555555555', 'QUALITY_INSPECTOR',  'Quality Inspector');
