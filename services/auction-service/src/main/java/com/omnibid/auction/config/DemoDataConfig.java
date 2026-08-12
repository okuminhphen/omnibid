package com.omnibid.auction.config;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.AuctionStatus;
import com.omnibid.auction.repository.AuctionRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Configuration
public class DemoDataConfig {

    public static final UUID DEMO_AUCTION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID PHASE_2_AUCTION_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    @Bean
    CommandLineRunner seedAuction(AuctionRepository repository) {
        return args -> {
            seedAuction(repository, DEMO_AUCTION_ID, "Mechanical Keyboard - Founder's Edition");
            seedAuction(repository, PHASE_2_AUCTION_ID, "Phase 2 Event-Driven Auction");
        };
    }

    private void seedAuction(AuctionRepository repository, UUID auctionId, String title) {
        if (repository.existsById(auctionId)) {
            return;
        }

        Auction auction = new Auction();
        auction.setId(auctionId);
        auction.setTitle(title);
        auction.setStatus(AuctionStatus.ACTIVE);
        auction.setStartingPrice(new BigDecimal("100.00"));
        auction.setCurrentPrice(new BigDecimal("100.00"));
        auction.setStepPrice(new BigDecimal("10.00"));
        auction.setDepositAmount(new BigDecimal("100.00"));
        auction.setStartTime(Instant.now().minus(1, ChronoUnit.DAYS));
        auction.setEndTime(Instant.now().plus(30, ChronoUnit.DAYS));
        repository.save(auction);
    }
}
