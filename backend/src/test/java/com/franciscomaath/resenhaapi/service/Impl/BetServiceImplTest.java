package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.BetItemRequest;
import com.franciscomaath.resenhaapi.controller.dto.request.BetRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.BetSlipResponseDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.WalletResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.BetSlip;
import com.franciscomaath.resenhaapi.domain.entity.BetSlipItem;
import com.franciscomaath.resenhaapi.domain.entity.Event;
import com.franciscomaath.resenhaapi.domain.entity.Group;
import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.entity.Market;
import com.franciscomaath.resenhaapi.domain.entity.Outcome;
import com.franciscomaath.resenhaapi.domain.entity.Player;
import com.franciscomaath.resenhaapi.domain.entity.Tournament;
import com.franciscomaath.resenhaapi.domain.entity.TournamentWallet;
import com.franciscomaath.resenhaapi.domain.entity.Transaction;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.BetSlipItemStatus;
import com.franciscomaath.resenhaapi.domain.enums.BetSlipStatus;
import com.franciscomaath.resenhaapi.domain.enums.MarketStatus;
import com.franciscomaath.resenhaapi.domain.enums.MarketType;
import com.franciscomaath.resenhaapi.domain.enums.TransactionType;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import com.franciscomaath.resenhaapi.domain.exception.BusinessException;
import com.franciscomaath.resenhaapi.domain.exception.InsufficientFundsException;
import com.franciscomaath.resenhaapi.domain.repository.BetSlipItemRepository;
import com.franciscomaath.resenhaapi.domain.repository.BetSlipRepository;
import com.franciscomaath.resenhaapi.domain.repository.EventRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.MarketRepository;
import com.franciscomaath.resenhaapi.domain.repository.OutcomeRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentWalletRepository;
import com.franciscomaath.resenhaapi.domain.repository.TransactionRepository;
import com.franciscomaath.resenhaapi.mapper.BetMapper;
import com.franciscomaath.resenhaapi.mapper.MarketMapper;
import com.franciscomaath.resenhaapi.mapper.OutcomeMapper;
import com.franciscomaath.resenhaapi.mapper.WalletMapper;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BetServiceImplTest {

    @Mock
    private BetSlipRepository betSlipRepository;

    @Mock
    private BetSlipItemRepository betSlipItemRepository;

    @Mock
    private GroupTournamentRepository groupTournamentRepository;

    @Mock
    private TournamentWalletRepository tournamentWalletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private MarketRepository marketRepository;

    @Mock
    private OutcomeRepository outcomeRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private BetMapper betMapper;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private MarketMapper marketMapper;

    @Mock
    private OutcomeMapper outcomeMapper;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @InjectMocks
    private BetServiceImpl betService;

    @Test
    void placeBet_debitsTournamentWalletAndStoresGroupTournament() {
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        Tournament tournament = MultiGroupFixtures.tournament(99L, TournamentType.FIFA_MATCH);
        GroupTournament groupTournament = groupTournament(tournament);
        Event event = event(1L, tournament);
        Market market = market(2L, event, MarketStatus.OPEN);
        Outcome outcome = outcome(3L, market, "Home", new BigDecimal("2.50"));
        TournamentWallet wallet = MultiGroupFixtures.tournamentWallet(4L, groupTournament, user, new BigDecimal("100.00"));
        BetRequestDTO request = request(99L, new BigDecimal("10.00"), 1L, 2L, 3L);

        stubPlacement(user, tournament, groupTournament, event, market, outcome, wallet);
        when(betSlipRepository.save(any(BetSlip.class))).thenAnswer(invocation -> {
            BetSlip slip = invocation.getArgument(0);
            slip.setId(5L);
            return slip;
        });
        when(betMapper.toResponse(any(BetSlip.class))).thenReturn(new BetSlipResponseDTO());
        when(walletMapper.toResponse(wallet)).thenReturn(new WalletResponseDTO());

        betService.placeBet(request);

        ArgumentCaptor<BetSlip> slipCaptor = ArgumentCaptor.forClass(BetSlip.class);
        verify(betSlipRepository).save(slipCaptor.capture());
        BetSlip savedSlip = slipCaptor.getValue();
        assertEquals(groupTournament, savedSlip.getGroupTournament());
        assertEquals(new BigDecimal("10.00"), savedSlip.getStake());
        assertEquals(new BigDecimal("25.00"), savedSlip.getPotentialReturn());

        verify(tournamentWalletRepository).save(wallet);
        assertEquals(new BigDecimal("90.00"), wallet.getBalance());
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals(TransactionType.BET_PLACED, transactionCaptor.getValue().getType());
        assertEquals(wallet, transactionCaptor.getValue().getTournamentWallet());
    }

    @Test
    void placeBet_duplicatePendingCheckIncludesGroupTournamentId() {
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        Tournament tournament = MultiGroupFixtures.tournament(99L, TournamentType.FIFA_MATCH);
        GroupTournament groupTournament = groupTournament(tournament);
        Event event = event(1L, tournament);
        Market market = market(2L, event, MarketStatus.OPEN);
        Outcome outcome = outcome(3L, market, "Home", new BigDecimal("2.50"));
        TournamentWallet wallet = MultiGroupFixtures.tournamentWallet(4L, groupTournament, user, new BigDecimal("100.00"));

        stubPlacement(user, tournament, groupTournament, event, market, outcome, wallet);
        when(betSlipRepository.save(any(BetSlip.class))).thenAnswer(invocation -> {
            BetSlip slip = invocation.getArgument(0);
            slip.setId(5L);
            return slip;
        });
        when(betMapper.toResponse(any(BetSlip.class))).thenReturn(new BetSlipResponseDTO());
        when(walletMapper.toResponse(wallet)).thenReturn(new WalletResponseDTO());

        betService.placeBet(request(99L, new BigDecimal("10.00"), 1L, 2L, 3L));

        verify(betSlipRepository).save(any(BetSlip.class));
    }

    @Test
    void placeBet_insufficientTournamentWalletBalanceThrows() {
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        Tournament tournament = MultiGroupFixtures.tournament(99L, TournamentType.FIFA_MATCH);
        GroupTournament groupTournament = groupTournament(tournament);
        Event event = event(1L, tournament);
        Market market = market(2L, event, MarketStatus.OPEN);
        Outcome outcome = outcome(3L, market, "Home", new BigDecimal("2.50"));
        TournamentWallet wallet = MultiGroupFixtures.tournamentWallet(4L, groupTournament, user, new BigDecimal("5.00"));

        stubPlacement(user, tournament, groupTournament, event, market, outcome, wallet);
        assertThrows(InsufficientFundsException.class, () -> betService.placeBet(request(99L, new BigDecimal("10.00"), 1L, 2L, 3L)));
        verify(betSlipRepository, never()).save(any());
    }

    @Test
    void resolveBetsForEvent_creditsWalletBySlipGroupTournament() {
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        Tournament tournament = MultiGroupFixtures.tournament(99L, TournamentType.FIFA_MATCH);
        GroupTournament groupTournament = groupTournament(tournament);
        Event event = event(1L, tournament);
        event.setHomeScore(2);
        event.setAwayScore(1);
        Market market = market(2L, event, MarketStatus.OPEN);
        Outcome outcome = outcome(3L, market, "Vitória Casa", new BigDecimal("2.50"));
        BetSlip slip = slip(5L, user, groupTournament, BetSlipStatus.PENDING, new BigDecimal("25.00"));
        BetSlipItem item = item(slip, event, outcome, BetSlipItemStatus.PENDING);
        slip.setItems(List.of(item));
        TournamentWallet wallet = MultiGroupFixtures.tournamentWallet(4L, groupTournament, user, new BigDecimal("90.00"));

        when(eventRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(event));
        when(marketRepository.findAllByEventId(1L)).thenReturn(List.of(market));
        when(betSlipItemRepository.findByEventIdAndStatus(1L, BetSlipItemStatus.PENDING)).thenReturn(List.of(item));
        when(betSlipRepository.findById(5L)).thenReturn(Optional.of(slip));
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 1L)).thenReturn(Optional.of(wallet));
        when(marketMapper.toResponse(market)).thenReturn(new com.franciscomaath.resenhaapi.controller.dto.response.MarketResponseDTO());
        when(outcomeRepository.findByMarketIdIn(List.of(2L))).thenReturn(List.of(outcome));

        betService.resolveBetsForEvent(1L);

        assertEquals(BetSlipStatus.WON, slip.getStatus());
        assertEquals(new BigDecimal("115.00"), wallet.getBalance());
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals(TransactionType.BET_WON, transactionCaptor.getValue().getType());
        assertEquals(wallet, transactionCaptor.getValue().getTournamentWallet());
    }

    @Test
    void cancelBetsForEvent_refundsWalletBySlipGroupTournament() {
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        Tournament tournament = MultiGroupFixtures.tournament(99L, TournamentType.FIFA_MATCH);
        GroupTournament groupTournament = groupTournament(tournament);
        Event event = event(1L, tournament);
        Market market = market(2L, event, MarketStatus.OPEN);
        Outcome outcome = outcome(3L, market, "Home", new BigDecimal("2.50"));
        BetSlip slip = slip(5L, user, groupTournament, BetSlipStatus.PENDING, new BigDecimal("25.00"));
        slip.setStake(new BigDecimal("10.00"));
        BetSlipItem item = item(slip, event, outcome, BetSlipItemStatus.PENDING);
        slip.setItems(List.of(item));
        TournamentWallet wallet = MultiGroupFixtures.tournamentWallet(4L, groupTournament, user, new BigDecimal("90.00"));

        when(eventRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(event));
        when(betSlipItemRepository.findByEventIdAndStatus(1L, BetSlipItemStatus.PENDING)).thenReturn(List.of(item));
        when(betSlipRepository.findById(5L)).thenReturn(Optional.of(slip));
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 1L)).thenReturn(Optional.of(wallet));

        betService.cancelBetsForEvent(1L);

        assertEquals(BetSlipStatus.CANCELLED, slip.getStatus());
        assertEquals(new BigDecimal("100.00"), wallet.getBalance());
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals(TransactionType.BET_REFUND, transactionCaptor.getValue().getType());
        assertEquals(wallet, transactionCaptor.getValue().getTournamentWallet());
    }

    @Test
    void reopenBetsForEvent_revertsWinningsAndSetsPending() {
        User user = MultiGroupFixtures.user(1L, "User", UserType.USER);
        Tournament tournament = MultiGroupFixtures.tournament(99L, TournamentType.FIFA_MATCH);
        GroupTournament groupTournament = groupTournament(tournament);
        Event event = event(1L, tournament);
        Market market = market(2L, event, MarketStatus.CLOSED);
        Outcome outcome = outcome(3L, market, "Vitória Casa", new BigDecimal("2.50"));
        BetSlip slip = slip(5L, user, groupTournament, BetSlipStatus.WON, new BigDecimal("25.00"));
        BetSlipItem item = item(slip, event, outcome, BetSlipItemStatus.WON);
        slip.setItems(List.of(item));
        TournamentWallet wallet = MultiGroupFixtures.tournamentWallet(4L, groupTournament, user, new BigDecimal("115.00"));
        Transaction transaction = new Transaction();
        transaction.setId(7L);
        transaction.setTournamentWallet(wallet);
        transaction.setBetSlip(slip);
        transaction.setType(TransactionType.BET_WON);
        transaction.setValue(new BigDecimal("25.00"));

        when(eventRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(event));
        when(betSlipItemRepository.findByEventId(1L)).thenReturn(List.of(item));
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 1L)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByTournamentWalletGroupTournamentId(100L)).thenReturn(List.of(transaction));
        when(walletMapper.toResponse(wallet)).thenReturn(new WalletResponseDTO());

        betService.reopenBetsForEvent(1L);

        assertEquals(BetSlipStatus.PENDING, slip.getStatus());
        assertEquals(BetSlipItemStatus.PENDING, item.getStatus());
        assertEquals(new BigDecimal("90.00"), wallet.getBalance());
        assertEquals(TransactionType.BET_WON, transaction.getType());
        assertNotNull(transaction.getDeletedAt());
        verify(tournamentWalletRepository).save(wallet);
        verify(betSlipRepository).save(slip);
        verify(transactionRepository).save(transaction);
    }

    private void stubPlacement(User user, Tournament tournament, GroupTournament groupTournament, Event event,
                               Market market, Outcome outcome, TournamentWallet wallet) {
        when(currentUserContext.getRequiredUser()).thenReturn(user);
        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(tournamentRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.of(tournament));
        when(groupTournamentRepository.findByTournamentIdAndGroupId(99L, 10L)).thenReturn(Optional.of(groupTournament));
        when(eventRepository.findAllById(List.of(1L))).thenReturn(List.of(event));
        when(marketRepository.findById(2L)).thenReturn(Optional.of(market));
        when(outcomeRepository.findById(3L)).thenReturn(Optional.of(outcome));
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 1L)).thenReturn(Optional.of(wallet));
    }

    private BetRequestDTO request(Long tournamentId, BigDecimal stake, Long eventId, Long marketId, Long outcomeId) {
        BetItemRequest itemRequest = new BetItemRequest();
        itemRequest.setEventId(eventId);
        itemRequest.setMarketId(marketId);
        itemRequest.setOutcomeId(outcomeId);
        BetRequestDTO request = new BetRequestDTO();
        request.setTournamentId(tournamentId);
        request.setStake(stake);
        request.setItems(List.of(itemRequest));
        return request;
    }

    private GroupTournament groupTournament(Tournament tournament) {
        Group group = MultiGroupFixtures.group(10L, "Group");
        return MultiGroupFixtures.groupTournament(100L, group, tournament);
    }

    private Event event(Long id, Tournament tournament) {
        Event event = new Event();
        event.setId(id);
        event.setTournament(tournament);
        event.setPlayerHome(Player.builder().id(1L).name("Home").build());
        event.setPlayerAway(Player.builder().id(2L).name("Away").build());
        return event;
    }

    private Market market(Long id, Event event, MarketStatus status) {
        Market market = new Market();
        market.setId(id);
        market.setEvent(event);
        market.setMarketType(MarketType.MATCH_RESULT);
        market.setStatus(status);
        return market;
    }

    private Outcome outcome(Long id, Market market, String name, BigDecimal odd) {
        Outcome outcome = new Outcome();
        outcome.setId(id);
        outcome.setMarket(market);
        outcome.setName(name);
        outcome.setOdd(odd);
        return outcome;
    }

    private BetSlip slip(Long id, User user, GroupTournament groupTournament, BetSlipStatus status, BigDecimal potentialReturn) {
        BetSlip slip = new BetSlip();
        slip.setId(id);
        slip.setUser(user);
        slip.setGroupTournament(groupTournament);
        slip.setStatus(status);
        slip.setPotentialReturn(potentialReturn);
        return slip;
    }

    private BetSlipItem item(BetSlip slip, Event event, Outcome outcome, BetSlipItemStatus status) {
        BetSlipItem item = new BetSlipItem();
        item.setBetSlip(slip);
        item.setEvent(event);
        item.setOutcome(outcome);
        item.setOddSnapshot(outcome.getOdd());
        item.setStatus(status);
        return item;
    }
}
