CREATE TABLE groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE group_member (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_member_group FOREIGN KEY (group_id) REFERENCES groups(id),
    CONSTRAINT fk_group_member_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_group_member_group_user UNIQUE (group_id, user_id)
);

CREATE TABLE group_tournament (
    id BIGSERIAL PRIMARY KEY,
    group_id BIGINT NOT NULL,
    tournament_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_tournament_group FOREIGN KEY (group_id) REFERENCES groups(id),
    CONSTRAINT fk_group_tournament_tournament FOREIGN KEY (tournament_id) REFERENCES tournament(id),
    CONSTRAINT uk_group_tournament_group_tournament UNIQUE (group_id, tournament_id)
);

CREATE TABLE tournament_wallet (
    id BIGSERIAL PRIMARY KEY,
    group_tournament_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.0,
    initial_balance DECIMAL(19, 2) NOT NULL DEFAULT 0.0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tournament_wallet_group_tournament FOREIGN KEY (group_tournament_id) REFERENCES group_tournament(id),
    CONSTRAINT fk_tournament_wallet_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_tournament_wallet_group_tournament_user UNIQUE (group_tournament_id, user_id)
);

ALTER TABLE session ADD COLUMN current_group_id BIGINT;
ALTER TABLE session ADD CONSTRAINT fk_session_current_group FOREIGN KEY (current_group_id) REFERENCES groups(id);

ALTER TABLE player ADD COLUMN group_id BIGINT;
ALTER TABLE player ADD CONSTRAINT fk_player_group FOREIGN KEY (group_id) REFERENCES groups(id);
ALTER TABLE player DROP CONSTRAINT IF EXISTS player_user_id_key;
ALTER TABLE player ADD CONSTRAINT uk_player_group_user UNIQUE (group_id, user_id);

ALTER TABLE bet_slip ADD COLUMN group_tournament_id BIGINT;
ALTER TABLE bet_slip ADD CONSTRAINT fk_bet_slip_group_tournament FOREIGN KEY (group_tournament_id) REFERENCES group_tournament(id);
ALTER TABLE bet_slip ALTER COLUMN tournament_id DROP NOT NULL;

ALTER TABLE transaction ADD COLUMN tournament_wallet_id BIGINT;
ALTER TABLE transaction ADD CONSTRAINT fk_transaction_tournament_wallet FOREIGN KEY (tournament_wallet_id) REFERENCES tournament_wallet(id);
ALTER TABLE transaction ALTER COLUMN wallet_id DROP NOT NULL;
