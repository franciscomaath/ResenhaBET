ALTER TABLE resenha.tournament
    ADD COLUMN competition_id BIGINT REFERENCES resenha.competition(id);

CREATE TABLE resenha.tournament_market_type (
    tournament_id BIGINT NOT NULL REFERENCES resenha.tournament(id),
    market_type   VARCHAR(50) NOT NULL,
    PRIMARY KEY (tournament_id, market_type)
);

INSERT INTO resenha.competition
    (uuid, name, season, api_football_league_id, api_football_country_id, game_forecast_league_id, start_date, end_date)
VALUES
    (gen_random_uuid(), 'Copa do Mundo da FIFA™ 2026', '2026', '28', '8', '149', '2026-06-11 00:00:00', '2026-07-19 23:59:00');
