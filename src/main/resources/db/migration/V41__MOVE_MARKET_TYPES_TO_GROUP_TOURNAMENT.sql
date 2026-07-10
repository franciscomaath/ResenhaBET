-- Drop old tournament-scoped market types table (start fresh, system not in production)
DROP TABLE IF EXISTS resenha.tournament_market_type;

-- Create group-scoped market types table
CREATE TABLE resenha.group_tournament_market_type (
    group_tournament_id BIGINT      NOT NULL REFERENCES resenha.group_tournament(id),
    market_type         VARCHAR(50) NOT NULL,
    PRIMARY KEY (group_tournament_id, market_type)
);
