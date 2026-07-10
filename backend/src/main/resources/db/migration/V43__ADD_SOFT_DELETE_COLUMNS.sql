-- P0 entities
ALTER TABLE tournament ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE event ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE player ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE team ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE groups ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE group_member ADD COLUMN deleted_at TIMESTAMP NULL;
ALTER TABLE competition ADD COLUMN deleted_at TIMESTAMP NULL;
-- GroupTournament (needed for REAL_FOOTBALL shared tournament logic)
ALTER TABLE group_tournament ADD COLUMN deleted_at TIMESTAMP NULL;

-- Indexes for query performance
CREATE INDEX idx_tournament_deleted_at ON tournament(deleted_at);
CREATE INDEX idx_event_deleted_at ON event(deleted_at);
CREATE INDEX idx_player_deleted_at ON player(deleted_at);
CREATE INDEX idx_team_deleted_at ON team(deleted_at);
CREATE INDEX idx_group_deleted_at ON groups(deleted_at);
CREATE INDEX idx_group_member_deleted_at ON group_member(deleted_at);
CREATE INDEX idx_competition_deleted_at ON competition(deleted_at);
CREATE INDEX idx_group_tournament_deleted_at ON group_tournament(deleted_at);
