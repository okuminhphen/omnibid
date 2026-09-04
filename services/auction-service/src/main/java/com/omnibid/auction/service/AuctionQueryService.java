package com.omnibid.auction.service;

import com.omnibid.auction.dto.AuctionResponse;
import com.omnibid.auction.dto.BidHistoryResponse;
import com.omnibid.auction.repository.AuctionRepository;
import com.omnibid.auction.repository.BidRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionQueryService {
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;

    public List<AuctionResponse> list() {
        return auctionRepository.findTop100ByOrderByEndTimeAsc().stream()
                .map(AuctionResponse::from)
                .toList();
    }

    public AuctionResponse get(UUID auctionId) {
        return auctionRepository.findById(auctionId)
                .map(AuctionResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Auction not found: " + auctionId));
    }

    public List<BidHistoryResponse> bidHistory(UUID auctionId) {
        if (!auctionRepository.existsById(auctionId)) {
            throw new NoSuchElementException("Auction not found: " + auctionId);
        }
        return bidRepository.findTop100ByAuctionIdOrderByPlacedAtDesc(auctionId).stream()
                .map(BidHistoryResponse::from)
                .toList();
    }
}
