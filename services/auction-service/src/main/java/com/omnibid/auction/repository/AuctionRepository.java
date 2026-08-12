package com.omnibid.auction.repository;

import com.omnibid.auction.domain.Auction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuctionRepository extends JpaRepository<Auction, UUID> {
    List<Auction> findAllByOrderByEndsAtAsc();
}
