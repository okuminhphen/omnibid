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
        wallet = new Wallet();
        wallet.setId(UUID.randomUUID());
        wallet.setUserId(userId);
        wallet.setBalance(new BigDecimal("1000.00"));
        wallet.setFrozenBalance(new BigDecimal("100.00"));
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
        WalletTransaction duplicate = new WalletTransaction();
        duplicate.setWalletId(wallet.getId());
        duplicate.setAmount(new BigDecimal("500.0"));
        duplicate.setType(WalletTransactionType.TOP_UP);
        duplicate.setStatus(WalletTransactionStatus.SUCCESS);

        when(walletRepository.findByUserIdForUpdate(userId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.findByIdempotencyKey("top-up-key"))
                .thenReturn(Optional.of(duplicate));

        Wallet result = service.topUp(userId, new BigDecimal("500.00"), "top-up-key");

        assertThat(result.getBalance()).isEqualByComparingTo("1000.00");
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
