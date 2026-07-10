-- V25: Make player_home_id, player_away_id, and game_datetime nullable on event table
-- Required for BRACKET MANUAL mode where empty event slots are created before players are assigned
-- game_datetime is set when the event is actually scheduled

ALTER TABLE resenha.event ALTER COLUMN player_home_id DROP NOT NULL;
ALTER TABLE resenha.event ALTER COLUMN player_away_id DROP NOT NULL;
ALTER TABLE resenha.event ALTER COLUMN game_datetime DROP NOT NULL;