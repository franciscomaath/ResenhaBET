CREATE TABLE resenha.competition (
    id                          BIGSERIAL PRIMARY KEY,
    uuid                        UUID NOT NULL UNIQUE,
    name                        VARCHAR(255) NOT NULL,
    season                      VARCHAR(20) NOT NULL,
    api_football_league_id      VARCHAR(20) NOT NULL,
    api_football_country_id     VARCHAR(20) NOT NULL,
    game_forecast_league_id     VARCHAR(20) NOT NULL,
    start_date                  TIMESTAMP NOT NULL,
    end_date                    TIMESTAMP NOT NULL,
    active                      BOOLEAN NOT NULL DEFAULT true,
    created_at                  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_competition_external
        UNIQUE (api_football_league_id, api_football_country_id, season)
);
