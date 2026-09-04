package com.omnibid.auction.service;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.dto.AuctionResponse;
import com.omnibid.auction.dto.CreateAuctionRequest;
import com.omnibid.auction.repository.AuctionRepository;
import com.omnibid.auction.service.port.AuctionLockExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuctionLifecycleUseCase {
    private final AuctionRepository auctionRepository;
    private final AuctionLockExecutor lockExecutor;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    @Transactional
    public AuctionResponse create(CreateAuctionRequest request) {
        Auction auction = Auction.schedule(
                UUID.randomUUID(),
                request.title(),
                request.startingPrice(),
                request.stepPrice(),
                request.depositAmount(),
                request.startTime(),
                request.endTime()
        );
        return AuctionResponse.from(auctionRepository.saveAndFlush(auction));
    }

    public AuctionResponse activate(UUID auctionId) {
        return lockExecutor.execute(auctionId, () -> Objects.requireNonNull(
                transactionTemplate.execute(status -> {
                    Auction auction = auctionRepository.findById(auctionId)
                            .orElseThrow(() -> new NoSuchElementException(
                                    "Auction not found: " + auctionId
                            ));
                    auction.activate(clock.instant());
                    return AuctionResponse.from(auctionRepository.saveAndFlush(auction));
                })
        ));
    }
}
