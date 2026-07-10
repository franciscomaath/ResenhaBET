package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.response.OddsImportResult;
import lombok.Getter;
import lombok.Setter;

public interface OddsImportService {
    OddsImportResult importForTournament(Long tournamentId);
}
