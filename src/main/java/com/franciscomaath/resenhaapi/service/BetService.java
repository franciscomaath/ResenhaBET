package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.request.BetRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.BetSlipResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Event;

import java.util.List;

public interface BetService {

    BetSlipResponseDTO placeBet(BetRequestDTO dto);

    List<BetSlipResponseDTO> getUserBets();

    List<BetSlipResponseDTO> getBetsByEvent(Long eventId);

    void resolveBetsForEvent(Long eventId);

    void cancelBetsForEvent(Long eventId);

    void reopenBetsForEvent(Long eventId);
}
