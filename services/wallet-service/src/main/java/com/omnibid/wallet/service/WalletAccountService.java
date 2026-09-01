package com.omnibid.wallet.service;

import com.omnibid.wallet.domain.Wallet;
import com.omnibid.wallet.domain.WalletTransaction;
import com.omnibid.wallet.domain.WalletTransactionStatus;
import com.omnibid.wallet.domain.WalletTransactionType;
import com.omnibid.wallet.repository.WalletRepository;
import com.omnibid.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletAccountService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public Wallet getWallet(UUID userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found for user: " + userId));
    }

    @Transactional
    public Wallet topUp(UUID userId, BigDecimal amount, String idempotencyKey) {
        validateMoneyCommand(amount, idempotencyKey, "Top-up");

        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found for user: " + userId));

        WalletTransaction duplicate = transactionRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);
        if (duplicate != null) {
            boolean sameRequest = duplicate.getType() == WalletTransactionType.TOP_UP
                    && duplicate.getStatus() == WalletTransactionStatus.SUCCESS
                    && duplicate.getWalletId().equals(wallet.getId())
                    && duplicate.getAmount().compareTo(amount) == 0;
            if (!sameRequest) {
                throw new IllegalArgumentException(
                        "X-Idempotency-Key was already used for a different wallet operation"
                );
            }
            return wallet;
        }

        wallet.credit(amount);

        WalletTransaction transaction = WalletTransaction.succeeded(
                UUID.randomUUID(), wallet.getId(), null, amount,
                WalletTransactionType.TOP_UP, idempotencyKey
        );
        transactionRepository.save(transaction);
        return wallet;
    }

    @Transactional
    public Wallet withdraw(UUID userId, BigDecimal amount, String idempotencyKey) {
        validateMoneyCommand(amount, idempotencyKey, "Withdraw");
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found for user: " + userId));

        WalletTransaction duplicate = transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElse(null);
        if (duplicate != null) {
            boolean sameRequest = duplicate.getType() == WalletTransactionType.WITHDRAW
                    && duplicate.getStatus() == WalletTransactionStatus.SUCCESS
                    && duplicate.getWalletId().equals(wallet.getId())
                    && duplicate.getAmount().compareTo(amount) == 0;
            if (!sameRequest) {
                throw new IllegalArgumentException(
                        "X-Idempotency-Key was already used for a different wallet operation"
                );
            }
            return wallet;
        }

        wallet.debitAvailable(amount);
        WalletTransaction transaction = WalletTransaction.succeeded(
                UUID.randomUUID(), wallet.getId(), null, amount,
                WalletTransactionType.WITHDRAW, idempotencyKey
        );
        transactionRepository.save(transaction);
        return wallet;
    }

    @Transactional(readOnly = true)
    public List<WalletTransaction> transactionHistory(UUID userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found for user: " + userId));
        return transactionRepository.findTop100ByWalletIdOrderByCreatedAtDesc(wallet.getId());
    }

    @Transactional
    public Wallet provisionWallet(UUID userId) {
        Wallet existing = walletRepository.findByUserId(userId).orElse(null);
        if (existing != null) {
            return existing;
        }
        Wallet wallet = Wallet.open(userId, userId, BigDecimal.ZERO.setScale(2));
        return walletRepository.save(wallet);
    }

    @Transactional
    public FreezeResult freezeDeposit(
            String idempotencyKey,
            UUID userId,
            UUID auctionId,
            BigDecimal amount
    ) {
        if (amount == null || amount.signum() <= 0) {
            return FreezeResult.rejected("INVALID_AMOUNT", "Amount must be positive");
        }

        // The row lock is the durable concurrency boundary for wallet balances.
        Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found for user: " + userId));

        WalletTransaction duplicate = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (duplicate != null) {
            verifySameFreezeRequest(duplicate, wallet.getId(), auctionId, amount);
            return FreezeResult.success(duplicate.getId(), false);
        }

        // A user deposits only once per auction, even if a later bid carries a new key.
        WalletTransaction latestAuctionTransaction = transactionRepository
                .findFirstByWalletIdAndAuctionIdAndStatusOrderByCreatedAtDesc(
                        wallet.getId(),
                        auctionId,
                        WalletTransactionStatus.SUCCESS
                )
                .orElse(null);
        if (latestAuctionTransaction != null
                && latestAuctionTransaction.getType() == WalletTransactionType.FREEZE) {
            if (latestAuctionTransaction.getAmount().compareTo(amount) != 0) {
                return FreezeResult.rejected(
                        "DEPOSIT_AMOUNT_CONFLICT",
                        "A different deposit amount was already frozen for this auction"
                );
            }
            return FreezeResult.success(latestAuctionTransaction.getId(), false);
        }

        if (!wallet.hasEnoughAvailableBalance(amount)) {
            return FreezeResult.rejected("INSUFFICIENT_FUNDS", "Available balance is too low");
        }

        wallet.freeze(amount);

        WalletTransaction transaction = WalletTransaction.succeeded(
                UUID.randomUUID(), wallet.getId(), auctionId, amount,
                WalletTransactionType.FREEZE, idempotencyKey
        );
        transactionRepository.save(transaction);

        return FreezeResult.success(transaction.getId(), true);
    }

    private void verifySameFreezeRequest(
            WalletTransaction transaction,
            UUID walletId,
            UUID auctionId,
            BigDecimal amount
    ) {
        boolean samePayload = transaction.getType() == WalletTransactionType.FREEZE
                && transaction.getStatus() == WalletTransactionStatus.SUCCESS
                && transaction.getWalletId().equals(walletId)
                && transaction.getAuctionId().equals(auctionId)
                && transaction.getAmount().compareTo(amount) == 0;
        if (!samePayload) {
            throw new IllegalArgumentException("Idempotency key was already used with a different payload");
        }
    }

    private void validateMoneyCommand(BigDecimal amount, String idempotencyKey, String operation) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(operation + " amount must be positive");
        }
        if (amount.compareTo(new BigDecimal("10000000.00")) > 0) {
            throw new IllegalArgumentException(operation + " amount exceeds the demo limit");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > 120) {
            throw new IllegalArgumentException("X-Idempotency-Key must contain 1 to 120 characters");
        }
    }
}
