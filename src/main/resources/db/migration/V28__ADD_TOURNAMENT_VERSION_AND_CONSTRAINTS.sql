ALTER TABLE tournament ADD COLUMN version BIGINT DEFAULT 0;

ALTER TABLE tournament_player
ADD CONSTRAINT uk_tournament_player UNIQUE (tournament_id, player_id);

CREATE INDEX IF NOT EXISTS idx_event_tournament_status ON event(tournament_id, status);
CREATE INDEX IF NOT EXISTS idx_tournament_player_group ON tournament_player(tournament_id, group_number);
