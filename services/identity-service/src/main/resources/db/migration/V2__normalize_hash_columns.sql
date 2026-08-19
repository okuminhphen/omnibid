-- PostgreSQL reports CHAR as bpchar while Hibernate maps Java String to VARCHAR.
-- Hashes have an invariant application-level length, so VARCHAR(64) avoids
-- padding semantics and lets schema validation remain portable.
ALTER TABLE auth_sessions
    ALTER COLUMN refresh_token_hash TYPE VARCHAR(64),
    ALTER COLUMN ip_hash TYPE VARCHAR(64);

ALTER TABLE auth_events
    ALTER COLUMN ip_hash TYPE VARCHAR(64);
