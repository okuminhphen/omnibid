package com.omnibid.wallet.service;

import com.omnibid.wallet.domain.Wallet;
import com.omnibid.wallet.domain.WalletTransaction;
import com.omnibid.wallet.domain.WalletTransactionStatus;
import com.omnibid.wallet.domain.WalletTransactionType;
import com.omnibid.wallet.repository.WalletRepository;
import com.omnibid.wallet.repository.WalletTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletAccountServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository transactionRepository;

    @InjectMocks
    private WalletAccountService service;

    private Wallet wallet;
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        wallet = Wallet.open(UUID.randomUUID(), userId, new BigDecimal("1000.00"));
        wallet.freeze(new BigDecimal("100.00"));
    }

    @Test
    void topUpCreditsBalanceAndStoresDurableIdempotencyKey() {
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByIdempotencyKey("top-up-key")).thenReturn(Optional.empty());

        Wallet result = service.topUp(userId, new BigDecimal("500.00"), "top-up-key");

        assertThat(result.getBalance()).isEqualByComparingTo("1500.00");
        assertThat(result.getAvailableBalance()).isEqualByComparingTo("1400.00");

        ArgumentCaptor<WalletTransaction> transaction = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getType()).isEqualTo(WalletTransactionType.TOP_UP);
        assertThat(transaction.getValue().getStatus()).isEqualTo(WalletTransactionStatus.SUCCESS);
        assertThat(transaction.getValue().getIdempotencyKey()).isEqualTo("top-up-key");
    }

    @Test
    void duplicateTopUpReturnsWalletWithoutCreditingAgain() {
        WalletTransaction duplicate = WalletTransaction.succeeded(
                UUID.randomUUID(), wallet.getId(), null, new BigDecimal("500.0"),
                WalletTransactionType.TOP_UP, "top-up-key"
        );

        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByIdempotencyKey("top-up-key"))
                .thenReturn(Optional.of(duplicate));

        Wallet result = service.topUp(userId, new BigDecimal("500.00"), "top-up-key");

        assertThat(result.getBalance()).isEqualByComparingTo("1000.00");
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void freezeResultDistinguishesNewReservationFromIdempotentReplay() {
        UUID auctionId = UUID.randomUUID();
        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByIdempotencyKey("deposit-key"))
                .thenReturn(Optional.empty());
        when(transactionRepository.findFirstByWalletIdAndAuctionIdAndStatusOrderByCreatedAtDesc(
                wallet.getId(), auctionId, WalletTransactionStatus.SUCCESS
        )).thenReturn(Optional.empty());

        FreezeResult created = service.freezeDeposit(
                "deposit-key", userId, auctionId, new BigDecimal("50.00")
        );

        assertThat(created.success()).isTrue();
        assertThat(created.newlyCreated()).isTrue();

        WalletTransaction existing = WalletTransaction.succeeded(
                created.transactionId(), wallet.getId(), auctionId, new BigDecimal("50.00"),
                WalletTransactionType.FREEZE, "deposit-key"
        );
        when(transactionRepository.findByIdempotencyKey("deposit-key"))
                .thenReturn(Optional.of(existing));

        FreezeResult replay = service.freezeDeposit(
                "deposit-key", userId, auctionId, new BigDecimal("50.00")
        );
        assertThat(replay.success()).isTrue();
        assertThat(replay.newlyCreated()).isFalse();
        assertThat(replay.transactionId()).isEqualTo(created.transactionId());
    }
}
