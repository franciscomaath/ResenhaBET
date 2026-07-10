package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.OutcomeResponseDTO;
import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.entity.Market;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.entity.Outcome;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
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
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.testsupport.MultiGroupFixtures;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MarketServiceImplTest {

    @Mock
    private MarketRepository marketRepository;

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private MarketMapper marketMapper;

    @Mock
    private OutcomeMapper outcomeMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @Mock
    private GroupTournamentRepository groupTournamentRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @InjectMocks
    private MarketServiceImpl marketService;

    @Test
    void findByEventId_shouldReturnMarketWithOutcomes() {
        Player playerHome = new Player();
        playerHome.setId(1L);
        playerHome.setName("Casa");

        Player playerAway = new Player();
        playerAway.setId(2L);
        playerAway.setName("Fora");

        Event event = eventWithTournament();
        event.setId(1L);
        event.setPlayerHome(playerHome);
        event.setPlayerAway(playerAway);

        Market market = new Market();
        market.setId(10L);
        market.setEvent(event);
        market.setName("Resultado Final");
        market.setStatus(MarketStatus.OPEN);

        Outcome outcome1 = new Outcome();
        outcome1.setId(100L);
        outcome1.setName("Casa");
        outcome1.setOdd(new BigDecimal("2.10"));
        outcome1.setMarket(market);

        Outcome outcome2 = new Outcome();
        outcome2.setId(101L);
        outcome2.setName("Empate");
        outcome2.setOdd(new BigDecimal("3.20"));
        outcome2.setMarket(market);

        Outcome outcome3 = new Outcome();
        outcome3.setId(102L);
        outcome3.setName("Fora");
        outcome3.setOdd(new BigDecimal("3.50"));
        outcome3.setMarket(market);

        MarketResponseDTO responseDTO = new MarketResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setEventId(1L);
        responseDTO.setName("Resultado Final");
        responseDTO.setStatus("OPEN");

        when(marketRepository.findByEventIdAndMarketType(1L, MarketType.MATCH_RESULT)).thenReturn(Optional.of(market));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(outcomeRepository.findByMarketId(10L)).thenReturn(List.of(outcome1, outcome2, outcome3));
        when(marketMapper.toResponse(market)).thenReturn(responseDTO);

        MarketResponseDTO result = marketService.findByEventId(1L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getEventId());
        assertEquals("OPEN", result.getStatus());
        verify(marketMapper, times(3)).toOutcomeResponse(any(Outcome.class));
    }

    @Test
    void findByEventId_whenMarketNotFound_shouldThrowResourceNotFoundException() {
        Player playerHome = new Player();
        playerHome.setId(1L);
        playerHome.setName("Casa");

        Player playerAway = new Player();
        playerAway.setId(2L);
        playerAway.setName("Fora");

        Event event = eventWithTournament();
        event.setId(999L);
        event.setPlayerHome(playerHome);
        event.setPlayerAway(playerAway);

        when(eventRepository.findById(999L)).thenReturn(Optional.of(event));
        when(marketRepository.findByEventIdAndMarketType(999L, MarketType.MATCH_RESULT)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> marketService.findByEventId(999L));

        assertTrue(exception.getMessage().contains("999"));
        verify(outcomeRepository, never()).findByMarketId(any());
    }

    @Test
    void openMarket_shouldSetStatusToOpen() {
        Event event = eventWithTournament();
        event.setId(1L);

        Market market = new Market();
        market.setId(10L);
        market.setEvent(event);
        market.setStatus(MarketStatus.CLOSED);

        MarketResponseDTO responseDTO = new MarketResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setEventId(1L);
        responseDTO.setStatus("OPEN");

        when(marketRepository.findAllByEventId(1L)).thenReturn(List.of(market));
        when(marketMapper.toResponse(market)).thenReturn(responseDTO);
        when(outcomeRepository.findByMarketId(10L)).thenReturn(List.of());

        marketService.openMarket(1L);

        ArgumentCaptor<Market> captor = ArgumentCaptor.forClass(Market.class);
        verify(marketRepository).save(captor.capture());
        assertEquals(MarketStatus.OPEN, captor.getValue().getStatus());
    }

    @Test
    void openMarket_whenNotFound_shouldThrowResourceNotFoundException() {
        when(marketRepository.findAllByEventId(999L)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> marketService.openMarket(999L));

        verify(marketRepository, never()).save(any());
    }

    @Test
    void closeMarket_shouldSetStatusToClosed() {
        Event event = eventWithTournament();
        event.setId(1L);

        Market market = new Market();
        market.setId(10L);
        market.setEvent(event);
        market.setStatus(MarketStatus.OPEN);

        MarketResponseDTO responseDTO = new MarketResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setEventId(1L);
        responseDTO.setStatus("CLOSED");

        when(marketRepository.findAllByEventId(1L)).thenReturn(List.of(market));
        when(marketMapper.toResponse(market)).thenReturn(responseDTO);
        when(outcomeRepository.findByMarketId(10L)).thenReturn(List.of());

        marketService.closeMarket(1L);

        ArgumentCaptor<Market> captor = ArgumentCaptor.forClass(Market.class);
        verify(marketRepository).save(captor.capture());
        assertEquals(MarketStatus.CLOSED, captor.getValue().getStatus());
    }

    @Test
    void closeMarket_whenNotFound_shouldThrowResourceNotFoundException() {
        when(marketRepository.findAllByEventId(999L)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> marketService.closeMarket(999L));

        verify(marketRepository, never()).save(any());
    }

    @Test
    void cancelMarket_shouldSetStatusToCancelled() {
        Event event = eventWithTournament();
        event.setId(1L);

        Market market = new Market();
        market.setId(10L);
        market.setEvent(event);
        market.setStatus(MarketStatus.OPEN);

        MarketResponseDTO responseDTO = new MarketResponseDTO();
        responseDTO.setId(10L);
        responseDTO.setEventId(1L);
        responseDTO.setStatus("CANCELLED");

        when(marketRepository.findAllByEventId(1L)).thenReturn(List.of(market));
        when(marketMapper.toResponse(market)).thenReturn(responseDTO);
        when(outcomeRepository.findByMarketId(10L)).thenReturn(List.of());

        marketService.cancelMarket(1L);

        ArgumentCaptor<Market> captor = ArgumentCaptor.forClass(Market.class);
        verify(marketRepository).save(captor.capture());
        assertEquals(MarketStatus.CANCELLED, captor.getValue().getStatus());
    }

    @Test
    void cancelMarket_whenNotFound_shouldThrowResourceNotFoundException() {
        when(marketRepository.findAllByEventId(999L)).thenReturn(List.of());

        assertThrows(ResourceNotFoundException.class,
                () -> marketService.cancelMarket(999L));

        verify(marketRepository, never()).save(any());
    }

    // ========================
    // findAllByEventId — multi-market return
    // ========================

    @Test
    void findAllByEventId_shouldReturnMultipleMarkets() {
        Event event = eventWithTournament();
        event.setId(1L);

        Set<MarketType> marketTypes = EnumSet.of(MarketType.MATCH_RESULT, MarketType.OVER_UNDER_25);
        GroupTournament groupTournament = groupTournamentWithMarketTypes(marketTypes);

        Market market1 = new Market();
        market1.setId(10L);
        market1.setEvent(event);
        market1.setName("Resultado Final");

        Market market2 = new Market();
        market2.setId(20L);
        market2.setEvent(event);
        market2.setName("Over/Under 2.5");

        MarketResponseDTO dto1 = new MarketResponseDTO();
        dto1.setId(10L);
        MarketResponseDTO dto2 = new MarketResponseDTO();
        dto2.setId(20L);

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(groupTournamentRepository.findByTournamentIdAndGroupId(99L, 10L)).thenReturn(Optional.of(groupTournament));
        when(marketRepository.findAllByEventIdAndMarketTypeIn(1L, marketTypes)).thenReturn(List.of(market1, market2));
        when(marketMapper.toResponse(market1)).thenReturn(dto1);
        when(marketMapper.toResponse(market2)).thenReturn(dto2);
        when(outcomeRepository.findByMarketId(10L)).thenReturn(List.of());
        when(outcomeRepository.findByMarketId(20L)).thenReturn(List.of());

        List<MarketResponseDTO> result = marketService.findAllByEventId(1L);

        assertEquals(2, result.size());
    }

    @Test
    void findAllByEventId_whenNoMarkets_returnsEmptyList() {
        Event event = eventWithTournament();
        event.setId(1L);

        Set<MarketType> marketTypes = EnumSet.of(MarketType.MATCH_RESULT);
        GroupTournament groupTournament = groupTournamentWithMarketTypes(marketTypes);

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(groupTournamentRepository.findByTournamentIdAndGroupId(99L, 10L)).thenReturn(Optional.of(groupTournament));
        when(marketRepository.findAllByEventIdAndMarketTypeIn(1L, marketTypes)).thenReturn(List.of());

        List<MarketResponseDTO> result = marketService.findAllByEventId(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllByEventId_whenEventNotFound_throws() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> marketService.findAllByEventId(999L));
    }

    @Test
    void findByEventId_whenRealFootballEvent_returnsEmptyDto() {
        Event event = eventWithTournament();
        event.setId(1L);
        event.setPlayerHome(null);
        event.setPlayerAway(null);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        MarketResponseDTO result = marketService.findByEventId(1L);

        assertNotNull(result);
        assertNull(result.getId());
        verify(marketRepository, never()).findByEventIdAndMarketType(any(), any());
    }

    @Test
    void openMarket_whenEventCompleted_throwsBusinessException() {
        Event event = eventWithTournament();
        event.setId(1L);
        event.setStatus(EventStatus.COMPLETED);

        Market market = new Market();
        market.setId(10L);
        market.setEvent(event);

        when(marketRepository.findAllByEventId(1L)).thenReturn(List.of(market));

        assertThrows(BusinessException.class,
                () -> marketService.openMarket(1L));
    }

    @Test
    void openMarket_iteratesAllMarkets() {
        Event event = eventWithTournament();
        event.setId(1L);

        Market market1 = new Market();
        market1.setId(10L);
        market1.setEvent(event);
        market1.setStatus(MarketStatus.CLOSED);

        Market market2 = new Market();
        market2.setId(20L);
        market2.setEvent(event);
        market2.setStatus(MarketStatus.CLOSED);

        when(marketRepository.findAllByEventId(1L)).thenReturn(List.of(market1, market2));
        when(marketMapper.toResponse(any(Market.class))).thenReturn(new MarketResponseDTO());
        when(outcomeRepository.findByMarketId(anyLong())).thenReturn(List.of());

        marketService.openMarket(1L);

        assertEquals(MarketStatus.OPEN, market1.getStatus());
        assertEquals(MarketStatus.OPEN, market2.getStatus());
        verify(marketRepository, times(2)).save(any(Market.class));
    }

    @Test
    void closeMarket_iteratesAllMarkets() {
        Event event = eventWithTournament();
        event.setId(1L);

        Market market1 = new Market();
        market1.setId(10L);
        market1.setEvent(event);
        market1.setStatus(MarketStatus.OPEN);

        Market market2 = new Market();
        market2.setId(20L);
        market2.setEvent(event);
        market2.setStatus(MarketStatus.OPEN);

        when(marketRepository.findAllByEventId(1L)).thenReturn(List.of(market1, market2));
        when(marketMapper.toResponse(any(Market.class))).thenReturn(new MarketResponseDTO());
        when(outcomeRepository.findByMarketId(anyLong())).thenReturn(List.of());

        marketService.closeMarket(1L);

        assertEquals(MarketStatus.CLOSED, market1.getStatus());
        assertEquals(MarketStatus.CLOSED, market2.getStatus());
        verify(marketRepository, times(2)).save(any(Market.class));
    }

    @Test
    void cancelMarket_iteratesAllMarkets() {
        Event event = eventWithTournament();
        event.setId(1L);

        Market market1 = new Market();
        market1.setId(10L);
        market1.setEvent(event);
        market1.setStatus(MarketStatus.OPEN);

        Market market2 = new Market();
        market2.setId(20L);
        market2.setEvent(event);
        market2.setStatus(MarketStatus.OPEN);

        when(marketRepository.findAllByEventId(1L)).thenReturn(List.of(market1, market2));
        when(marketMapper.toResponse(any(Market.class))).thenReturn(new MarketResponseDTO());
        when(outcomeRepository.findByMarketId(anyLong())).thenReturn(List.of());

        marketService.cancelMarket(1L);

        assertEquals(MarketStatus.CANCELLED, market1.getStatus());
        assertEquals(MarketStatus.CANCELLED, market2.getStatus());
        verify(marketRepository, times(2)).save(any(Market.class));
    }

    private Event eventWithTournament() {
        Event event = new Event();
        event.setTournament(MultiGroupFixtures.tournament(99L, TournamentType.FIFA_MATCH));
        return event;
    }

    private GroupTournament groupTournamentWithMarketTypes(Set<MarketType> marketTypes) {
        return GroupTournament.builder()
                .id(100L)
                .marketTypes(marketTypes)
                .build();
    }
}
