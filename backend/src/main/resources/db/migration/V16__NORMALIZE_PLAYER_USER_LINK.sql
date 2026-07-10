ALTER TABLE player
    ADD COLUMN IF NOT EXISTS user_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_player_user_id ON player(user_id);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.table_constraints
        WHERE constraint_schema = current_schema()
          AND table_name = 'player'
          AND constraint_name = 'fk_player_user'
    ) THEN
        ALTER TABLE player
            ADD CONSTRAINT fk_player_user FOREIGN KEY (user_id) REFERENCES users(id);
    END IF;
END $$;
