package com.omnibid.wallet.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(name = "uk_wallet_user_id", columnNames = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Wallet {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Total amount owned by the user, including the currently frozen amount. */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(name = "frozen_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal frozenBalance;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public BigDecimal getAvailableBalance() {
        return balance.subtract(frozenBalance);
    }

    public boolean hasEnoughAvailableBalance(BigDecimal amount) {
        return amount != null
                && amount.signum() > 0
                && getAvailableBalance().compareTo(amount) >= 0;
    }

    public void freeze(BigDecimal amount) {
        if (!hasEnoughAvailableBalance(amount)) {
            throw new IllegalStateException("Insufficient available balance");
        }
        frozenBalance = frozenBalance.add(amount);
    }

    public void refundFrozen(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || frozenBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("Frozen balance is lower than refund amount");
        }
        frozenBalance = frozenBalance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        balance = balance.add(amount);
    }
}
