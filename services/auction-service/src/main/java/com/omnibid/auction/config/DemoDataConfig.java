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

    @Bean
    CommandLineRunner seedAuction(AuctionRepository repository) {
        return args -> {
            Auction auction = repository.findById(DEMO_AUCTION_ID).orElseGet(() -> {
                Auction created = new Auction();
                created.setId(DEMO_AUCTION_ID);
                created.setCurrentPrice(new BigDecimal("100.00"));
                return created;
            });
            auction.setTitle("Mechanical Keyboard - Founder's Edition");
            auction.setStatus(AuctionStatus.ACTIVE);
            auction.setStartingPrice(new BigDecimal("100.00"));
            auction.setStepPrice(new BigDecimal("10.00"));
            auction.setDepositAmount(new BigDecimal("100.00"));
            auction.setStartTime(Instant.now().minus(1, ChronoUnit.DAYS));
            auction.setEndTime(Instant.now().plus(30, ChronoUnit.DAYS));
            repository.save(auction);
        };
    }
}
