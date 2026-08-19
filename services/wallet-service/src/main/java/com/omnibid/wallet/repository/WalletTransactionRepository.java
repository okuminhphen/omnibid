package com.omnibid.wallet.repository;

import com.omnibid.wallet.domain.WalletTransaction;
import com.omnibid.wallet.domain.WalletTransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    Optional<WalletTransaction> findByIdempotencyKey(String idempotencyKey);

    Optional<WalletTransaction> findFirstByWalletIdAndAuctionIdAndStatusOrderByCreatedAtDesc(
            UUID walletId,
            UUID auctionId,
            WalletTransactionStatus status
    );

    List<WalletTransaction> findTop100ByWalletIdOrderByCreatedAtDesc(UUID walletId);
}
