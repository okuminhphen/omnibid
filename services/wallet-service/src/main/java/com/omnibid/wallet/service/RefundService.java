package com.omnibid.wallet.service;

import com.omnibid.wallet.domain.Wallet;
import com.omnibid.wallet.domain.WalletTransaction;
import com.omnibid.wallet.domain.WalletTransactionStatus;
import com.omnibid.wallet.domain.WalletTransactionType;
import com.omnibid.wallet.messaging.RefundCommand;
import com.omnibid.wallet.repository.WalletRepository;
import com.omnibid.wallet.repository.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    @Transactional
    public void refund(RefundCommand command) {
        if (command.amount() == null || command.amount().signum() <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }

        Wallet wallet = walletRepository.findByUserIdForUpdate(command.userId())
                .orElseThrow(() -> new NoSuchElementException("Wallet not found for user: " + command.userId()));

        String idempotencyKey = "refund:" + command.transactionId();
        WalletTransaction duplicate = transactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (duplicate != null) {
            boolean samePayload = duplicate.getType() == WalletTransactionType.REFUND
                    && duplicate.getStatus() == WalletTransactionStatus.SUCCESS
                    && duplicate.getWalletId().equals(wallet.getId())
                    && Objects.equals(duplicate.getAuctionId(), command.auctionId())
                    && duplicate.getAmount().compareTo(command.amount()) == 0;
            if (!samePayload) {
                throw new IllegalArgumentException("Refund transactionId was reused with a different payload");
            }
            return;
        }

        WalletTransaction freezeTransaction = transactionRepository.findById(command.transactionId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Freeze transaction not found: " + command.transactionId()
                ));
        validateFreezeTransaction(freezeTransaction, wallet, command);

        // balance is total owned money. Decreasing frozenBalance restores
        // availableBalance = balance - frozenBalance without minting money.
        wallet.refundFrozen(command.amount());

        WalletTransaction transaction = WalletTransaction.succeeded(
                UUID.randomUUID(),
                wallet.getId(),
                command.auctionId(),
                command.amount(),
                WalletTransactionType.REFUND,
                idempotencyKey
        );
        transactionRepository.save(transaction);
    }

    private void validateFreezeTransaction(
            WalletTransaction freezeTransaction,
            Wallet wallet,
            RefundCommand command
    ) {
        boolean matches = freezeTransaction.getType() == WalletTransactionType.FREEZE
                && freezeTransaction.getStatus() == WalletTransactionStatus.SUCCESS
                && freezeTransaction.getWalletId().equals(wallet.getId())
                && Objects.equals(freezeTransaction.getAuctionId(), command.auctionId())
                && freezeTransaction.getAmount().compareTo(command.amount()) == 0;
        if (!matches) {
            throw new IllegalArgumentException("Refund command does not match the original freeze transaction");
        }
    }
}
