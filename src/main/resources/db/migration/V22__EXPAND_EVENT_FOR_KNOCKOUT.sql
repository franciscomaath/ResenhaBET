-- V22: Event expansion for knockout support
-- Adds penalties scores and next round event linkage

ALTER TABLE resenha.event ADD COLUMN penalties_home INTEGER NULL;
ALTER TABLE resenha.event ADD COLUMN penalties_away INTEGER NULL;
ALTER TABLE resenha.event ADD COLUMN next_round_event_id BIGINT NULL;
ALTER TABLE resenha.event ADD COLUMN home_source_event_id BIGINT NULL;
ALTER TABLE resenha.event ADD COLUMN away_source_event_id BIGINT NULL;

-- Add self-referential FK for bracket advancement
ALTER TABLE resenha.event ADD CONSTRAINT fk_event_next_round
    FOREIGN KEY (next_round_event_id) REFERENCES resenha.event(id);

ALTER TABLE resenha.event ADD CONSTRAINT fk_event_home_source
    FOREIGN KEY (home_source_event_id) REFERENCES resenha.event(id);

ALTER TABLE resenha.event ADD CONSTRAINT fk_event_away_source
    FOREIGN KEY (away_source_event_id) REFERENCES resenha.event(id);
