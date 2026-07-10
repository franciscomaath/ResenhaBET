-- V20: Tournament expansion for Bracket/LEAGUE_BRACKET support
-- Adds generation mode, third place match flag, and tournament group configuration

ALTER TABLE resenha.tournament ADD COLUMN generation_mode VARCHAR(10) NOT NULL DEFAULT 'MANUAL';
ALTER TABLE resenha.tournament ADD COLUMN has_third_place_match BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE resenha.tournament ADD COLUMN number_of_groups INTEGER NOT NULL DEFAULT 1;
ALTER TABLE resenha.tournament ADD COLUMN players_advancing_per_group INTEGER NOT NULL DEFAULT 2;

-- Add LEAGUE_BRACKET to format enum
ALTER TABLE resenha.tournament ADD CONSTRAINT chk_tournament_format
    CHECK (format IN ('LEAGUE', 'BRACKET', 'LEAGUE_BRACKET'));

-- Add GENERATION_MODE enum constraint
ALTER TABLE resenha.tournament ADD CONSTRAINT chk_tournament_generation_mode
    CHECK (generation_mode IN ('AUTO', 'MANUAL'));