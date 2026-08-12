package com.omnibid.wallet.service;

import com.omnibid.wallet.domain.Wallet;
import com.omnibid.wallet.domain.WalletTransaction;
import com.omnibid.wallet.domain.WalletTransactionStatus;
import com.omnibid.wallet.domain.WalletTransactionType;
import com.omnibid.wallet.messaging.RefundMessage;
import com.omnibid.wallet.repository.WalletRepository;
import com.omnibid.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Transactional
    public void refund(RefundMessage message) {
        if (message.amount() == null || message.amount().signum() <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }

        Wallet wallet = walletRepository.findByIdForUpdate(message.walletId())
                .orElseThrow(() -> new NoSuchElementException("Wallet not found: " + message.walletId()));

        String idempotencyKey = "refund:" + message.messageId();
        WalletTransaction duplicate = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (duplicate != null) {
            boolean samePayload = duplicate.getType() == WalletTransactionType.REFUND
                    && duplicate.getWalletId().equals(message.walletId())
                    && java.util.Objects.equals(duplicate.getAuctionId(), message.auctionId())
                    && duplicate.getAmount().compareTo(message.amount()) == 0;
            if (!samePayload) {
                throw new IllegalArgumentException("Refund messageId was reused with a different payload");
            }
            return;
        }

        wallet.refundFrozen(message.amount());

        WalletTransaction transaction = new WalletTransaction();
        transaction.setId(UUID.randomUUID());
        transaction.setWalletId(message.walletId());
        transaction.setAuctionId(message.auctionId());
        transaction.setAmount(message.amount());
        transaction.setType(WalletTransactionType.REFUND);
        transaction.setStatus(WalletTransactionStatus.SUCCESS);
        transaction.setIdempotencyKey(idempotencyKey);
        transactionRepository.save(transaction);
    }
}
