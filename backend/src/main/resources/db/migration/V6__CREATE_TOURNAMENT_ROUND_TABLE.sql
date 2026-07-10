CREATE TABLE tournament_round (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    multiplier DECIMAL(10, 4) NOT NULL,
    round_order INT NOT NULL,
    CONSTRAINT fk_round_tournament FOREIGN KEY (tournament_id) REFERENCES tournament(id));
