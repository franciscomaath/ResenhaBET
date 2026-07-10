package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.response.SyncResult;

import java.time.LocalDate;

public interface FixtureSyncService {
    SyncResult sync(Long tournamentId);
}
