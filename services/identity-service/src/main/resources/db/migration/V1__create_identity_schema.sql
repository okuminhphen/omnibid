CREATE TABLE users (
    id UUID PRIMARY KEY,
    primary_email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('ACTIVE', 'BLOCKED', 'DELETED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_login_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_users_primary_email_ci ON users (lower(primary_email));

CREATE TABLE user_identities (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    provider VARCHAR(30) NOT NULL
        CHECK (provider IN ('GOOGLE', 'LOCAL_DEV')),
    provider_subject VARCHAR(255) NOT NULL,
    provider_email VARCHAR(320),
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_used_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_user_identity_provider_subject UNIQUE (provider, provider_subject)
);

CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    display_name VARCHAR(120) NOT NULL,
    avatar_url VARCHAR(1000),
    phone_number VARCHAR(30),
    locale VARCHAR(20) NOT NULL DEFAULT 'vi-VN',
    timezone VARCHAR(50) NOT NULL DEFAULT 'Asia/Ho_Chi_Minh',
    bio VARCHAR(500),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE roles (
    id SMALLINT PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE
        CHECK (code IN ('CUSTOMER', 'ADMIN'))
);

INSERT INTO roles(id, code) VALUES (1, 'CUSTOMER'), (2, 'ADMIN');

CREATE TABLE user_roles (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id SMALLINT NOT NULL REFERENCES roles(id),
    granted_by_user_id UUID REFERENCES users(id),
    granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE auth_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_family_id UUID NOT NULL,
    refresh_token_hash CHAR(64) NOT NULL UNIQUE,
    user_agent VARCHAR(500),
    ip_hash CHAR(64),
    status VARCHAR(20) NOT NULL
        CHECK (status IN ('ACTIVE', 'ROTATED', 'REVOKED', 'EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    replaced_by_session_id UUID REFERENCES auth_sessions(id)
);

CREATE INDEX idx_auth_sessions_user_status ON auth_sessions (user_id, status);
CREATE INDEX idx_auth_sessions_family ON auth_sessions (token_family_id);

CREATE TABLE auth_events (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    event_type VARCHAR(50) NOT NULL,
    result VARCHAR(20) NOT NULL,
    ip_hash CHAR(64),
    user_agent VARCHAR(500),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE identity_outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(150) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ
);

CREATE INDEX idx_identity_outbox_unpublished
    ON identity_outbox_events (created_at)
    WHERE published_at IS NULL;
