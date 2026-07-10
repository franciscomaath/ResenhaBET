package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.event.MarketChangeEvent;
import com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.OutcomeResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.entity.Market;
import com.franciscomaath.resenhaapi.domain.entity.Outcome;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.MarketStatus;
import com.franciscomaath.resenhaapi.domain.enums.MarketType;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.MarketRepository;
import com.franciscomaath.resenhaapi.domain.repository.OutcomeRepository;
import com.franciscomaath.resenhaapi.mapper.MarketMapper;
import com.franciscomaath.resenhaapi.mapper.OutcomeMapper;
import com.franciscomaath.resenhaapi.service.MarketService;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MarketServiceImpl implements MarketService {

    private final MarketRepository marketRepository;
    private final OutcomeRepository outcomeRepository;
    private final MarketMapper marketMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final OutcomeMapper outcomeMapper;
    private final EventRepository eventRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final GroupTournamentRepository groupTournamentRepository;
    private final CurrentUserContext currentUserContext;

    @Override
    public List<MarketResponseDTO> findAllByEventId(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
        groupAuthorizationService.requireTournamentAccess(event.getTournament().getId());

        Long groupId = currentUserContext.getRequiredGroupId();
        GroupTournament groupTournament = groupTournamentRepository
                .findByTournamentIdAndGroupId(event.getTournament().getId(), groupId)
                .orElseThrow(() -> new ResourceNotFoundException("GroupTournament not found for tournament " + event.getTournament().getId()));

        Set<MarketType> marketTypes = groupTournament.getMarketTypes();

        List<Market> markets = marketRepository.findAllByEventIdAndMarketTypeIn(eventId, marketTypes);

        if (markets.isEmpty()) {
            return Collections.emptyList();
        }

        return markets.stream()
                .map(this::toResponseWithOutcomes)
                .toList();
    }

    @Override
    public MarketResponseDTO findByEventId(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id " + eventId));
        groupAuthorizationService.requireTournamentAccess(event.getTournament().getId());

        if (event.getPlayerHome() == null || event.getPlayerAway() == null) {
            return new MarketResponseDTO();
        }

        Market market = marketRepository.findByEventIdAndMarketType(eventId, MarketType.MATCH_RESULT)
                .orElseThrow(() -> new ResourceNotFoundException("Market not found for event id: " + eventId));

        return toResponseWithOutcomes(market);
    }

    @Override
    @Transactional
    public void openMarket(Long eventId) {
        List<Market> markets = marketRepository.findAllByEventId(eventId);

        if (markets.isEmpty()) {
            throw new ResourceNotFoundException("Market not found for event id: " + eventId);
        }

        for (Market market : markets) {
            if (market.getEvent().getStatus() == EventStatus.COMPLETED) {
                throw new BusinessException("This event already ended.");
            }
            market.setStatus(MarketStatus.OPEN);
            marketRepository.save(market);
            publishMarketChange(market);
        }
    }

    @Override
    @Transactional
    public void closeMarket(Long eventId) {
        List<Market> markets = marketRepository.findAllByEventId(eventId);

        if (markets.isEmpty()) {
            throw new ResourceNotFoundException("Market not found for event id: " + eventId);
        }

        for (Market market : markets) {
            market.setStatus(MarketStatus.CLOSED);
            marketRepository.save(market);
            publishMarketChange(market);
        }
    }

    @Override
    @Transactional
    public void closeAllMarkets(Long eventId) {
        closeMarket(eventId);
    }

    @Override
    @Transactional
    public void cancelMarket(Long eventId) {
        List<Market> markets = marketRepository.findAllByEventId(eventId);

        if (markets.isEmpty()) {
            throw new ResourceNotFoundException("Market not found for event id: " + eventId);
        }

        for (Market market : markets) {
            market.setStatus(MarketStatus.CANCELLED);
            marketRepository.save(market);
            publishMarketChange(market);
        }
    }

    private MarketResponseDTO toResponseWithOutcomes(Market market) {
        List<Outcome> outcomes = outcomeRepository.findByMarketId(market.getId());
        MarketResponseDTO response = marketMapper.toResponse(market);
        List<OutcomeResponseDTO> outcomeDTOs = outcomes.stream()
                .map(marketMapper::toOutcomeResponse)
                .toList();
        response.setOutcomes(outcomeDTOs);
        return response;
    }

    private void publishMarketChange(Market market) {
        List<Outcome> outcomes = outcomeRepository.findByMarketId(market.getId());
        MarketResponseDTO dto = marketMapper.toResponse(market);
        dto.setOutcomes(outcomes.stream().map(outcomeMapper::toResponse).toList());
        eventPublisher.publishEvent(new MarketChangeEvent(this, market.getEvent().getId(), dto));
    }
}
