package com.omnibid.auction.repository;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {
    List<Auction> findAllByOrderByEndTimeAsc();

    List<Auction> findTop100ByStatusAndEndTimeLessThanEqualOrderByEndTimeAsc(
            AuctionStatus status,
            Instant endTime
    );
}
