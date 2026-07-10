CREATE UNIQUE INDEX event_tournament_external_match_unique
    ON resenha.event (tournament_id, external_match_id)
    WHERE external_match_id IS NOT NULL;
