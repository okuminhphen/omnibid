package com.omnibid.wallet.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WalletTest {

    @Test
    void freezesOnlyFromAvailableBalance() {
        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("100.00"));
        wallet.setFrozenBalance(new BigDecimal("20.00"));

        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("80.00");
        assertThat(wallet.hasEnoughAvailableBalance(new BigDecimal("80.00"))).isTrue();
        assertThat(wallet.hasEnoughAvailableBalance(new BigDecimal("80.01"))).isFalse();

        wallet.freeze(new BigDecimal("30.00"));
        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        assertThat(wallet.getFrozenBalance()).isEqualByComparingTo("50.00");
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("50.00");

        assertThatThrownBy(() -> wallet.freeze(new BigDecimal("50.01")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Insufficient available balance");
    }

    @Test
    void refundMovesMoneyOutOfFrozenBalanceWithoutChangingTotalBalance() {
        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("100.00"));
        wallet.setFrozenBalance(new BigDecimal("40.00"));

        wallet.refundFrozen(new BigDecimal("15.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo("100.00");
        assertThat(wallet.getFrozenBalance()).isEqualByComparingTo("25.00");
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo("75.00");
    }
}
