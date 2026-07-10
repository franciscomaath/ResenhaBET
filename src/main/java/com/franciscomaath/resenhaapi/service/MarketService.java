package com.franciscomaath.resenhaapi.service;

import com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Market;

import java.util.List;

public interface MarketService {
    List<MarketResponseDTO> findAllByEventId(Long eventId);

    MarketResponseDTO findByEventId(Long eventId);

    void openMarket(Long eventId);

    void closeMarket(Long eventId);

    void closeAllMarkets(Long eventId);

    void cancelMarket(Long eventId);
}
