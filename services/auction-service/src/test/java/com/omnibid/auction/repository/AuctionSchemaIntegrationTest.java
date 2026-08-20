package com.omnibid.auction.repository;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.AuctionStatus;
import com.omnibid.auction.domain.Bid;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=false"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AuctionSchemaIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("auction_test")
            .withUsername("omnibid")
            .withPassword("omnibid");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private AuctionRepository auctionRepository;

    @Autowired
    private BidRepository bidRepository;

    @Test
    void flywaySchemaSupportsAuctionBidAndDurableIdempotency() {
        UUID auctionId = UUID.randomUUID();
        Auction auction = activeAuction(auctionId);
        auctionRepository.saveAndFlush(auction);

        String idempotencyKey = UUID.randomUUID().toString();
        bidRepository.saveAndFlush(bid(auctionId, idempotencyKey, new BigDecimal("120.00")));

        assertThat(bidRepository.findByIdempotencyKey(idempotencyKey)).isPresent();

        Bid duplicate = bid(auctionId, idempotencyKey, new BigDecimal("130.00"));
        assertThatThrownBy(() -> bidRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Auction activeAuction(UUID id) {
        Instant now = Instant.now();
        Auction auction = new Auction();
        auction.setId(id);
        auction.setTitle("Testcontainers Auction");
        auction.setStartingPrice(new BigDecimal("100.00"));
        auction.setCurrentPrice(new BigDecimal("100.00"));
        auction.setStepPrice(new BigDecimal("10.00"));
        auction.setDepositAmount(new BigDecimal("50.00"));
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setStartTime(now.minus(1, ChronoUnit.HOURS));
        auction.setEndTime(now.plus(1, ChronoUnit.HOURS));
        return auction;
    }

    private Bid bid(UUID auctionId, String idempotencyKey, BigDecimal amount) {
        Bid bid = new Bid();
        bid.setId(UUID.randomUUID());
        bid.setAuctionId(auctionId);
        bid.setBidderId(UUID.randomUUID());
        bid.setAmount(amount);
        bid.setIdempotencyKey(idempotencyKey);
        bid.setWalletTransactionId(UUID.randomUUID());
        bid.setPlacedAt(Instant.now());
        return bid;
    }
}
