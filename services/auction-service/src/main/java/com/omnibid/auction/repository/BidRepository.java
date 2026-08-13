package com.omnibid.auction.repository;

import com.omnibid.auction.domain.Bid;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepository extends JpaRepository<Bid, UUID> {
    Optional<Bid> findByIdempotencyKey(String idempotencyKey);

    List<Bid> findAllByAuctionIdOrderByPlacedAtAsc(UUID auctionId);

    List<Bid> findAllByAuctionIdOrderByPlacedAtDesc(UUID auctionId);
}
