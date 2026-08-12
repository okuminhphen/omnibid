package com.omnibid.wallet.service;

import com.omnibid.wallet.domain.Wallet;
import com.omnibid.wallet.domain.WalletTransaction;
import com.omnibid.wallet.domain.WalletTransactionType;
import com.omnibid.wallet.repository.WalletRepository;
import com.omnibid.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletAccountService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Transactional
    public FreezeResult freezeDeposit(
            String requestId,
            UUID auctionId,
            UUID bidderId,
            UUID bidId,
            BigDecimal amount
    ) {
        if (amount.signum() <= 0) {
            return FreezeResult.rejected("INVALID_AMOUNT", "Amount must be positive");
        }

        // Serialize balance changes in PostgreSQL even if multiple gRPC workers race.
        Wallet wallet = walletRepository.findByIdForUpdate(bidderId)
                .orElseThrow(() -> new NoSuchElementException("Wallet not found: " + bidderId));

        WalletTransaction duplicate = transactionRepository.findByRequestId(requestId).orElse(null);
        if (duplicate != null) {
            verifySameFreezeRequest(duplicate, auctionId, bidderId, bidId, amount);
            return FreezeResult.success(duplicate.getId());
        }

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            return FreezeResult.rejected("INSUFFICIENT_FUNDS", "Available balance is too low");
        }

        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(amount));
        wallet.setFrozenBalance(wallet.getFrozenBalance().add(amount));

        WalletTransaction transaction = new WalletTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setRequestId(requestId);
        transaction.setWalletId(bidderId);
        transaction.setAuctionId(auctionId);
        transaction.setBidId(bidId);
        transaction.setAmount(amount);
        transaction.setType(WalletTransactionType.FREEZE);
        transaction.setCreatedAt(Instant.now());
        transactionRepository.save(transaction);

        return FreezeResult.success(transaction.getId());
    }

    private void verifySameFreezeRequest(
            WalletTransaction transaction,
            UUID auctionId,
            UUID bidderId,
            UUID bidId,
            BigDecimal amount
    ) {
        boolean samePayload = transaction.getType() == WalletTransactionType.FREEZE
                && transaction.getWalletId().equals(bidderId)
                && transaction.getAuctionId().equals(auctionId)
                && transaction.getBidId().equals(bidId)
                && transaction.getAmount().compareTo(amount) == 0;
        if (!samePayload) {
            throw new IllegalArgumentException("Idempotency key was already used with a different payload");
        }
    }
}
