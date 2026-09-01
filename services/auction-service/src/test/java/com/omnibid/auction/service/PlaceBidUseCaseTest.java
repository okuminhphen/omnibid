package com.omnibid.auction.service;

import com.omnibid.auction.domain.Auction;
import com.omnibid.auction.domain.Bid;
import com.omnibid.auction.repository.AuctionRepository;
import com.omnibid.auction.repository.BidRepository;
import com.omnibid.auction.service.port.AuctionLockExecutor;
import com.omnibid.auction.service.port.AuctionPriceCache;
import com.omnibid.auction.service.port.WalletDepositPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlaceBidUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-08-31T08:00:00Z");

    private AuctionLockExecutor lockExecutor;
    private AuctionRepository auctionRepository;
    private BidRepository bidRepository;
    private WalletDepositPort walletDepositPort;
    private AuctionPriceCache priceCache;
    private AuctionOutboxService outboxService;
    private TransactionTemplate transactionTemplate;
    private PlaceBidUseCase useCase;

    @BeforeEach
    void setUp() {
        lockExecutor = mock(AuctionLockExecutor.class);
        auctionRepository = mock(AuctionRepository.class);
        bidRepository = mock(BidRepository.class);
        walletDepositPort = mock(WalletDepositPort.class);
        priceCache = mock(AuctionPriceCache.class);
        outboxService = mock(AuctionOutboxService.class);
        transactionTemplate = mock(TransactionTemplate.class);

        when(lockExecutor.execute(any(), any())).thenAnswer(invocation -> {
            Supplier<?> action = invocation.getArgument(1);
            return action.get();
        });
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        useCase = new PlaceBidUseCase(
                lockExecutor,
                auctionRepository,
                bidRepository,
                walletDepositPort,
                priceCache,
                outboxService,
                transactionTemplate,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void commitsAggregateBidAndOutboxThroughPorts() {
        UUID auctionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID walletTransactionId = UUID.randomUUID();
        Auction auction = activeAuction(auctionId);
        when(bidRepository.findByIdempotencyKey("request-1")).thenReturn(Optional.empty());
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(walletDepositPort.freeze(userId, auctionId, new BigDecimal("50.00"),
                "deposit:" + auctionId + ":" + userId))
                .thenReturn(WalletDepositPort.DepositReservation.accepted(walletTransactionId, true));

        var response = useCase.execute(auctionId, userId, new BigDecimal("120.00"), "request-1");

        assertThat(response.amount()).isEqualByComparingTo("120.00");
        assertThat(auction.getCurrentPrice()).isEqualByComparingTo("120.00");
        assertThat(auction.getWinningUserId()).isEqualTo(userId);
        verify(auctionRepository).saveAndFlush(auction);
        verify(bidRepository).save(any(Bid.class));
        verify(outboxService).enqueueBidPlaced(any());
        verify(priceCache).put(auctionId, new BigDecimal("120.00"));
    }

    @Test
    void returnsSameResultForDuplicateWithoutRepeatingSideEffects() {
        UUID auctionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Bid duplicate = Bid.place(
                UUID.randomUUID(), auctionId, userId, new BigDecimal("120.00"),
                "request-1", UUID.randomUUID(), NOW
        );
        when(bidRepository.findByIdempotencyKey("request-1")).thenReturn(Optional.of(duplicate));

        var response = useCase.execute(auctionId, userId, new BigDecimal("120.00"), "request-1");

        assertThat(response.bidId()).isEqualTo(duplicate.getId());
        verify(walletDepositPort, never()).freeze(any(), any(), any(), any());
        verify(outboxService, never()).enqueueBidPlaced(any());
        verify(priceCache, never()).put(any(), any());
    }

    @Test
    void releasesOnlyNewReservationWhenAuctionTransactionFails() {
        UUID auctionId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID walletTransactionId = UUID.randomUUID();
        Auction auction = activeAuction(auctionId);
        when(bidRepository.findByIdempotencyKey("request-2")).thenReturn(Optional.empty());
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(walletDepositPort.freeze(userId, auctionId, new BigDecimal("50.00"),
                "deposit:" + auctionId + ":" + userId))
                .thenReturn(WalletDepositPort.DepositReservation.accepted(walletTransactionId, true));
        when(auctionRepository.saveAndFlush(auction)).thenThrow(new IllegalStateException("db failed"));

        assertThatThrownBy(() -> useCase.execute(
                auctionId, userId, new BigDecimal("120.00"), "request-2"
        )).isInstanceOf(IllegalStateException.class).hasMessage("db failed");

        verify(walletDepositPort).release(
                walletTransactionId,
                userId,
                auctionId,
                new BigDecimal("50.00"),
                "release:" + walletTransactionId
        );
        verify(priceCache, never()).put(any(), any());
    }

    private Auction activeAuction(UUID auctionId) {
        Auction auction = Auction.schedule(
                auctionId,
                "Clean Architecture Auction",
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("50.00"),
                NOW.minusSeconds(60),
                NOW.plusSeconds(3600)
        );
        auction.activate(NOW);
        return auction;
    }
}
