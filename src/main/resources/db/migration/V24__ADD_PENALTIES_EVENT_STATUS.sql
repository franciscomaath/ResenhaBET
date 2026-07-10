-- V24: Add PENALTIES to EventStatus enum
-- New status for knockout matches that end in a draw after regular time

ALTER TABLE resenha.event ADD CONSTRAINT chk_event_status
    CHECK (status IN ('CREATED', 'IN_PROGRESS', 'PENALTIES', 'COMPLETED', 'CANCELLED'));