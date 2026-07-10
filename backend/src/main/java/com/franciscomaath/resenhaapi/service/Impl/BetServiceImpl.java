package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.domain.event.MarketChangeEvent;
import com.franciscomaath.resenhaapi.domain.event.WalletChangeEvent;
import com.franciscomaath.resenhaapi.controller.dto.request.BetItemRequest;
import com.franciscomaath.resenhaapi.controller.dto.request.BetRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.BetSlipResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.WalletResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.*;
import com.franciscomaath.resenhaapi.domain.enums.*;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.exception.InsufficientFundsException;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.repository.*;
import com.franciscomaath.resenhaapi.mapper.BetMapper;
import com.franciscomaath.resenhaapi.mapper.MarketMapper;
import com.franciscomaath.resenhaapi.mapper.OutcomeMapper;
import com.franciscomaath.resenhaapi.mapper.WalletMapper;
import com.franciscomaath.resenhaapi.service.BetService;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BetServiceImpl implements BetService {

    private final BetSlipRepository betSlipRepository;
    private final BetSlipItemRepository betSlipItemRepository;
    private final GroupTournamentRepository groupTournamentRepository;
    private final TournamentWalletRepository tournamentWalletRepository;
    private final TransactionRepository transactionRepository;
    private final MarketRepository marketRepository;
    private final OutcomeRepository outcomeRepository;
    private final EventRepository eventRepository;
    private final TournamentRepository tournamentRepository;
    private final BetMapper betMapper;
    private final WalletMapper walletMapper;
    private final MarketMapper marketMapper;
    private final OutcomeMapper outcomeMapper;
    private final CurrentUserContext currentUserContext;
    private final ApplicationEventPublisher eventPublisher;
    private final GroupAuthorizationService groupAuthorizationService;

    @Override
    @Transactional
    public BetSlipResponseDTO placeBet(BetRequestDTO dto) {
        User user = currentUserContext.getRequiredUser();

        Tournament tournament = tournamentRepository.findByIdAndDeletedAtIsNull(dto.getTournamentId())
                .orElseThrow(() -> new ResourceNotFoundException("Torneio nao encontrado."));
        GroupTournament groupTournament = groupTournamentRepository.findByTournamentIdAndGroupId(
                        tournament.getId(), currentUserContext.getRequiredGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("GroupTournament", "tournamentId", tournament.getId()));

        if (dto.getItems().isEmpty()) {
            throw new BusinessException("A aposta deve conter pelo menos uma selecao.");
        }

        List<Long> eventIds = dto.getItems().stream()
                .map(BetItemRequest::getEventId)
                .toList();

        List<Event> events = eventRepository.findAllById(eventIds);
        if (!events.stream().allMatch(e -> eventIds.contains(e.getId()))) {
            throw new BusinessException("Um ou mais eventos nao foram encontrados.");
        }

        for (Event event : events) {
            if (!event.getTournament().getId().equals(dto.getTournamentId())) {
                throw new BusinessException("Todos os eventos devem pertencer ao mesmo torneio.");
            }
        }

        List<BetSlipItem> items = new ArrayList<>();
        for (BetItemRequest itemReq : dto.getItems()) {
            Event event = events.stream()
                    .filter(e -> e.getId().equals(itemReq.getEventId()))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado."));

            Market market = marketRepository.findById(itemReq.getMarketId())
                    .orElseThrow(() -> new ResourceNotFoundException("Market nao encontrado."));

            if(market.getEvent() == null || !market.getEvent().getId().equals(event.getId())) {
                throw new BusinessException("O market informado nao pertence ao evento " + event.getId());
            }

            if (market.getStatus() != MarketStatus.OPEN) {
                throw new BusinessException("O market " + itemReq.getMarketId() + " do evento " + event.getId() + " nao esta aberto.");
            }

            Outcome outcome = outcomeRepository.findById(itemReq.getOutcomeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Outcome nao encontrado."));

            if(tournament.getType() == TournamentType.FIFA_MATCH){
                if(event.getPlayerHome().getUser() != null &&
                        event.getPlayerHome().getUser().getId().equals(user.getId()) &&
                        outcome.getName().equals(event.getPlayerAway().getName())) {
                    throw new BusinessException("Voce nao pode apostar contra si mesmo no torneio FIFA Match.");
                }

                if(event.getPlayerAway().getUser() != null &&
                        event.getPlayerAway().getUser().getId().equals(user.getId()) &&
                        outcome.getName().equals(event.getPlayerHome().getName())) {
                    throw new BusinessException("Voce nao pode apostar contra si mesmo no torneio FIFA Match.");
                }
            }

            if (!outcome.getMarket().getId().equals(market.getId())) {
                throw new BusinessException("O outcome informado nao pertence ao market do evento " + event.getId());
            }

            if (items.stream().anyMatch(item -> item.getOutcome().getMarket().getId().equals(market.getId()))) {
                throw new BusinessException("Já existe uma aposta nesse market neste bilhete.");
            }

            if (isFifaOpponentBet(user, event, market, outcome)) {
                throw new BusinessException("Voce nao pode apostar contra si mesmo no torneio FIFA Match.");
            }

            BetSlipItem item = new BetSlipItem();
            item.setEvent(event);
            item.setOutcome(outcome);
            item.setOddSnapshot(outcome.getOdd());
            item.setStatus(BetSlipItemStatus.PENDING);
            items.add(item);
        }

        BigDecimal stake = dto.getStake().setScale(2, RoundingMode.HALF_UP);
        if (stake.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("O valor da aposta deve ser maior que zero.");
        }

        TournamentWallet wallet = tournamentWalletRepository.findByGroupTournamentIdAndUserId(groupTournament.getId(), user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Carteira nao encontrada."));

        if (wallet.getBalance().compareTo(stake) < 0) {
            throw new InsufficientFundsException("Saldo insuficiente.", stake, wallet.getBalance());
        }

        BigDecimal combinedOdd = items.stream()
                .map(BetSlipItem::getOddSnapshot)
                .reduce(BigDecimal.ONE, BigDecimal::multiply)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal potentialReturn = stake.multiply(combinedOdd).setScale(2, RoundingMode.HALF_UP);

        BetSlip betSlip = new BetSlip();
        betSlip.setUser(user);
        betSlip.setGroupTournament(groupTournament);
        betSlip.setStake(stake);
        betSlip.setCombinedOdd(combinedOdd);
        betSlip.setPotentialReturn(potentialReturn);
        betSlip.setStatus(BetSlipStatus.PENDING);
        betSlip.setCreatedAt(LocalDateTime.now());

        betSlip = betSlipRepository.save(betSlip);

        for (BetSlipItem item : items) {
            item.setBetSlip(betSlip);
        }
        betSlipItemRepository.saveAll(items);
        betSlip.setItems(items);

        wallet.setBalance(wallet.getBalance().subtract(stake));
        tournamentWalletRepository.save(wallet);
        publishWalletChange(wallet);

        Transaction transaction = new Transaction();
        transaction.setTournamentWallet(wallet);
        transaction.setBetSlip(betSlip);
        transaction.setType(TransactionType.BET_PLACED);
        transaction.setValue(stake);
        transactionRepository.save(transaction);

        return betMapper.toResponse(betSlip);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BetSlipResponseDTO> getUserBets() {
        User user = currentUserContext.getRequiredUser();
        List<BetSlip> betSlips = betSlipRepository.findByUserIdAndGroupTournamentGroupIdOrderByCreatedAtDesc(
                user.getId(), currentUserContext.getRequiredGroupId());
        return betSlips.stream()
                .map(betMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BetSlipResponseDTO> getBetsByEvent(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado."));
        groupAuthorizationService.requireTournamentAdmin(event.getTournament().getId());
        List<BetSlip> betSlips = betSlipRepository.findByEventIdAndGroupId(eventId, currentUserContext.getRequiredGroupId());
        return betSlips.stream()
                .map(betMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void resolveBetsForEvent(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado."));

        int homeScore = event.getHomeScore() != null ? event.getHomeScore() : 0;
        int awayScore = event.getAwayScore() != null ? event.getAwayScore() : 0;
        boolean isKnockout = event.getIsKnockout() != null && event.getIsKnockout();

        String homeName = event.getHomeName();
        String awayName = event.getAwayName();

        List<Market> markets = marketRepository.findAllByEventId(event.getId());

        if(markets.isEmpty()){
            throw new ResourceNotFoundException("No markets found for this event.");
        }

        Map<MarketType, Market> marketByType = markets.stream()
                .collect(Collectors.toMap(Market::getMarketType, market -> market, (left, right) -> left, () -> new EnumMap<>(MarketType.class)));

        Map<Long, List<Outcome>> outcomesByMarketId = outcomeRepository.findByMarketIdIn(
                        markets.stream().map(Market::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(outcome -> outcome.getMarket().getId()));

        Map<MarketType, String> winningNameByType = buildWinningNamesByType(
                event, homeScore, awayScore, isKnockout, homeName, awayName, marketByType, outcomesByMarketId);

        List<BetSlipItem> pendingItems = betSlipItemRepository.findByEventIdAndStatus(
                event.getId(), BetSlipItemStatus.PENDING);

        Set<Long> affectedBetSlipIds = new HashSet<>();

        for (BetSlipItem item : pendingItems) {
            MarketType marketType = item.getOutcome().getMarket().getMarketType();
            String winningName = winningNameByType.get(marketType);
            if (winningName == null) {
                continue;
            }
            if (item.getOutcome().getName().equals(winningName)) {
                item.setStatus(BetSlipItemStatus.WON);
            } else {
                item.setStatus(BetSlipItemStatus.LOST);
            }
            betSlipItemRepository.save(item);
            affectedBetSlipIds.add(item.getBetSlip().getId());
        }

        for (Long slipId : affectedBetSlipIds) {
            BetSlip betSlip = betSlipRepository.findById(slipId)
                    .orElseThrow(() -> new ResourceNotFoundException("BetSlip nao encontrado."));

            if (betSlip.getStatus() != BetSlipStatus.PENDING) {
                continue;
            }

            boolean hasLost = betSlip.getItems().stream()
                    .anyMatch(i -> i.getStatus() == BetSlipItemStatus.LOST);
            boolean allResolved = betSlip.getItems().stream()
                    .noneMatch(i -> i.getStatus() == BetSlipItemStatus.PENDING);

            if (hasLost) {
                betSlip.setStatus(BetSlipStatus.LOST);
                betSlipRepository.save(betSlip);
            } else if (allResolved) {
                betSlip.setStatus(BetSlipStatus.WON);

                TournamentWallet wallet = tournamentWalletRepository.findByGroupTournamentIdAndUserId(
                                betSlip.getGroupTournament().getId(), betSlip.getUser().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Carteira nao encontrada."));

                wallet.setBalance(wallet.getBalance().add(betSlip.getPotentialReturn()));
                tournamentWalletRepository.save(wallet);
                publishWalletChange(wallet);

                Transaction transaction = new Transaction();
                transaction.setTournamentWallet(wallet);
                transaction.setBetSlip(betSlip);
                transaction.setType(TransactionType.BET_WON);
                transaction.setValue(betSlip.getPotentialReturn());
                transactionRepository.save(transaction);

                betSlipRepository.save(betSlip);
            }
        }

        markets.forEach(m -> m.setStatus(MarketStatus.CLOSED));
        marketRepository.saveAll(markets);
        publishMarketChanges(markets);
    }

    @Override
    @Transactional
    public void cancelBetsForEvent(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado."));

        List<BetSlipItem> pendingItems = betSlipItemRepository.findByEventIdAndStatus(
                event.getId(), BetSlipItemStatus.PENDING);

        if (pendingItems.isEmpty()) {
            return;
        }

        Set<Long> affectedBetSlipIds = new HashSet<>();
        for (BetSlipItem item : pendingItems) {
            item.setStatus(BetSlipItemStatus.CANCELLED);
            betSlipItemRepository.save(item);
            affectedBetSlipIds.add(item.getBetSlip().getId());
        }

        for (Long slipId : affectedBetSlipIds) {
            BetSlip betSlip = betSlipRepository.findById(slipId)
                    .orElseThrow(() -> new ResourceNotFoundException("BetSlip nao encontrado."));

            if (betSlip.getStatus() != BetSlipStatus.PENDING) {
                continue;
            }

            boolean hasRemainingPending = betSlip.getItems().stream()
                    .anyMatch(i -> i.getStatus() == BetSlipItemStatus.PENDING);
            boolean hasCancelled = betSlip.getItems().stream()
                    .anyMatch(i -> i.getStatus() == BetSlipItemStatus.CANCELLED);

            if (!hasRemainingPending && hasCancelled) {
                betSlip.setStatus(BetSlipStatus.CANCELLED);
                betSlipRepository.save(betSlip);

                TournamentWallet wallet = tournamentWalletRepository.findByGroupTournamentIdAndUserId(
                                betSlip.getGroupTournament().getId(), betSlip.getUser().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Carteira nao encontrada."));

                wallet.setBalance(wallet.getBalance().add(betSlip.getStake()));
                tournamentWalletRepository.save(wallet);
                publishWalletChange(wallet);

                Transaction transaction = new Transaction();
                transaction.setTournamentWallet(wallet);
                transaction.setBetSlip(betSlip);
                transaction.setType(TransactionType.BET_REFUND);
                transaction.setValue(betSlip.getStake());
                transactionRepository.save(transaction);
            }
        }
    }

    @Override
    @Transactional
    public void reopenBetsForEvent(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedAtIsNull(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento nao encontrado."));

        List<BetSlipItem> items = betSlipItemRepository.findByEventId(event.getId());
        if (items.isEmpty()) {
            return;
        }

        Set<BetSlip> affectedSlips = new HashSet<>();
        for (BetSlipItem item : items) {
            item.setStatus(BetSlipItemStatus.PENDING);
            betSlipItemRepository.save(item);
            if (item.getBetSlip() != null) {
                affectedSlips.add(item.getBetSlip());
            }
        }

        for (BetSlip betSlip : affectedSlips) {
            BetSlipStatus previousStatus = betSlip.getStatus();
            boolean hasLost = betSlip.getItems().stream().anyMatch(i -> i.getStatus() == BetSlipItemStatus.LOST);
            boolean hasPending = betSlip.getItems().stream().anyMatch(i -> i.getStatus() == BetSlipItemStatus.PENDING);

            BetSlipStatus newStatus = hasLost ? BetSlipStatus.LOST : (hasPending ? BetSlipStatus.PENDING : BetSlipStatus.WON);

            if (previousStatus == BetSlipStatus.WON && newStatus != BetSlipStatus.WON) {
                reverseWinningTransactions(betSlip, eventId);
            }

            betSlip.setStatus(newStatus);
            betSlipRepository.save(betSlip);
        }
    }

    private void reverseWinningTransactions(BetSlip betSlip, Long eventId) {
        TournamentWallet wallet = tournamentWalletRepository.findByGroupTournamentIdAndUserId(
                        betSlip.getGroupTournament().getId(), betSlip.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Carteira nao encontrada."));

        List<Transaction> transactions = transactionRepository.findByTournamentWalletGroupTournamentId(
                        betSlip.getGroupTournament().getId())
                .stream()
                .filter(tx -> tx.getBetSlip() != null && tx.getBetSlip().getId().equals(betSlip.getId()))
                .filter(tx -> tx.getType() == TransactionType.BET_WON)
                .filter(tx -> tx.getDeletedAt() == null)
                .toList();

        BigDecimal payoutToReverse = transactions.stream()
                .map(Transaction::getValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (payoutToReverse.compareTo(BigDecimal.ZERO) == 0) {
            payoutToReverse = betSlip.getPotentialReturn() != null ? betSlip.getPotentialReturn() : BigDecimal.ZERO;
        }

        wallet.setBalance(wallet.getBalance().subtract(payoutToReverse));
        tournamentWalletRepository.save(wallet);
        publishWalletChange(wallet);

        for (Transaction transaction : transactions) {
            transaction.softDelete();
            transactionRepository.save(transaction);
        }
    }

    private void publishWalletChange(TournamentWallet wallet) {
        WalletResponseDTO dto = walletMapper.toResponse(wallet);
        eventPublisher.publishEvent(new WalletChangeEvent(this, wallet.getUser().getId(), dto));
    }

    private void publishMarketChange(Market market) {
        List<Outcome> outcomes = outcomeRepository.findByMarketId(market.getId());
        MarketResponseDTO dto = marketMapper.toResponse(market);
        dto.setOutcomes(outcomes.stream().map(outcomeMapper::toResponse).toList());
        eventPublisher.publishEvent(new MarketChangeEvent(this, market.getEvent().getId(), dto));
    }

    private void publishMarketChanges(List<Market> markets) {
        if (markets == null || markets.isEmpty()) {
            return;
        }

        List<Long> marketIds = markets.stream()
                .map(Market::getId)
                .toList();

        List<Outcome> allOutcomes = outcomeRepository.findByMarketIdIn(marketIds);

        Map<Long, List<Outcome>> outcomesByMarket = allOutcomes.stream()
                .collect(Collectors.groupingBy(o -> o.getMarket().getId()));

        List<MarketResponseDTO> dtos = markets.stream().map(market -> {
            MarketResponseDTO dto = marketMapper.toResponse(market);

            List<Outcome> marketOutcomes = outcomesByMarket.getOrDefault(market.getId(), Collections.emptyList());

            dto.setOutcomes(marketOutcomes.stream().map(outcomeMapper::toResponse).toList());
            return dto;
        }).toList();

        for (int i = 0; i < markets.size(); i++) {
            eventPublisher.publishEvent(new MarketChangeEvent(this, markets.get(i).getEvent().getId(), dtos.get(i)));
        }
    }

    private Map<MarketType, String> buildWinningNamesByType(Event event, int homeScore, int awayScore, boolean isKnockout, String homeName, String awayName, Map<MarketType, Market> marketsByType, Map<Long, List<Outcome>> outcomesByMarketId) {
        Map<MarketType, String> map = new EnumMap<>(MarketType.class);

        if (homeScore > awayScore) {
            map.put(MarketType.MATCH_RESULT, "Vitória Casa");
        } else if (homeScore == awayScore) {
            map.put(MarketType.MATCH_RESULT, "Empate");
        } else {
            map.put(MarketType.MATCH_RESULT, "Vitória Fora");
        }

        int totalGoals = homeScore + awayScore;
        map.put(MarketType.OVER_UNDER_25, totalGoals >= 3 ? "Acima de 2.5" : "Abaixo de 2.5");
        map.put(MarketType.OVER_UNDER_35, totalGoals >= 4 ? "Acima de 3.5" : "Abaixo de 3.5");
        map.put(MarketType.BTTS, (homeScore > 0 && awayScore > 0) ? "Ambas Marcam - Sim" : "Ambas Marcam - Não");

        Market exactScoreMarket = marketsByType.get(MarketType.EXACT_SCORE);
        String exactScore = homeScore + "-" + awayScore;
        boolean hasExactOutcome = exactScoreMarket != null
                && outcomesByMarketId.getOrDefault(exactScoreMarket.getId(), List.of()).stream()
                .anyMatch(outcome -> outcome.getName().equals(exactScore));
        map.put(MarketType.EXACT_SCORE, hasExactOutcome ? exactScore : "Outro Placar");

        if (isKnockout) {
            String qualifyWinner;
            if (homeScore > awayScore) {
                qualifyWinner = homeName + " avança";
            } else if (awayScore > homeScore) {
                qualifyWinner = awayName + " avança";
            } else {
                Integer penaltiesHome = event.getPenaltiesHome();
                Integer penaltiesAway = event.getPenaltiesAway();
                if (penaltiesHome == null || penaltiesAway == null) {
                    throw new BusinessException("Penalty scores must be set for knockout event resolution.");
                }
                qualifyWinner = penaltiesHome > penaltiesAway ? homeName + " avança" : awayName + " avança";
            }
            map.put(MarketType.QUALIFY, qualifyWinner);
        }

        return map;
    }

    private boolean isFifaOpponentBet(User user, Event event, Market market, Outcome outcome) {
        if (event.getTournament() == null || event.getTournament().getType() != TournamentType.FIFA_MATCH) {
            return false;
        }

        if (event.getPlayerHome() == null || event.getPlayerAway() == null) {
            return false;
        }

        Long homeUserId = event.getPlayerHome().getUser() != null ? event.getPlayerHome().getUser().getId() : null;
        Long awayUserId = event.getPlayerAway().getUser() != null ? event.getPlayerAway().getUser().getId() : null;

        if (homeUserId != null && homeUserId.equals(user.getId())) {
            return market.getMarketType() == MarketType.MATCH_RESULT && "Vitória Fora".equals(outcome.getName())
                    || market.getMarketType() == MarketType.QUALIFY && (event.getPlayerAway().getName() + " avança").equals(outcome.getName());
        }

        if (awayUserId != null && awayUserId.equals(user.getId())) {
            return market.getMarketType() == MarketType.MATCH_RESULT && "Vitória Casa".equals(outcome.getName())
                    || market.getMarketType() == MarketType.QUALIFY && (event.getPlayerHome().getName() + " avança").equals(outcome.getName());
        }

        return false;
    }
}
