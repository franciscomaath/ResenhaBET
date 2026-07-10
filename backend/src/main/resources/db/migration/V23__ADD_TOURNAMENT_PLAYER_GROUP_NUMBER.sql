-- V23: TournamentPlayer group number
-- Tracks which group a player belongs to in LEAGUE_BRACKET format

ALTER TABLE resenha.tournament_player ADD COLUMN group_number INTEGER NULL;