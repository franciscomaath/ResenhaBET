CREATE TABLE bet (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    event_id BIGINT NOT NULL,
    outcome_id BIGINT NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    odd_snapshot DECIMAL(10, 2) NOT NULL,
    potential_return DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bet_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_bet_event FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT fk_bet_outcome FOREIGN KEY (outcome_id) REFERENCES outcome(id));
