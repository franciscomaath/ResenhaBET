package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.OddsProperties;
import com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.OutcomeResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.entity.Market;
import com.franciscomaath.resenhaapi.domain.entity.Outcome;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.MarketStatus;
import com.franciscomaath.resenhaapi.domain.enums.MarketType;
import com.franciscomaath.resenhaapi.domain.event.MarketChangeEvent;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.MarketRepository;
import com.franciscomaath.resenhaapi.domain.repository.OutcomeRepository;
import com.franciscomaath.resenhaapi.mapper.MarketMapper;
import com.franciscomaath.resenhaapi.mapper.OutcomeMapper;
import com.franciscomaath.resenhaapi.domain.utils.H2HUtil;
import com.franciscomaath.resenhaapi.service.OddsCalculatorService;
import com.franciscomaath.resenhaapi.service.OddsUpdateService;
import com.franciscomaath.resenhaapi.service.dto.H2HRecord;
import com.franciscomaath.resenhaapi.service.dto.OddsResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OddsUpdateServiceImpl implements OddsUpdateService {

    private final EventRepository eventRepository;
    private final MarketRepository marketRepository;
    private final OutcomeRepository outcomeRepository;
    private final OddsCalculatorService oddsCalculatorService;
    private final OddsProperties oddsProperties;
    private final ApplicationEventPublisher eventPublisher;
    private final MarketMapper marketMapper;
    private final OutcomeMapper outcomeMapper;

    @Override
    @Transactional
    public void recalculateFutureOdds(Long tournamentId) {
        List<Event> futureEvents = eventRepository.findFutureEventsWithPlayers(tournamentId);
        if (futureEvents.isEmpty()) {
            log.info("No future events to recalculate odds for tournament {}", tournamentId);
            return;
        }

        List<Long> eventIds = futureEvents.stream().map(Event::getId).toList();
        List<Market> markets = marketRepository.findByEventIdIn(eventIds).stream()
                .filter(market -> market.getMarketType() == MarketType.MATCH_RESULT)
                .toList();
        if (markets.isEmpty()) {
            return;
        }

        List<Long> marketIds = markets.stream().map(Market::getId).toList();
        List<Outcome> allOutcomes = outcomeRepository.findByMarketIdIn(marketIds);

        Map<Long, Market> marketByEventId = markets.stream()
                .collect(Collectors.toMap(m -> m.getEvent().getId(), m -> m));
        Map<Long, List<Outcome>> outcomesByMarketId = allOutcomes.stream()
                .collect(Collectors.groupingBy(o -> o.getMarket().getId()));

        int updatedCount = 0;

        for (Event event : futureEvents) {
            Market market = marketByEventId.get(event.getId());
            if (market == null || market.getStatus() != MarketStatus.OPEN) {
                continue;
            }

            List<Outcome> outcomes = outcomesByMarketId.get(market.getId());
            if (outcomes == null || outcomes.isEmpty()) {
                continue;
            }

            Player home = event.getPlayerHome();
            Player away = event.getPlayerAway();

            List<Event> h2hEvents = eventRepository.findDirectConfrontations(
                    home.getId(), away.getId(), EventStatus.COMPLETED,
                    PageRequest.of(0, oddsProperties.getH2hMatchLimit()));
            H2HRecord h2hRecord = H2HUtil.buildH2HRecord(h2hEvents, home.getId(), away.getId());
            OddsResult odds = oddsCalculatorService.calculate(home.getCurrentElo(), away.getCurrentElo(), h2hRecord);
            if (odds == null) {
                odds = oddsCalculatorService.calculateNoDraw(home.getCurrentElo(), away.getCurrentElo(), h2hRecord);
            }

            String homeName = event.getHomeName();
            String awayName = event.getAwayName();

            for (Outcome outcome : outcomes) {
                if (outcome.getName().equals("Vitória Casa") || outcome.getName().equals(homeName)) {
                    outcome.setOdd(odds.getHomeOdd());
                } else if (outcome.getName().equals("Empate")) {
                    outcome.setOdd(odds.getDrawOdd());
                } else if (outcome.getName().equals("Vitória Fora") || outcome.getName().equals(awayName)) {
                    outcome.setOdd(odds.getAwayOdd());
                }
            }

            outcomeRepository.saveAll(outcomes);

            MarketResponseDTO dto = marketMapper.toResponse(market);
            List<OutcomeResponseDTO> outcomeDTOs = outcomes.stream()
                    .map(outcomeMapper::toResponse)
                    .toList();
            dto.setOutcomes(outcomeDTOs);
            eventPublisher.publishEvent(new MarketChangeEvent(this, event.getId(), dto));

            updatedCount++;
        }

        log.info("Recalculated odds for {} future events in tournament {}", updatedCount, tournamentId);
    }
}
