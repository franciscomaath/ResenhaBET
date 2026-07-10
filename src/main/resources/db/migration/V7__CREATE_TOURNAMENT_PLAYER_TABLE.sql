CREATE TABLE tournament_player (
    tournament_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    PRIMARY KEY (tournament_id, player_id),
    CONSTRAINT fk_tp_tournament FOREIGN KEY (tournament_id) REFERENCES tournament(id), CONSTRAINT fk_tp_player FOREIGN KEY (player_id) REFERENCES player(id));
