-- V21: TournamentRound phase type
-- Distinguishes GROUP_STAGE rounds from KNOCKOUT rounds

ALTER TABLE resenha.tournament_round ADD COLUMN phase_type VARCHAR(15) NOT NULL DEFAULT 'GROUP_STAGE';

-- Add PHASE_TYPE enum constraint
ALTER TABLE resenha.tournament_round ADD CONSTRAINT chk_tournament_round_phase_type
    CHECK (phase_type IN ('GROUP_STAGE', 'KNOCKOUT'));