ALTER TABLE resenha.event
    ADD COLUMN team_home_id BIGINT REFERENCES resenha.team(id),
    ADD COLUMN team_away_id BIGINT REFERENCES resenha.team(id),
    ADD COLUMN external_match_id VARCHAR(20);
