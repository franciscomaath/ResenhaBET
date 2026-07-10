package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.entity.TournamentRound;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.PlayerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EloServiceImplTest {

    private static final BigDecimal INITIAL_ELO = BigDecimal.valueOf(1000);
    private static final BigDecimal K_FACTOR = BigDecimal.valueOf(32);
    private static final int ELO_SCALE = 2;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private PlayerRepository playerRepository;

    @InjectMocks
    private EloServiceImpl eloService;

    @Test
    void calculateElo_shouldRebuildFromHistoryUsingStoredEloBefore() {
        Player target = Player.builder().id(1L).name("Target").build();
        Player opponentA = Player.builder().id(2L).name("OppA").build();
        Player opponentB = Player.builder().id(3L).name("OppB").build();
        TournamentRound round = TournamentRound.builder().id(1L).multiplier(BigDecimal.ONE).build();

        Event winHome = Event.builder()
                .id(10L)
                .status(EventStatus.COMPLETED)
                .playerHome(target)
                .playerAway(opponentA)
                .homeScore(2)
                .awayScore(1)
                .homeEloBefore(BigDecimal.valueOf(1000))
                .awayEloBefore(BigDecimal.valueOf(1000))
                .round(round)
                .build();

        Event drawAway = Event.builder()
                .id(11L)
                .status(EventStatus.COMPLETED)
                .playerHome(opponentB)
                .playerAway(target)
                .homeScore(1)
                .awayScore(1)
                .homeEloBefore(BigDecimal.valueOf(1000))
                .awayEloBefore(BigDecimal.valueOf(1016))
                .round(round)
                .build();

        when(eventRepository.findCompletedNonByeByPlayerId(1L, EventStatus.COMPLETED))
                .thenReturn(List.of(winHome, drawAway));
        when(playerRepository.save(any(Player.class))).thenReturn(target);

        BigDecimal result = eloService.calculateElo(target);

        BigDecimal afterWin = applyDelta(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 1.0);
        BigDecimal finalElo = applyDelta(afterWin, expectedScore(afterWin.doubleValue(), 1000), 0.5);

        assertEquals(finalElo, result);
        assertEquals(finalElo, target.getCurrentElo());
        verify(playerRepository).save(target);
    }

    @Test
    void applyEloForEvent_shouldUpdateBothPlayersUsingStoredEloBefore() {
        Player home = Player.builder().id(1L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(2L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();

        Event event = Event.builder()
                .id(20L)
                .status(EventStatus.COMPLETED)
                .playerHome(home)
                .playerAway(away)
                .homeScore(3)
                .awayScore(1)
                .homeEloBefore(BigDecimal.valueOf(1200))
                .awayEloBefore(BigDecimal.valueOf(1100))
                .round(TournamentRound.builder().id(1L).multiplier(BigDecimal.ONE).build())
                .build();

        BigDecimal result = eloService.applyEloForEvent(event);

        BigDecimal expectedHome = applyDelta(BigDecimal.valueOf(1200), expectedScore(1200, 1100), 1.0);
        BigDecimal expectedAway = applyDelta(BigDecimal.valueOf(1100), expectedScore(1100, 1200), 0.0);

        assertEquals(expectedHome, result);
        assertEquals(expectedHome, home.getCurrentElo());
        assertEquals(expectedAway, away.getCurrentElo());
        verify(playerRepository).save(home);
        verify(playerRepository).save(away);
    }

    @Test
    void applyEloForEvent_shouldScaleKFactorByRoundMultiplier() {
        Player home = Player.builder().id(1L).name("Home").currentElo(BigDecimal.valueOf(1000)).build();
        Player away = Player.builder().id(2L).name("Away").currentElo(BigDecimal.valueOf(1000)).build();

        Event event = Event.builder()
                .id(21L)
                .status(EventStatus.COMPLETED)
                .playerHome(home)
                .playerAway(away)
                .homeScore(2)
                .awayScore(0)
                .homeEloBefore(BigDecimal.valueOf(1000))
                .awayEloBefore(BigDecimal.valueOf(1000))
                .round(TournamentRound.builder().id(2L).multiplier(new BigDecimal("1.5")).build())
                .build();

        BigDecimal result = eloService.applyEloForEvent(event);

        BigDecimal effectiveK = BigDecimal.valueOf(32).multiply(new BigDecimal("1.5"));
        BigDecimal expectedHome = applyDeltaWithK(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 1.0, effectiveK);
        BigDecimal expectedAway = applyDeltaWithK(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 0.0, effectiveK);

        assertEquals(expectedHome, result);
        assertEquals(expectedHome, home.getCurrentElo());
        assertEquals(expectedAway, away.getCurrentElo());
    }

    @Test
    void applyEloForEvent_shouldDefaultToMultiplierOneWhenRoundIsNull() {
        Player home = Player.builder().id(1L).name("Home").currentElo(BigDecimal.valueOf(1000)).build();
        Player away = Player.builder().id(2L).name("Away").currentElo(BigDecimal.valueOf(1000)).build();

        Event event = Event.builder()
                .id(22L)
                .status(EventStatus.COMPLETED)
                .playerHome(home)
                .playerAway(away)
                .homeScore(1)
                .awayScore(0)
                .homeEloBefore(BigDecimal.valueOf(1000))
                .awayEloBefore(BigDecimal.valueOf(1000))
                .round(null)
                .build();

        BigDecimal result = eloService.applyEloForEvent(event);

        BigDecimal expectedHome = applyDelta(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 1.0);

        assertEquals(expectedHome, result);
    }

    @Test
    void applyEloForEvent_whenScoresMissing_shouldReturnNullAndNotPersist() {
        Player home = Player.builder().id(1L).name("Home").currentElo(BigDecimal.valueOf(1200)).build();
        Player away = Player.builder().id(2L).name("Away").currentElo(BigDecimal.valueOf(1100)).build();

        Event event = Event.builder()
                .id(30L)
                .status(EventStatus.COMPLETED)
                .playerHome(home)
                .playerAway(away)
                .homeScore(null)
                .awayScore(1)
                .build();

        BigDecimal result = eloService.applyEloForEvent(event);

        assertNull(result);
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    void calculateElo_withNoEvents_shouldReturnInitialElo() {
        Player target = Player.builder().id(1L).name("Target").build();

        when(eventRepository.findCompletedNonByeByPlayerId(1L, EventStatus.COMPLETED))
                .thenReturn(List.of());
        when(playerRepository.save(any(Player.class))).thenReturn(target);

        BigDecimal result = eloService.calculateElo(target);

        assertEquals(BigDecimal.valueOf(1000), result);
        assertEquals(BigDecimal.valueOf(1000), target.getCurrentElo());
        verify(playerRepository).save(target);
    }

    @Test
    void calculateElo_shouldSkipEventsWithNullScores() {
        Player target = Player.builder().id(1L).name("Target").build();
        Player opponent = Player.builder().id(2L).name("Opp").build();
        TournamentRound round = TournamentRound.builder().id(1L).multiplier(BigDecimal.ONE).build();

        Event validEvent = Event.builder()
                .id(10L)
                .status(EventStatus.COMPLETED)
                .playerHome(target)
                .playerAway(opponent)
                .homeScore(2)
                .awayScore(1)
                .homeEloBefore(BigDecimal.valueOf(1000))
                .awayEloBefore(BigDecimal.valueOf(1000))
                .round(round)
                .build();

        Event nullScoreEvent = Event.builder()
                .id(11L)
                .status(EventStatus.COMPLETED)
                .playerHome(target)
                .playerAway(opponent)
                .homeScore(null)
                .awayScore(1)
                .homeEloBefore(BigDecimal.valueOf(1000))
                .awayEloBefore(BigDecimal.valueOf(1000))
                .round(round)
                .build();

        when(eventRepository.findCompletedNonByeByPlayerId(1L, EventStatus.COMPLETED))
                .thenReturn(List.of(validEvent, nullScoreEvent));
        when(playerRepository.save(any(Player.class))).thenReturn(target);

        BigDecimal result = eloService.calculateElo(target);

        BigDecimal expectedElo = applyDelta(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 1.0);
        assertEquals(expectedElo, result);
    }

    @Test
    void calculateElo_shouldApplyDifferentMultipliersPerEvent() {
        Player target = Player.builder().id(1L).name("Target").build();
        Player opponent = Player.builder().id(2L).name("Opp").build();
        TournamentRound regularRound = TournamentRound.builder().id(1L).multiplier(BigDecimal.ONE).build();
        TournamentRound finalRound = TournamentRound.builder().id(2L).multiplier(new BigDecimal("1.5")).build();

        Event regularWin = Event.builder()
                .id(10L)
                .status(EventStatus.COMPLETED)
                .playerHome(target)
                .playerAway(opponent)
                .homeScore(2)
                .awayScore(0)
                .homeEloBefore(BigDecimal.valueOf(1000))
                .awayEloBefore(BigDecimal.valueOf(1000))
                .round(regularRound)
                .build();

        Event finalWin = Event.builder()
                .id(11L)
                .status(EventStatus.COMPLETED)
                .playerHome(target)
                .playerAway(opponent)
                .homeScore(3)
                .awayScore(0)
                .homeEloBefore(BigDecimal.valueOf(1016))
                .awayEloBefore(BigDecimal.valueOf(1000))
                .round(finalRound)
                .build();

        when(eventRepository.findCompletedNonByeByPlayerId(1L, EventStatus.COMPLETED))
                .thenReturn(List.of(regularWin, finalWin));
        when(playerRepository.save(any(Player.class))).thenReturn(target);

        BigDecimal result = eloService.calculateElo(target);

        BigDecimal afterRegular = applyDeltaWithK(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 1.0, BigDecimal.valueOf(32));
        BigDecimal afterFinal = applyDeltaWithK(afterRegular, expectedScore(afterRegular.doubleValue(), 1000), 1.0, BigDecimal.valueOf(48));

        assertEquals(afterFinal, result);
    }

    @Test
    void applyEloForEvent_withDraw_shouldMoveElosLessThanWin() {
        Player home = Player.builder().id(1L).name("Home").currentElo(BigDecimal.valueOf(1000)).build();
        Player away = Player.builder().id(2L).name("Away").currentElo(BigDecimal.valueOf(1000)).build();

        Event drawEvent = Event.builder()
                .id(20L)
                .status(EventStatus.COMPLETED)
                .playerHome(home)
                .playerAway(away)
                .homeScore(1)
                .awayScore(1)
                .homeEloBefore(BigDecimal.valueOf(1000))
                .awayEloBefore(BigDecimal.valueOf(1000))
                .round(TournamentRound.builder().id(1L).multiplier(BigDecimal.ONE).build())
                .build();

        BigDecimal result = eloService.applyEloForEvent(drawEvent);

        BigDecimal expectedElo = applyDelta(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 0.5);
        assertEquals(expectedElo, result);
        assertEquals(expectedElo, home.getCurrentElo());
        assertEquals(expectedElo, away.getCurrentElo());
    }

    @Test
    void applyEloForEvent_shouldFallbackToCurrentEloWhenEloBeforeNull() {
        Player home = Player.builder().id(1L).name("Home").currentElo(BigDecimal.valueOf(1100)).build();
        Player away = Player.builder().id(2L).name("Away").currentElo(BigDecimal.valueOf(900)).build();

        Event event = Event.builder()
                .id(21L)
                .status(EventStatus.COMPLETED)
                .playerHome(home)
                .playerAway(away)
                .homeScore(2)
                .awayScore(0)
                .homeEloBefore(null)
                .awayEloBefore(null)
                .round(TournamentRound.builder().id(1L).multiplier(BigDecimal.ONE).build())
                .build();

        BigDecimal result = eloService.applyEloForEvent(event);

        BigDecimal expectedHome = applyDelta(BigDecimal.valueOf(1100), expectedScore(1100, 900), 1.0);
        BigDecimal expectedAway = applyDelta(BigDecimal.valueOf(900), expectedScore(900, 1100), 0.0);

        assertEquals(expectedHome, result);
        assertEquals(expectedHome, home.getCurrentElo());
        assertEquals(expectedAway, away.getCurrentElo());
    }

    @Test
    void applyEloForEvent_shouldTreatPenaltyWinAsWinNotDraw() {
        Player home = Player.builder().id(1L).name("Home").currentElo(BigDecimal.valueOf(1000)).build();
        Player away = Player.builder().id(2L).name("Away").currentElo(BigDecimal.valueOf(1000)).build();

        Event event = Event.builder()
                .id(40L)
                .status(EventStatus.COMPLETED)
                .playerHome(home)
                .playerAway(away)
                .homeScore(1)
                .awayScore(1)
                .penaltiesHome(4)
                .penaltiesAway(3)
                .homeEloBefore(BigDecimal.valueOf(1000))
                .awayEloBefore(BigDecimal.valueOf(1000))
                .round(TournamentRound.builder().id(1L).multiplier(BigDecimal.ONE).build())
                .build();

        BigDecimal result = eloService.applyEloForEvent(event);

        BigDecimal expectedHome = applyDelta(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 1.0);
        BigDecimal expectedAway = applyDelta(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 0.0);

        assertEquals(expectedHome, result);
        assertEquals(expectedHome, home.getCurrentElo());
        assertEquals(expectedAway, away.getCurrentElo());
    }

    @Test
    void recalculateGroupElos_shouldReplayFullGroupHistoryChronologically() {
        com.franciscomaath.resenhaapi.domain.entity.Group group = com.franciscomaath.resenhaapi.domain.entity.Group.builder()
                .id(10L)
                .name("Group")
                .active(true)
                .build();
        Player p1 = Player.builder().id(1L).name("P1").group(group).build();
        Player p2 = Player.builder().id(2L).name("P2").group(group).build();
        TournamentRound round = TournamentRound.builder().id(1L).multiplier(BigDecimal.ONE).build();

        Event first = Event.builder()
                .id(10L)
                .status(EventStatus.COMPLETED)
                .playerHome(p1)
                .playerAway(p2)
                .homeScore(2)
                .awayScore(0)
                .round(round)
                .build();
        Event second = Event.builder()
                .id(11L)
                .status(EventStatus.COMPLETED)
                .playerHome(p2)
                .playerAway(p1)
                .homeScore(1)
                .awayScore(0)
                .round(round)
                .build();

        when(playerRepository.findByGroupIdOrderByNameAsc(10L)).thenReturn(List.of(p1, p2));
        when(eventRepository.findCompletedNonByeByGroupId(10L, EventStatus.COMPLETED)).thenReturn(List.of(first, second));
        when(playerRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<Player> updated = eloService.recalculateGroupElos(10L);

        BigDecimal afterFirstP1 = applyDelta(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 1.0);
        BigDecimal afterFirstP2 = applyDelta(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 0.0);
        BigDecimal afterSecondP2 = applyDelta(afterFirstP2, expectedScore(afterFirstP2.doubleValue(), afterFirstP1.doubleValue()), 1.0);
        BigDecimal afterSecondP1 = applyDelta(afterFirstP1, expectedScore(afterFirstP1.doubleValue(), afterFirstP2.doubleValue()), 0.0);

        assertEquals(afterSecondP1, p1.getCurrentElo());
        assertEquals(afterSecondP2, p2.getCurrentElo());
        assertEquals(BigDecimal.valueOf(1000), first.getHomeEloBefore());
        assertEquals(BigDecimal.valueOf(1000), first.getAwayEloBefore());
        assertEquals(afterFirstP2, second.getHomeEloBefore());
        assertEquals(afterFirstP1, second.getAwayEloBefore());
        assertEquals(2, updated.size());
    }

    @Test
    void calculateElo_shouldTreatPenaltyWinAsWinNotDraw() {
        Player target = Player.builder().id(1L).name("Target").build();
        Player opponent = Player.builder().id(2L).name("Opp").build();
        TournamentRound round = TournamentRound.builder().id(1L).multiplier(BigDecimal.ONE).build();

        Event penaltyWin = Event.builder()
                .id(12L)
                .status(EventStatus.COMPLETED)
                .playerHome(target)
                .playerAway(opponent)
                .homeScore(0)
                .awayScore(0)
                .penaltiesHome(5)
                .penaltiesAway(4)
                .homeEloBefore(BigDecimal.valueOf(1000))
                .awayEloBefore(BigDecimal.valueOf(1000))
                .round(round)
                .build();

        when(eventRepository.findCompletedNonByeByPlayerId(1L, EventStatus.COMPLETED))
                .thenReturn(List.of(penaltyWin));
        when(playerRepository.save(any(Player.class))).thenReturn(target);

        BigDecimal result = eloService.calculateElo(target);

        BigDecimal expectedElo = applyDelta(BigDecimal.valueOf(1000), expectedScore(1000, 1000), 1.0);
        assertEquals(expectedElo, result);
        assertEquals(expectedElo, target.getCurrentElo());
    }

    private BigDecimal applyDelta(BigDecimal currentElo, double expected, double actual) {
        BigDecimal delta = K_FACTOR.multiply(BigDecimal.valueOf(actual - expected));
        return currentElo.add(delta).setScale(ELO_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal applyDeltaWithK(BigDecimal currentElo, double expected, double actual, BigDecimal effectiveK) {
        BigDecimal delta = effectiveK.multiply(BigDecimal.valueOf(actual - expected));
        return currentElo.add(delta).setScale(ELO_SCALE, RoundingMode.HALF_UP);
    }

    private double expectedScore(double playerElo, double opponentElo) {
        double diff = opponentElo - playerElo;
        double power = Math.pow(10.0, diff / 400.0);
        return 1.0 / (1.0 + power);
    }
}

