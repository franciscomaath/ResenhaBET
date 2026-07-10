package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.OddsProperties;
import com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.OutcomeResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.*;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.enums.MarketStatus;
import com.franciscomaath.resenhaapi.domain.enums.MarketType;
import com.franciscomaath.resenhaapi.domain.event.MarketChangeEvent;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.MarketRepository;
import com.franciscomaath.resenhaapi.domain.repository.OutcomeRepository;
import com.franciscomaath.resenhaapi.mapper.MarketMapper;
import com.franciscomaath.resenhaapi.mapper.OutcomeMapper;
import com.franciscomaath.resenhaapi.service.OddsCalculatorService;
import com.franciscomaath.resenhaapi.service.dto.OddsResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OddsUpdateServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private MarketRepository marketRepository;

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private OddsCalculatorService oddsCalculatorService;

    @Mock
    private OddsProperties oddsProperties;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private MarketMapper marketMapper;

    @Mock
    private OutcomeMapper outcomeMapper;

    @InjectMocks
    private OddsUpdateServiceImpl oddsUpdateService;

    private Player homePlayer;
    private Player awayPlayer;
    private Event event;
    private Market market;

    @BeforeEach
    void setUp() {
        homePlayer = new Player();
        homePlayer.setId(1L);
        homePlayer.setName("Home");
        homePlayer.setCurrentElo(BigDecimal.valueOf(1000));

        awayPlayer = new Player();
        awayPlayer.setId(2L);
        awayPlayer.setName("Away");
        awayPlayer.setCurrentElo(BigDecimal.valueOf(1000));

        event = new Event();
        event.setId(10L);
        event.setPlayerHome(homePlayer);
        event.setPlayerAway(awayPlayer);
        event.setIsKnockout(false);

        market = new Market();
        market.setId(20L);
        market.setEvent(event);
        market.setStatus(MarketStatus.OPEN);
        market.setMarketType(MarketType.MATCH_RESULT);
    }

    @Test
    void recalculate_whenNoFutureEvents_returnsEarly() {
        when(eventRepository.findFutureEventsWithPlayers(1L)).thenReturn(List.of());

        oddsUpdateService.recalculateFutureOdds(1L);

        verify(marketRepository, never()).findByEventIdIn(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void recalculate_whenNoMarkets_returnsEarly() {
        when(eventRepository.findFutureEventsWithPlayers(1L)).thenReturn(List.of(event));
        when(marketRepository.findByEventIdIn(anyList())).thenReturn(List.of());

        oddsUpdateService.recalculateFutureOdds(1L);

        verify(outcomeRepository, never()).findByMarketIdIn(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void recalculate_whenMarketNotOpen_skipsEvent() {
        market.setStatus(MarketStatus.CLOSED);

        when(eventRepository.findFutureEventsWithPlayers(1L)).thenReturn(List.of(event));
        when(marketRepository.findByEventIdIn(anyList())).thenReturn(List.of(market));
        when(outcomeRepository.findByMarketIdIn(anyList())).thenReturn(List.of());

        oddsUpdateService.recalculateFutureOdds(1L);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void recalculate_whenOutcomesEmpty_skipsEvent() {
        when(eventRepository.findFutureEventsWithPlayers(1L)).thenReturn(List.of(event));
        when(marketRepository.findByEventIdIn(anyList())).thenReturn(List.of(market));
        when(outcomeRepository.findByMarketIdIn(anyList())).thenReturn(List.of());

        oddsUpdateService.recalculateFutureOdds(1L);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void recalculate_knockoutEvent_calculatesNoDrawAndUpdatesOutcomes() {
        event.setIsKnockout(true);

        Outcome homeOutcome = new Outcome();
        homeOutcome.setId(1L);
        homeOutcome.setMarket(market);
        homeOutcome.setName("Home");
        homeOutcome.setOdd(BigDecimal.valueOf(2.00));

        Outcome drawOutcome = new Outcome();
        drawOutcome.setId(2L);
        drawOutcome.setMarket(market);
        drawOutcome.setName("Empate");
        drawOutcome.setOdd(BigDecimal.valueOf(3.00));

        Outcome awayOutcome = new Outcome();
        awayOutcome.setId(3L);
        awayOutcome.setMarket(market);
        awayOutcome.setName("Away");
        awayOutcome.setOdd(BigDecimal.valueOf(4.00));

        List<Outcome> outcomes = List.of(homeOutcome, drawOutcome, awayOutcome);

        OddsResult oddsResult = new OddsResult(
                BigDecimal.valueOf(1.50), BigDecimal.valueOf(3.30), BigDecimal.valueOf(2.50));

        when(oddsProperties.getH2hMatchLimit()).thenReturn(5);
        when(eventRepository.findFutureEventsWithPlayers(1L)).thenReturn(List.of(event));
        when(marketRepository.findByEventIdIn(anyList())).thenReturn(List.of(market));
        when(outcomeRepository.findByMarketIdIn(anyList())).thenReturn(outcomes);
        when(eventRepository.findDirectConfrontations(eq(1L), eq(2L), eq(EventStatus.COMPLETED), any(Pageable.class)))
                .thenReturn(List.of());
        when(oddsCalculatorService.calculateNoDraw(any(BigDecimal.class), any(BigDecimal.class), any()))
                .thenReturn(oddsResult);
        when(marketMapper.toResponse(any(Market.class))).thenReturn(new MarketResponseDTO());
        when(outcomeMapper.toResponse(any(Outcome.class))).thenReturn(new OutcomeResponseDTO());

        oddsUpdateService.recalculateFutureOdds(1L);

        assertEquals(0, oddsResult.getHomeOdd().compareTo(homeOutcome.getOdd()));
        assertEquals(0, oddsResult.getDrawOdd().compareTo(drawOutcome.getOdd()));
        assertEquals(0, oddsResult.getAwayOdd().compareTo(awayOutcome.getOdd()));

        verify(outcomeRepository).saveAll(outcomes);
        verify(eventPublisher).publishEvent(any(MarketChangeEvent.class));
    }

    @Test
    void recalculate_nonKnockoutEvent_calculatesAndUpdatesOutcomes() {
        Outcome homeOutcome = new Outcome();
        homeOutcome.setId(1L);
        homeOutcome.setMarket(market);
        homeOutcome.setName("Home");
        homeOutcome.setOdd(BigDecimal.valueOf(2.00));

        Outcome drawOutcome = new Outcome();
        drawOutcome.setId(2L);
        drawOutcome.setMarket(market);
        drawOutcome.setName("Empate");
        drawOutcome.setOdd(BigDecimal.valueOf(3.00));

        Outcome awayOutcome = new Outcome();
        awayOutcome.setId(3L);
        awayOutcome.setMarket(market);
        awayOutcome.setName("Away");
        awayOutcome.setOdd(BigDecimal.valueOf(4.00));

        List<Outcome> outcomes = List.of(homeOutcome, drawOutcome, awayOutcome);

        OddsResult oddsResult = new OddsResult(
                BigDecimal.valueOf(2.10), BigDecimal.valueOf(3.20), BigDecimal.valueOf(3.50));

        when(oddsProperties.getH2hMatchLimit()).thenReturn(5);
        when(eventRepository.findFutureEventsWithPlayers(1L)).thenReturn(List.of(event));
        when(marketRepository.findByEventIdIn(anyList())).thenReturn(List.of(market));
        when(outcomeRepository.findByMarketIdIn(anyList())).thenReturn(outcomes);
        when(eventRepository.findDirectConfrontations(eq(1L), eq(2L), eq(EventStatus.COMPLETED), any(Pageable.class)))
                .thenReturn(List.of());
        when(oddsCalculatorService.calculate(any(BigDecimal.class), any(BigDecimal.class), any()))
                .thenReturn(oddsResult);
        when(marketMapper.toResponse(any(Market.class))).thenReturn(new MarketResponseDTO());
        when(outcomeMapper.toResponse(any(Outcome.class))).thenReturn(new OutcomeResponseDTO());

        oddsUpdateService.recalculateFutureOdds(1L);

        assertEquals(0, oddsResult.getHomeOdd().compareTo(homeOutcome.getOdd()));
        assertEquals(0, oddsResult.getDrawOdd().compareTo(drawOutcome.getOdd()));
        assertEquals(0, oddsResult.getAwayOdd().compareTo(awayOutcome.getOdd()));

        verify(outcomeRepository).saveAll(outcomes);
        verify(eventPublisher).publishEvent(any(MarketChangeEvent.class));
    }

    @Test
    void recalculate_unmatchedOutcomeName_doesNotUpdateOdd() {
        Outcome matchedOutcome = new Outcome();
        matchedOutcome.setId(1L);
        matchedOutcome.setMarket(market);
        matchedOutcome.setName("Home");
        matchedOutcome.setOdd(BigDecimal.valueOf(2.00));

        Outcome unmatchedOutcome = new Outcome();
        unmatchedOutcome.setId(2L);
        unmatchedOutcome.setMarket(market);
        unmatchedOutcome.setName("Outro");
        unmatchedOutcome.setOdd(BigDecimal.valueOf(5.00));

        List<Outcome> outcomes = List.of(matchedOutcome, unmatchedOutcome);

        OddsResult oddsResult = new OddsResult(
                BigDecimal.valueOf(1.50), BigDecimal.valueOf(3.30), BigDecimal.valueOf(2.50));

        when(oddsProperties.getH2hMatchLimit()).thenReturn(5);
        when(eventRepository.findFutureEventsWithPlayers(1L)).thenReturn(List.of(event));
        when(marketRepository.findByEventIdIn(anyList())).thenReturn(List.of(market));
        when(outcomeRepository.findByMarketIdIn(anyList())).thenReturn(outcomes);
        when(eventRepository.findDirectConfrontations(eq(1L), eq(2L), eq(EventStatus.COMPLETED), any(Pageable.class)))
                .thenReturn(List.of());
        when(oddsCalculatorService.calculate(any(BigDecimal.class), any(BigDecimal.class), any()))
                .thenReturn(oddsResult);
        when(marketMapper.toResponse(any(Market.class))).thenReturn(new MarketResponseDTO());
        when(outcomeMapper.toResponse(any(Outcome.class))).thenReturn(new OutcomeResponseDTO());

        oddsUpdateService.recalculateFutureOdds(1L);

        assertEquals(0, oddsResult.getHomeOdd().compareTo(matchedOutcome.getOdd()));
        assertEquals(0, BigDecimal.valueOf(5.00).compareTo(unmatchedOutcome.getOdd()));

        verify(outcomeRepository).saveAll(outcomes);
        verify(eventPublisher).publishEvent(any(MarketChangeEvent.class));
    }

    @Test
    void recalculate_whenEventHasMultipleMarkets_updatesOnlyMatchResultMarket() {
        Market otherMarket = new Market();
        otherMarket.setId(30L);
        otherMarket.setEvent(event);
        otherMarket.setStatus(MarketStatus.OPEN);
        otherMarket.setMarketType(MarketType.OVER_UNDER_25);

        Outcome homeOutcome = new Outcome();
        homeOutcome.setId(1L);
        homeOutcome.setMarket(market);
        homeOutcome.setName("Home");
        homeOutcome.setOdd(BigDecimal.valueOf(2.00));

        Outcome otherOutcome = new Outcome();
        otherOutcome.setId(2L);
        otherOutcome.setMarket(otherMarket);
        otherOutcome.setName("Over 2.5");
        otherOutcome.setOdd(BigDecimal.valueOf(2.00));

        OddsResult oddsResult = new OddsResult(
                BigDecimal.valueOf(1.50), BigDecimal.valueOf(3.30), BigDecimal.valueOf(2.50));

        when(oddsProperties.getH2hMatchLimit()).thenReturn(5);
        when(eventRepository.findFutureEventsWithPlayers(1L)).thenReturn(List.of(event));
        when(marketRepository.findByEventIdIn(anyList())).thenReturn(List.of(market, otherMarket));
        when(outcomeRepository.findByMarketIdIn(List.of(20L))).thenReturn(List.of(homeOutcome));
        when(eventRepository.findDirectConfrontations(eq(1L), eq(2L), eq(EventStatus.COMPLETED), any(Pageable.class)))
                .thenReturn(List.of());
        when(oddsCalculatorService.calculate(any(BigDecimal.class), any(BigDecimal.class), any()))
                .thenReturn(oddsResult);
        when(marketMapper.toResponse(any(Market.class))).thenReturn(new MarketResponseDTO());
        when(outcomeMapper.toResponse(any(Outcome.class))).thenReturn(new OutcomeResponseDTO());

        oddsUpdateService.recalculateFutureOdds(1L);

        assertEquals(0, oddsResult.getHomeOdd().compareTo(homeOutcome.getOdd()));
        assertEquals(0, BigDecimal.valueOf(2.00).compareTo(otherOutcome.getOdd()));
        verify(outcomeRepository).findByMarketIdIn(List.of(20L));
        verify(outcomeRepository).saveAll(List.of(homeOutcome));
    }
}
