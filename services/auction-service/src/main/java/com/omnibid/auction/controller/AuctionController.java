package com.omnibid.auction.controller;

import com.omnibid.auction.dto.AuctionResponse;
import com.omnibid.auction.dto.BidResponse;
import com.omnibid.auction.dto.BidHistoryResponse;
import com.omnibid.auction.dto.EndAuctionResponse;
import com.omnibid.auction.dto.CreateAuctionRequest;
import com.omnibid.auction.dto.PlaceBidRequest;
import com.omnibid.auction.service.AuctionQueryService;
import com.omnibid.auction.service.AuctionService;
import com.omnibid.auction.service.AuctionLifecycleUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
public class AuctionController {

    private final AuctionQueryService queryService;
    private final AuctionService auctionService;
    private final AuctionLifecycleUseCase lifecycleUseCase;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuctionResponse create(@Valid @RequestBody CreateAuctionRequest request) {
        return lifecycleUseCase.create(request);
    }

    @PostMapping("/{auctionId}/activate")
    public AuctionResponse activate(@PathVariable UUID auctionId) {
        return lifecycleUseCase.activate(auctionId);
    }

    @GetMapping
    public List<AuctionResponse> list() {
        return queryService.list();
    }

    @GetMapping("/{auctionId}")
    public AuctionResponse get(@PathVariable UUID auctionId) {
        return queryService.get(auctionId);
    }

    @GetMapping("/{auctionId}/bids")
    public List<BidHistoryResponse> bidHistory(@PathVariable UUID auctionId) {
        return queryService.bidHistory(auctionId);
    }

    @PostMapping({"/{auctionId}/bid", "/{auctionId}/bids"})
    @ResponseStatus(HttpStatus.CREATED)
    public BidResponse placeBid(
            @PathVariable UUID auctionId,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PlaceBidRequest request
    ) {
        return auctionService.placeBid(
                auctionId,
                UUID.fromString(jwt.getSubject()),
                request.bidAmount(),
                idempotencyKey.trim()
        );
    }

    @PostMapping("/{auctionId}/end")
    public EndAuctionResponse endAuction(@PathVariable UUID auctionId) {
        return auctionService.endAuction(auctionId);
    }
}
