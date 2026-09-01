package com.omnibid.auction.dto;

import com.omnibid.auction.domain.Bid;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PublicAuctionProjectionTest {
    @Test
    void exposesStableAliasWithoutLeakingUserOrWalletIds() {
        UUID auctionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID walletTransactionId = UUID.randomUUID();
        Bid bid = Bid.place(
                UUID.randomUUID(), auctionId, userId, new BigDecimal("120.00"),
                "request-key", walletTransactionId, Instant.now()
        );

        BidHistoryResponse first = BidHistoryResponse.from(bid);
        BidHistoryResponse second = BidHistoryResponse.from(bid);

        assertThat(first.bidderAlias()).isEqualTo(second.bidderAlias()).startsWith("Bidder-");
        assertThat(first.toString()).doesNotContain(userId.toString(), walletTransactionId.toString());
    }
}
