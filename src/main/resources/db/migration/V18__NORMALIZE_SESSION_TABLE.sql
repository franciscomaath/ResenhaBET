CREATE TABLE IF NOT EXISTS session (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    token UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL
);

ALTER TABLE session
    ADD COLUMN IF NOT EXISTS user_id BIGINT,
    ADD COLUMN IF NOT EXISTS token UUID,
    ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uk_session_token ON session(token);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = current_schema()
          AND table_name = 'session'
          AND constraint_name IN ('fk_session_user', 'fk_user_id')
    ) THEN
        ALTER TABLE session
            ADD CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
