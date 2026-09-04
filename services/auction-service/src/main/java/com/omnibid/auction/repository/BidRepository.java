package com.omnibid.auction.repository;

import com.omnibid.auction.domain.Bid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BidRepository extends JpaRepository<Bid, UUID> {
    Optional<Bid> findByIdempotencyKey(String idempotencyKey);

    List<Bid> findTop100ByAuctionIdOrderByPlacedAtDesc(UUID auctionId);

    @Query(value = """
            SELECT DISTINCT ON (bidder_id) *
            FROM bids
            WHERE auction_id = :auctionId
              AND bidder_id IS DISTINCT FROM :winnerId
            ORDER BY bidder_id, placed_at ASC
            """, nativeQuery = true)
    List<Bid> findRefundCandidates(
            @Param("auctionId") UUID auctionId,
            @Param("winnerId") UUID winnerId
    );
}
