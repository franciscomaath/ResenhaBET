package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.controller.dto.request.WalletDepositRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.WalletResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Group;
import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.entity.TournamentWallet;
import com.franciscomaath.resenhaapi.domain.entity.Transaction;
import com.franciscomaath.resenhaapi.domain.entity.User;
import com.franciscomaath.resenhaapi.domain.enums.TransactionType;
import com.franciscomaath.resenhaapi.domain.enums.TournamentType;
import com.franciscomaath.resenhaapi.domain.enums.UserType;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.exception.UnauthorizedException;
import com.franciscomaath.resenhaapi.domain.exception.ValidationException;
import com.franciscomaath.resenhaapi.domain.repository.GroupMemberRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentWalletRepository;
import com.franciscomaath.resenhaapi.domain.repository.TransactionRepository;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CurrentUserContext currentUserContext;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private GroupTournamentRepository groupTournamentRepository;

    @Mock
    private TournamentWalletRepository tournamentWalletRepository;

    @Mock
    private GroupAuthorizationService groupAuthorizationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GroupMemberRepository groupMemberRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void deposit_addsBalanceToTournamentWallet() {
        User user = createUser(1L);
        GroupTournament groupTournament = groupTournament();
        TournamentWallet wallet = MultiGroupFixtures.tournamentWallet(1L, groupTournament, user, new BigDecimal("100.00"));
        WalletDepositRequestDTO dto = depositRequest(1L, 99L, new BigDecimal("50.00"));

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(groupTournamentRepository.findByTournamentIdAndGroupId(99L, 10L)).thenReturn(Optional.of(groupTournament));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 1L)).thenReturn(Optional.of(wallet));
        when(walletMapper.toResponse(wallet)).thenReturn(new WalletResponseDTO());

        BigDecimal result = walletService.deposit(dto);

        assertEquals(new BigDecimal("150.00"), result);
        assertEquals(new BigDecimal("150.00"), wallet.getBalance());
        verify(groupAuthorizationService).requireTournamentAdmin(99L);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction transaction = captor.getValue();
        assertEquals(TransactionType.DEPOSIT, transaction.getType());
        assertEquals(new BigDecimal("50.00"), transaction.getValue());
        assertEquals(wallet, transaction.getTournamentWallet());
    }

    @Test
    void deposit_createsMissingTournamentWalletForActiveGroupMember() {
        User user = createUser(1L);
        GroupTournament groupTournament = groupTournament();
        WalletDepositRequestDTO dto = depositRequest(1L, 99L, new BigDecimal("50.00"));

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(groupTournamentRepository.findByTournamentIdAndGroupId(99L, 10L)).thenReturn(Optional.of(groupTournament));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(true);
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 1L)).thenReturn(Optional.empty());
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(tournamentWalletRepository.save(any(TournamentWallet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(walletMapper.toResponse(any(TournamentWallet.class))).thenReturn(new WalletResponseDTO());

        BigDecimal result = walletService.deposit(dto);

        assertEquals(new BigDecimal("50.00"), result);
        ArgumentCaptor<TournamentWallet> walletCaptor = ArgumentCaptor.forClass(TournamentWallet.class);
        verify(tournamentWalletRepository, times(2)).save(walletCaptor.capture());
        assertEquals(groupTournament, walletCaptor.getValue().getGroupTournament());
        assertEquals(user, walletCaptor.getValue().getUser());
    }

    @Test
    void deposit_rejectsUserOutsideActiveGroup() {
        GroupTournament groupTournament = groupTournament();
        WalletDepositRequestDTO dto = depositRequest(1L, 99L, new BigDecimal("50.00"));

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(groupTournamentRepository.findByTournamentIdAndGroupId(99L, 10L)).thenReturn(Optional.of(groupTournament));
        when(groupMemberRepository.existsByGroupIdAndUserId(10L, 1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> walletService.deposit(dto));
        verify(tournamentWalletRepository, never()).findByGroupTournamentIdAndUserId(any(), any());
    }

    @Test
    void depositAll_addsToWalletsForCurrentGroupTournamentOnly() {
        User user1 = createUser(1L);
        User user2 = createUser(2L);
        GroupTournament groupTournament = groupTournament();
        TournamentWallet wallet1 = MultiGroupFixtures.tournamentWallet(1L, groupTournament, user1, new BigDecimal("100.00"));
        TournamentWallet wallet2 = MultiGroupFixtures.tournamentWallet(2L, groupTournament, user2, new BigDecimal("200.00"));

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(groupTournamentRepository.findByTournamentIdAndGroupId(99L, 10L)).thenReturn(Optional.of(groupTournament));
        when(tournamentWalletRepository.findByGroupTournamentId(100L))
                .thenReturn(List.of(wallet1, wallet2))
                .thenReturn(List.of(wallet1, wallet2));
        when(walletMapper.toResponse(any(TournamentWallet.class))).thenReturn(new WalletResponseDTO());

        walletService.depositAll(99L, new BigDecimal("25.00"));

        verify(groupAuthorizationService).requireTournamentAdmin(99L);
        verify(tournamentWalletRepository).addAllBalances(100L, new BigDecimal("25.00"));
        ArgumentCaptor<List<Transaction>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(transactionRepository).saveAll(captor.capture());
        @SuppressWarnings("unchecked")
        List<Transaction> transactions = captor.getValue();
        assertEquals(2, transactions.size());
        assertEquals(wallet1, transactions.get(0).getTournamentWallet());
        assertEquals(wallet2, transactions.get(1).getTournamentWallet());
    }

    @Test
    void me_returnsTournamentWalletByUserIdAndTournamentId() {
        User user = createUser(1L);
        GroupTournament groupTournament = groupTournament();
        TournamentWallet wallet = MultiGroupFixtures.tournamentWallet(1L, groupTournament, user, new BigDecimal("200.00"));
        WalletResponseDTO dto = new WalletResponseDTO();
        dto.setUserId(1L);
        dto.setTournamentId(99L);
        dto.setBalance(new BigDecimal("200.00"));

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(currentUserContext.getRequiredUser()).thenReturn(user);
        when(groupTournamentRepository.findByTournamentIdAndGroupId(99L, 10L)).thenReturn(Optional.of(groupTournament));
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 1L)).thenReturn(Optional.of(wallet));
        when(walletMapper.toResponse(wallet)).thenReturn(dto);

        WalletResponseDTO result = walletService.me(1L, 99L);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals(99L, result.getTournamentId());
        assertEquals(new BigDecimal("200.00"), result.getBalance());
        verify(groupAuthorizationService, never()).requireTournamentAdmin(99L);
    }

    @Test
    void me_allowsGroupAdminToReadAnotherUsersTournamentWallet() {
        User currentUser = createUser(1L);
        User targetUser = createUser(2L);
        GroupTournament groupTournament = groupTournament();
        TournamentWallet wallet = MultiGroupFixtures.tournamentWallet(1L, groupTournament, targetUser, new BigDecimal("200.00"));
        WalletResponseDTO dto = new WalletResponseDTO();
        dto.setUserId(2L);
        dto.setTournamentId(99L);

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(currentUserContext.getRequiredUser()).thenReturn(currentUser);
        when(groupTournamentRepository.findByTournamentIdAndGroupId(99L, 10L)).thenReturn(Optional.of(groupTournament));
        when(tournamentWalletRepository.findByGroupTournamentIdAndUserId(100L, 2L)).thenReturn(Optional.of(wallet));
        when(walletMapper.toResponse(wallet)).thenReturn(dto);

        WalletResponseDTO result = walletService.me(2L, 99L);

        assertEquals(2L, result.getUserId());
        verify(groupAuthorizationService).requireTournamentAdmin(99L);
    }

    @Test
    void me_rejectsNonAdminReadingAnotherUsersTournamentWallet() {
        User currentUser = createUser(1L);
        GroupTournament groupTournament = groupTournament();

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(currentUserContext.getRequiredUser()).thenReturn(currentUser);
        when(groupTournamentRepository.findByTournamentIdAndGroupId(99L, 10L)).thenReturn(Optional.of(groupTournament));
        doThrow(new UnauthorizedException("Acesso restrito ao administrador do grupo."))
                .when(groupAuthorizationService).requireTournamentAdmin(99L);

        assertThrows(UnauthorizedException.class, () -> walletService.me(2L, 99L));
        verify(tournamentWalletRepository, never()).findByGroupTournamentIdAndUserId(any(), any());
    }

    @Test
    void deposit_nullUserId_throwsValidationException() {
        WalletDepositRequestDTO dto = depositRequest(null, 99L, new BigDecimal("10.00"));

        ValidationException exception = assertThrows(ValidationException.class, () -> walletService.deposit(dto));

        assertTrue(exception.getMessage().contains("ID do usuario"));
        verify(groupTournamentRepository, never()).findByTournamentIdAndGroupId(any(), any());
    }

    @Test
    void deposit_nullOrNegativeAmount_throwsValidationException() {
        WalletDepositRequestDTO dtoNull = depositRequest(1L, 99L, null);
        ValidationException exNull = assertThrows(ValidationException.class, () -> walletService.deposit(dtoNull));
        assertTrue(exNull.getMessage().contains("valor"));

        WalletDepositRequestDTO dtoZero = depositRequest(1L, 99L, new BigDecimal("0"));
        ValidationException exZero = assertThrows(ValidationException.class, () -> walletService.deposit(dtoZero));
        assertTrue(exZero.getMessage().contains("maior que zero"));

        WalletDepositRequestDTO dtoNegative = depositRequest(1L, 99L, new BigDecimal("-10.00"));
        ValidationException exNegative = assertThrows(ValidationException.class, () -> walletService.deposit(dtoNegative));
        assertTrue(exNegative.getMessage().contains("maior que zero"));

        verify(groupTournamentRepository, never()).findByTournamentIdAndGroupId(any(), any());
    }

    @Test
    void deposit_tournamentNotAttachedToActiveGroup_throwsResourceNotFoundException() {
        WalletDepositRequestDTO dto = depositRequest(1L, 99L, new BigDecimal("50.00"));

        when(currentUserContext.getRequiredGroupId()).thenReturn(10L);
        when(groupTournamentRepository.findByTournamentIdAndGroupId(99L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> walletService.deposit(dto));
        verify(tournamentWalletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    private WalletDepositRequestDTO depositRequest(Long userId, Long tournamentId, BigDecimal amount) {
        WalletDepositRequestDTO dto = new WalletDepositRequestDTO();
        dto.setUserId(userId);
        dto.setTournamentId(tournamentId);
        dto.setAmount(amount);
        return dto;
    }

    private GroupTournament groupTournament() {
        Group group = MultiGroupFixtures.group(10L, "Group");
        return MultiGroupFixtures.groupTournament(100L, group, MultiGroupFixtures.tournament(99L, TournamentType.FIFA_MATCH));
    }

    private User createUser(Long id) {
        return MultiGroupFixtures.user(id, "User" + id, UserType.USER);
    }
}
