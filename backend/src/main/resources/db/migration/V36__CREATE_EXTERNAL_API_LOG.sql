-- V36__CREATE_EXTERNAL_API_LOG_TABLE.sql
CREATE TABLE resenha.external_api_log (
      id              BIGSERIAL PRIMARY KEY,
      provider        VARCHAR(30) NOT NULL,      -- 'API_FOOTBALL' | 'GAME_FORECAST'
      endpoint        VARCHAR(100) NOT NULL,     -- 'get_events', 'get_standings', '/events'
      request_key     VARCHAR(255) NOT NULL,     -- chave determinística dos params da chamada
      response_body   JSONB NOT NULL,
      status_code     INT,
      fetched_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_external_api_log_lookup
    ON resenha.external_api_log (provider, endpoint, request_key, fetched_at DESC);