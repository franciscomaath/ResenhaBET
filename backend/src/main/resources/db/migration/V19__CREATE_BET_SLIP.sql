DROP TABLE IF EXISTS bet CASCADE;

CREATE TABLE bet_slip (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tournament_id BIGINT NOT NULL,
    stake DECIMAL(19, 2) NOT NULL,
    combined_odd DECIMAL(10, 2) NOT NULL,
    potential_return DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bet_slip_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_bet_slip_tournament FOREIGN KEY (tournament_id) REFERENCES tournament(id)
);

CREATE TABLE bet_slip_item (
    id BIGSERIAL PRIMARY KEY,
    bet_slip_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    outcome_id BIGINT NOT NULL,
    odd_snapshot DECIMAL(10, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_bet_slip_item_slip FOREIGN KEY (bet_slip_id) REFERENCES bet_slip(id),
    CONSTRAINT fk_bet_slip_item_event FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT fk_bet_slip_item_outcome FOREIGN KEY (outcome_id) REFERENCES outcome(id)
);

ALTER TABLE transaction ADD COLUMN IF NOT EXISTS bet_slip_id BIGINT;
ALTER TABLE transaction ADD CONSTRAINT fk_transaction_bet_slip FOREIGN KEY (bet_slip_id) REFERENCES bet_slip(id);
