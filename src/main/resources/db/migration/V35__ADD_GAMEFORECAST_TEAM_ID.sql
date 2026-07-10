ALTER TABLE resenha.team
    DROP COLUMN external_api_id,
    ADD COLUMN api_football_team_id VARCHAR(20) UNIQUE,
    ADD COLUMN game_forecast_team_id VARCHAR(20) UNIQUE;
