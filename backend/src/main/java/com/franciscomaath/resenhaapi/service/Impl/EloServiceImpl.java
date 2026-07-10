package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.enums.EventStatus;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.PlayerRepository;
import com.franciscomaath.resenhaapi.service.EloService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class EloServiceImpl implements EloService {

    private static final BigDecimal INITIAL_ELO = BigDecimal.valueOf(1000);
    private static final BigDecimal K_FACTOR = BigDecimal.valueOf(32);
    private static final int ELO_SCALE = 2;

    private final EventRepository eventRepository;
    private final PlayerRepository playerRepository;

    @Override
    @Transactional
    public BigDecimal calculateElo(Player player) {
        List<Event> events = eventRepository.findCompletedNonByeByPlayerId(player.getId(), EventStatus.COMPLETED);
        BigDecimal currentElo = INITIAL_ELO;

        for (Event event : events) {
            if (event.getHomeScore() == null || event.getAwayScore() == null) {
                continue;
            }

            boolean isHome = player.getId().equals(event.getPlayerHome().getId());
            BigDecimal opponentEloBefore = resolveOpponentEloBefore(event, isHome);
            double expected = expectedScore(currentElo, opponentEloBefore);
            double actual = actualScore(event, isHome);
            BigDecimal effectiveK = getEffectiveKFactor(event);

            currentElo = applyDelta(currentElo, expected, actual, effectiveK);
        }

        player.setCurrentElo(currentElo);
        playerRepository.save(player);
        return currentElo;
    }

    @Override
    @Transactional
    public BigDecimal applyEloForEvent(Event event) {
        if (event.getHomeScore() == null || event.getAwayScore() == null) {
            return null;
        }

        Player home = event.getPlayerHome();
        Player away = event.getPlayerAway();

        BigDecimal homeEloBefore = event.getHomeEloBefore();
        BigDecimal awayEloBefore = event.getAwayEloBefore();

        if (homeEloBefore == null) {
            homeEloBefore = home.getCurrentElo() != null ? home.getCurrentElo() : INITIAL_ELO;
        }
        if (awayEloBefore == null) {
            awayEloBefore = away.getCurrentElo() != null ? away.getCurrentElo() : INITIAL_ELO;
        }

        double expectedHome = expectedScore(homeEloBefore, awayEloBefore);
        double expectedAway = expectedScore(awayEloBefore, homeEloBefore);
        double actualHome = actualScore(event, true);
        double actualAway = actualScore(event, false);
        BigDecimal effectiveK = getEffectiveKFactor(event);

        BigDecimal homeEloAfter = applyDelta(homeEloBefore, expectedHome, actualHome, effectiveK);
        BigDecimal awayEloAfter = applyDelta(awayEloBefore, expectedAway, actualAway, effectiveK);

        home.setCurrentElo(homeEloAfter);
        away.setCurrentElo(awayEloAfter);
        playerRepository.save(home);
        playerRepository.save(away);

        return homeEloAfter;
    }

    @Override
    @Transactional
    public List<Player> recalculateGroupElos(Long groupId) {
        List<Player> players = playerRepository.findByGroupIdOrderByNameAsc(groupId);
        Map<Long, BigDecimal> elosByPlayerId = new HashMap<>();

        for (Player player : players) {
            player.setCurrentElo(INITIAL_ELO);
            elosByPlayerId.put(player.getId(), INITIAL_ELO);
        }

        List<Event> events = eventRepository.findCompletedNonByeByGroupId(groupId, EventStatus.COMPLETED);
        for (Event event : events) {
            if (event.getHomeScore() == null || event.getAwayScore() == null) {
                continue;
            }

            Player home = event.getPlayerHome();
            Player away = event.getPlayerAway();
            if (home == null || away == null) {
                continue;
            }

            BigDecimal homeEloBefore = elosByPlayerId.getOrDefault(home.getId(), INITIAL_ELO);
            BigDecimal awayEloBefore = elosByPlayerId.getOrDefault(away.getId(), INITIAL_ELO);

            event.setHomeEloBefore(homeEloBefore);
            event.setAwayEloBefore(awayEloBefore);

            double expectedHome = expectedScore(homeEloBefore, awayEloBefore);
            double expectedAway = expectedScore(awayEloBefore, homeEloBefore);
            double actualHome = actualScore(event, true);
            double actualAway = actualScore(event, false);
            BigDecimal effectiveK = getEffectiveKFactor(event);

            BigDecimal homeEloAfter = applyDelta(homeEloBefore, expectedHome, actualHome, effectiveK);
            BigDecimal awayEloAfter = applyDelta(awayEloBefore, expectedAway, actualAway, effectiveK);

            home.setCurrentElo(homeEloAfter);
            away.setCurrentElo(awayEloAfter);
            elosByPlayerId.put(home.getId(), homeEloAfter);
            elosByPlayerId.put(away.getId(), awayEloAfter);
        }

        playerRepository.saveAll(players);
        eventRepository.saveAll(events);
        return players;
    }

    private BigDecimal resolveOpponentEloBefore(Event event, boolean isHome) {
        BigDecimal stored = isHome ? event.getAwayEloBefore() : event.getHomeEloBefore();
        if (stored != null) {
            return stored;
        }

        Player opponent = isHome ? event.getPlayerAway() : event.getPlayerHome();
        return opponent.getCurrentElo() != null ? opponent.getCurrentElo() : INITIAL_ELO;
    }

    private double expectedScore(BigDecimal playerElo, BigDecimal opponentElo) {
        double diff = opponentElo.doubleValue() - playerElo.doubleValue();
        double power = Math.pow(10.0, diff / 400.0);
        return 1.0 / (1.0 + power);
    }

    private double actualScore(Event event, boolean isHome) {
        int homeScore = event.getHomeScore();
        int awayScore = event.getAwayScore();
        if (homeScore == awayScore) {
            if (event.getPenaltiesHome() != null && event.getPenaltiesAway() != null
                    && !event.getPenaltiesHome().equals(event.getPenaltiesAway())) {
                boolean homeWonPenalties = event.getPenaltiesHome() > event.getPenaltiesAway();
                if (isHome) {
                    return homeWonPenalties ? 1.0 : 0.0;
                }
                return homeWonPenalties ? 0.0 : 1.0;
            }
            return 0.5;
        }
        boolean homeWon = homeScore > awayScore;
        if (isHome) {
            return homeWon ? 1.0 : 0.0;
        }
        return homeWon ? 0.0 : 1.0;
    }

    private BigDecimal getEffectiveKFactor(Event event) {
        BigDecimal multiplier = BigDecimal.ONE;
        if (event.getRound() != null && event.getRound().getMultiplier() != null) {
            multiplier = event.getRound().getMultiplier();
        }
        return K_FACTOR.multiply(multiplier);
    }

    private BigDecimal applyDelta(BigDecimal currentElo, double expected, double actual, BigDecimal effectiveK) {
        BigDecimal delta = effectiveK.multiply(BigDecimal.valueOf(actual - expected));
        return currentElo.add(delta).setScale(ELO_SCALE, RoundingMode.HALF_UP);
    }
}
