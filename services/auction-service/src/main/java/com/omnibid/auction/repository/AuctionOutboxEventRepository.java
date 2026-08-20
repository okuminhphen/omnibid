package com.omnibid.auction.repository;

import com.omnibid.auction.domain.AuctionOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface AuctionOutboxEventRepository extends JpaRepository<AuctionOutboxEvent, UUID> {

    @Query(value = """
            SELECT *
            FROM auction_outbox_events
            WHERE published_at IS NULL
            ORDER BY created_at ASC
            LIMIT 50
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AuctionOutboxEvent> lockNextUnpublishedBatch();
}
