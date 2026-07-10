CREATE TABLE outcome (
    id BIGSERIAL PRIMARY KEY,
    market_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    odd DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_outcome_market FOREIGN KEY (market_id) REFERENCES market(id));
