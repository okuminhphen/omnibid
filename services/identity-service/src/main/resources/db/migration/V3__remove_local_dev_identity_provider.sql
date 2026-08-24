CREATE TEMPORARY TABLE legacy_local_dev_users ON COMMIT DROP AS
SELECT DISTINCT u.id
FROM users u
JOIN user_identities i ON i.user_id = u.id
WHERE i.provider = 'LOCAL_DEV';

DELETE FROM identity_outbox_events
WHERE aggregate_type = 'UserAccount'
  AND aggregate_id IN (SELECT id FROM legacy_local_dev_users);

DELETE FROM user_roles
WHERE user_id IN (SELECT id FROM legacy_local_dev_users);

DELETE FROM users
WHERE id IN (SELECT id FROM legacy_local_dev_users);

ALTER TABLE user_identities
    DROP CONSTRAINT IF EXISTS user_identities_provider_check;

ALTER TABLE user_identities
    ADD CONSTRAINT user_identities_provider_check
    CHECK (provider = 'GOOGLE');
