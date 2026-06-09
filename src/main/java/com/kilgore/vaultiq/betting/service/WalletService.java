package com.kilgore.vaultiq.betting.service;

import com.kilgore.vaultiq.betting.dto.TransactionResponse;
import com.kilgore.vaultiq.betting.dto.WalletResponse;
import com.kilgore.vaultiq.betting.entity.TxnType;
import com.kilgore.vaultiq.betting.entity.WalletTransaction;
import com.kilgore.vaultiq.betting.repository.WalletTransactionRepository;
import com.kilgore.vaultiq.identity.entity.User;
import com.kilgore.vaultiq.identity.repository.UserRepository;
import com.kilgore.vaultiq.identity.service.UserService;
import com.kilgore.vaultiq.shared.exception.BadRequestException;
import com.kilgore.vaultiq.shared.exception.InsufficientBalanceException;
import com.kilgore.vaultiq.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Central wallet service. All money movement flows through here.
 *
 * ALL balance mutations use pessimistic locking via UserRepository.findByIdForWalletUpdate()
 * to prevent concurrent modification of wallet_balance.
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final UserRepository userRepository;
    private final WalletTransactionRepository transactionRepository;
    private final UserService userService;

    // ── Public Operations (called by controllers) ──

    @Transactional
    public WalletResponse deposit(BigDecimal amount) {
        User user = userService.getCurrentUser();
        return deposit(user.getId(), amount);
    }

    @Transactional
    public WalletResponse deposit(UUID userId, BigDecimal amount) {
        validateAmount(amount);
        User user = lockUserForWalletUpdate(userId);

        user.setWalletBalance(user.getWalletBalance().add(amount));
        userRepository.save(user);

        recordTransaction(user, TxnType.DEPOSIT, amount, null, null, "Wallet deposit");

        return buildWalletResponse(user);
    }

    @Transactional
    public WalletResponse withdraw(BigDecimal amount) {
        User user = userService.getCurrentUser();
        return withdraw(user.getId(), amount);
    }

    @Transactional
    public WalletResponse withdraw(UUID userId, BigDecimal amount) {
        validateAmount(amount);
        User user = lockUserForWalletUpdate(userId);

        if (user.getWalletBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, user.getWalletBalance());
        }

        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        userRepository.save(user);

        recordTransaction(user, TxnType.WITHDRAWAL, amount, null, null, "Wallet withdrawal");

        return buildWalletResponse(user);
    }

    @Transactional(readOnly = true)
    public WalletResponse getBalance() {
        User user = userService.getCurrentUser();
        return buildWalletResponse(user);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactionHistory(int page, int size) {
        User user = userService.getCurrentUser();
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransaction> transactions = transactionRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        return transactions.getContent().stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    // ── Internal Operations (called by other services in later phases) ──

    /**
     * Debit wallet for a bet placement, insurance premium, or syndicate contribution.
     * Uses pessimistic locking.
     */
    @Transactional
    public void debit(UUID userId, BigDecimal amount, TxnType type,
                      UUID referenceId, String referenceType, String description) {
        validateAmount(amount);
        User user = lockUserForWalletUpdate(userId);

        if (user.getWalletBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(amount, user.getWalletBalance());
        }

        user.setWalletBalance(user.getWalletBalance().subtract(amount));
        userRepository.save(user);

        recordTransaction(user, type, amount, referenceId, referenceType, description);
    }

    /**
     * Credit wallet for a bet win, cashout, insurance refund, syndicate payout, or tipster cut.
     * Uses pessimistic locking.
     */
    @Transactional
    public void credit(UUID userId, BigDecimal amount, TxnType type,
                       UUID referenceId, String referenceType, String description) {
        validateAmount(amount);
        User user = lockUserForWalletUpdate(userId);

        user.setWalletBalance(user.getWalletBalance().add(amount));
        userRepository.save(user);

        recordTransaction(user, type, amount, referenceId, referenceType, description);
    }

    // ── Private helpers ──

    private User lockUserForWalletUpdate(UUID userId) {
        return userRepository.findByIdForWalletUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Amount must be greater than zero");
        }
    }

    private void recordTransaction(User user, TxnType type, BigDecimal amount,
                                   UUID referenceId, String referenceType, String description) {
        WalletTransaction txn = WalletTransaction.builder()
                .user(user)
                .type(type)
                .amount(amount)
                .balanceAfter(user.getWalletBalance())
                .referenceId(referenceId)
                .referenceType(referenceType)
                .description(description)
                .build();

        transactionRepository.save(txn);
    }

    private WalletResponse buildWalletResponse(User user) {
        return WalletResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .balance(user.getWalletBalance())
                .build();
    }

    private TransactionResponse mapToTransactionResponse(WalletTransaction txn) {
        return TransactionResponse.builder()
                .id(txn.getId())
                .type(txn.getType().name())
                .amount(txn.getAmount())
                .balanceAfter(txn.getBalanceAfter())
                .referenceId(txn.getReferenceId())
                .referenceType(txn.getReferenceType())
                .description(txn.getDescription())
                .createdAt(txn.getCreatedAt())
                .build();
    }
}
