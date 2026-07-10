ALTER TABLE resenha.market
    ADD COLUMN market_type VARCHAR(30) NOT NULL DEFAULT 'MATCH_RESULT';

UPDATE resenha.market SET market_type = 'MATCH_RESULT';

ALTER TABLE resenha.market
    DROP CONSTRAINT IF EXISTS market_event_id_key;

ALTER TABLE resenha.market
    ADD CONSTRAINT market_event_market_type_key UNIQUE (event_id, market_type);
