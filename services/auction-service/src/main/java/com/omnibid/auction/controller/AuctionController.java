package com.omnibid.auction.controller;

import com.omnibid.auction.dto.AuctionResponse;
import com.omnibid.auction.dto.BidResponse;
import com.omnibid.auction.dto.EndAuctionResponse;
import com.omnibid.auction.dto.PlaceBidRequest;
import com.omnibid.auction.repository.AuctionRepository;
import com.omnibid.auction.service.AuctionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionRepository auctionRepository;
    private final AuctionService auctionService;

    @GetMapping
    public List<AuctionResponse> list() {
        return auctionRepository.findAllByOrderByEndTimeAsc().stream()
                .map(AuctionResponse::from)
                .toList();
    }

    @GetMapping("/{auctionId}")
    public AuctionResponse get(@PathVariable UUID auctionId) {
        return auctionRepository.findById(auctionId)
                .map(AuctionResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Auction not found: " + auctionId));
    }

    @PostMapping({"/{auctionId}/bid", "/{auctionId}/bids"})
    @ResponseStatus(HttpStatus.CREATED)
    public BidResponse placeBid(
            @PathVariable UUID auctionId,
            @Valid @RequestBody PlaceBidRequest request
    ) {
        return auctionService.placeBid(
                auctionId,
                request.userId(),
                request.bidAmount()
        );
    }

    @PostMapping("/{auctionId}/end")
    public EndAuctionResponse endAuction(@PathVariable UUID auctionId) {
        return auctionService.endAuction(auctionId);
    }
}
