package com.omnibid.auction.domain;

import com.omnibid.auction.exception.DomainException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuctionTest {
    private static final Instant NOW = Instant.parse("2026-08-31T08:00:00Z");

    @Test
    void controlsLifecycleAndBidInvariantInsideAggregate() {
        Auction auction = Auction.schedule(
                UUID.randomUUID(), "Auction", new BigDecimal("100.00"),
                new BigDecimal("10.00"), new BigDecimal("50.00"),
                NOW.minusSeconds(60), NOW.plusSeconds(3600)
        );
        auction.activate(NOW);

        assertThatThrownBy(() -> auction.acceptBid(
                UUID.randomUUID(), new BigDecimal("109.99"), NOW
        )).isInstanceOf(DomainException.class);

        UUID winner = UUID.randomUUID();
        auction.acceptBid(winner, new BigDecimal("110.00"), NOW);
        assertThat(auction.getCurrentPrice()).isEqualByComparingTo("110.00");
        assertThat(auction.getWinningUserId()).isEqualTo(winner);
    }

    @Test
    void rejectsInvalidScheduleInsteadOfCreatingBrokenEntity() {
        assertThatThrownBy(() -> Auction.schedule(
                UUID.randomUUID(), "Auction", new BigDecimal("100.00"),
                BigDecimal.ZERO, new BigDecimal("50.00"), NOW, NOW.plusSeconds(60)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stepPrice");
    }
}
