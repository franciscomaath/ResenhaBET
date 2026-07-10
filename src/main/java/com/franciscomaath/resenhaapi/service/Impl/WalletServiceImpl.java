package com.franciscomaath.resenhaapi.service.Impl;

import com.franciscomaath.resenhaapi.config.CurrentUserContext;
import com.franciscomaath.resenhaapi.domain.event.WalletChangeEvent;
import com.franciscomaath.resenhaapi.controller.dto.request.WalletDepositRequestDTO;
import com.franciscomaath.resenhaapi.controller.dto.response.WalletResponseDTO;
import com.franciscomaath.resenhaapi.domain.entity.Transaction;
import com.franciscomaath.resenhaapi.domain.entity.GroupTournament;
import com.franciscomaath.resenhaapi.domain.entity.TournamentWallet;
import com.franciscomaath.resenhaapi.domain.enums.TransactionType;
import com.franciscomaath.resenhaapi.domain.exception.ResourceNotFoundException;
import com.franciscomaath.resenhaapi.domain.exception.ValidationException;
import com.franciscomaath.resenhaapi.domain.repository.TransactionRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupMemberRepository;
import com.franciscomaath.resenhaapi.domain.repository.GroupTournamentRepository;
import com.franciscomaath.resenhaapi.domain.repository.TournamentWalletRepository;
import com.franciscomaath.resenhaapi.domain.repository.UserRepository;
import com.franciscomaath.resenhaapi.mapper.WalletMapper;
import com.franciscomaath.resenhaapi.service.GroupAuthorizationService;
import com.franciscomaath.resenhaapi.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletServiceImpl implements WalletService {

    private final WalletMapper walletMapper;
    private final TransactionRepository transactionRepository;
    private final CurrentUserContext currentUserContext;
    private final ApplicationEventPublisher eventPublisher;
    private final GroupTournamentRepository groupTournamentRepository;
    private final TournamentWalletRepository tournamentWalletRepository;
    private final GroupAuthorizationService groupAuthorizationService;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional(readOnly = true)
    public WalletResponseDTO me(Long userId, Long tournamentId) {
        GroupTournament groupTournament = getCurrentGroupTournament(tournamentId);
        Long currentUserId = currentUserContext.getRequiredUser().getId();
        if (!currentUserId.equals(userId)) {
            groupAuthorizationService.requireTournamentAdmin(groupTournament.getTournament().getId());
        }

        TournamentWallet wallet = tournamentWalletRepository.findByGroupTournamentIdAndUserId(groupTournament.getId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira nao encontrada."));
        
        // TODO: return transactions history
        
        return walletMapper.toResponse(wallet);
    }

    @Override
    @Transactional
    public BigDecimal deposit(WalletDepositRequestDTO dto) {
        if (dto.getUserId() == null) {
            throw new ValidationException("O ID do usuario e obrigatorio.");
        }

        if (dto.getAmount() == null) {
            throw new ValidationException("O valor do deposito e obrigatorio.");
        }

        if (dto.getAmount().scale() > 2) {
            throw new ValidationException("O valor do deposito deve ter no maximo 2 casas decimais.");
        }

        BigDecimal amount = dto.getAmount().setScale(2, RoundingMode.HALF_UP);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("O valor do deposito deve ser maior que zero.");
        }

        GroupTournament groupTournament = getCurrentGroupTournament(dto.getTournamentId());
        groupAuthorizationService.requireTournamentAdmin(groupTournament.getTournament().getId());
        requireUserInActiveGroup(dto.getUserId());
        TournamentWallet wallet = tournamentWalletRepository.findByGroupTournamentIdAndUserId(groupTournament.getId(), dto.getUserId())
                .orElseGet(() -> createWallet(groupTournament, dto.getUserId()));

        BigDecimal currentBalance = wallet.getBalance() == null ? BigDecimal.ZERO : wallet.getBalance();
        BigDecimal newBalance = currentBalance.add(amount).setScale(2, RoundingMode.HALF_UP);
        wallet.setBalance(newBalance);
        tournamentWalletRepository.save(wallet);
        publishWalletChange(wallet);

        Transaction transaction = new Transaction();
        transaction.setTournamentWallet(wallet);
        transaction.setType(TransactionType.DEPOSIT);
        transaction.setValue(amount);
        transactionRepository.save(transaction);

        return newBalance;
    }

    @Override
    @Transactional
    public void depositAll(Long tournamentId, BigDecimal amount) {
        if (amount == null) {
            throw new ValidationException("O valor do deposito e obrigatorio.");
        }

        if (amount.scale() > 2) {
            throw new ValidationException("O valor do deposito deve ter no maximo 2 casas decimais.");
        }

        BigDecimal scaledAmount = amount.setScale(2, RoundingMode.HALF_UP);
        if (scaledAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("O valor do deposito deve ser maior que zero.");
        }

        GroupTournament groupTournament = getCurrentGroupTournament(tournamentId);
        groupAuthorizationService.requireTournamentAdmin(groupTournament.getTournament().getId());
        List<TournamentWallet> wallets = tournamentWalletRepository.findByGroupTournamentId(groupTournament.getId());
        if (wallets.isEmpty()) {
            return;
        }

        tournamentWalletRepository.addAllBalances(groupTournament.getId(), scaledAmount);

        // Refresh wallets after bulk update to get accurate balances
        List<TournamentWallet> updatedWallets = tournamentWalletRepository.findByGroupTournamentId(groupTournament.getId());
        for (TournamentWallet wallet : updatedWallets) {
            publishWalletChange(wallet);
        }

        List<Transaction> transactions = wallets.stream().map(wallet -> {
            Transaction transaction = new Transaction();
            transaction.setTournamentWallet(wallet);
            transaction.setType(TransactionType.DEPOSIT);
            transaction.setValue(scaledAmount);
            return transaction;
        }).collect(Collectors.toList());

        transactionRepository.saveAll(transactions);
    }

    private GroupTournament getCurrentGroupTournament(Long tournamentId) {
        return groupTournamentRepository.findByTournamentIdAndGroupId(tournamentId, currentUserContext.getRequiredGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("GroupTournament", "tournamentId", tournamentId));
    }

    private TournamentWallet createWallet(GroupTournament groupTournament, Long userId) {
        TournamentWallet wallet = new TournamentWallet();
        wallet.setGroupTournament(groupTournament);
        wallet.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado com id: " + userId)));
        wallet.setBalance(BigDecimal.ZERO);
        wallet.setInitialBalance(BigDecimal.ZERO);
        return tournamentWalletRepository.save(wallet);
    }

    private void requireUserInActiveGroup(Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(currentUserContext.getRequiredGroupId(), userId)) {
            throw new ResourceNotFoundException("Membro do grupo nao encontrado para usuario id: " + userId);
        }
    }

    private void publishWalletChange(TournamentWallet wallet) {
        WalletResponseDTO dto = walletMapper.toResponse(wallet);
        eventPublisher.publishEvent(new WalletChangeEvent(this, wallet.getUser().getId(), dto));
    }

}
