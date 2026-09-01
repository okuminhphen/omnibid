package com.omnibid.auction.service;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.AuctionStatus;
import com.omnibid.auction.domain.Bid;
import com.omnibid.auction.dto.EndAuctionResponse;
import com.omnibid.auction.exception.BidConcurrencyException;
import com.omnibid.auction.exception.DomainException;
import com.omnibid.auction.messaging.RefundCommand;
import com.omnibid.auction.repository.AuctionRepository;
import com.omnibid.auction.repository.BidRepository;
import com.omnibid.auction.service.port.AuctionLockExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EndAuctionUseCase {
    private final AuctionLockExecutor lockExecutor;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionOutboxService outboxService;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public EndAuctionResponse execute(UUID auctionId) {
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId là bắt buộc");
        }
        return lockExecutor.execute(auctionId, () -> endWhileLocked(auctionId));
    }

    private EndAuctionResponse endWhileLocked(UUID auctionId) {
        try {
            return Objects.requireNonNull(
                    transactionTemplate.execute(status -> endInTransaction(auctionId))
            );
        } catch (OptimisticLockingFailureException exception) {
            throw new BidConcurrencyException(
                    "Phiên đấu giá vừa được cập nhật, vui lòng thử kết thúc lại!", exception
            );
        }
    }

    private EndAuctionResponse endInTransaction(UUID auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new NoSuchElementException("Auction not found: " + auctionId));
        if (auction.getStatus() == AuctionStatus.PENDING) {
            throw new DomainException("Không thể kết thúc phiên đấu giá đang PENDING");
        }
        if (auction.getStatus() != AuctionStatus.ENDED) {
            auction.end(clock.instant());
            auctionRepository.saveAndFlush(auction);
        }

        Map<UUID, RefundCommand> losingUsers = new LinkedHashMap<>();
        for (Bid bid : bidRepository.findRefundCandidates(auctionId, auction.getWinningUserId())) {
            losingUsers.putIfAbsent(
                    bid.getBidderId(),
                    new RefundCommand(
                            bid.getWalletTransactionId(),
                            bid.getBidderId(),
                            auctionId,
                            auction.getDepositAmount()
                    )
            );
        }
        losingUsers.values().forEach(outboxService::enqueueRefund);
        return new EndAuctionResponse(
                auction.getId(), auction.getStatus(), auction.getWinningUserId(), losingUsers.size()
        );
    }
}
