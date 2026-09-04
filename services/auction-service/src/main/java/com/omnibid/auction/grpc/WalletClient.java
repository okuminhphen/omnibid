package com.omnibid.auction.grpc;

import com.omnibid.contract.wallet.v1.FreezeDepositRequest;
import com.omnibid.contract.wallet.v1.FreezeDepositResponse;
import com.omnibid.contract.wallet.v1.WalletServiceGrpc;
import com.omnibid.contract.wallet.v1.ReleaseDepositRequest;
import com.omnibid.contract.wallet.v1.ReleaseDepositResponse;
import com.omnibid.auction.service.port.WalletDepositPort;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class WalletClient implements WalletDepositPort {

    @GrpcClient("wallet-service")
    private WalletServiceGrpc.WalletServiceBlockingStub walletStub;

    @Override
    public DepositReservation freeze(
            UUID userId,
            UUID auctionId,
            BigDecimal amount,
            String idempotencyKey
    ) {
        FreezeDepositRequest request = FreezeDepositRequest.newBuilder()
                .setIdempotencyKey(idempotencyKey)
                .setUserId(userId.toString())
                .setAmount(amount.toPlainString())
                .setAuctionId(auctionId.toString())
                .build();

        try {
            FreezeDepositResponse response = walletStub
                    // The first call after a container restart may establish an HTTP/2 channel.
                    .withWaitForReady()
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .freezeDeposit(request);
            if (!response.getSuccess()) {
                return DepositReservation.rejected(response.getErrorCode(), response.getMessage());
            }
            if (response.getTransactionId().isBlank()) {
                throw new IllegalStateException("Wallet service returned no transactionId");
            }
            return DepositReservation.accepted(
                    UUID.fromString(response.getTransactionId()),
                    response.getNewlyCreated()
            );
        } catch (StatusRuntimeException exception) {
            throw new IllegalStateException(
                    "Wallet service is unavailable: " + exception.getStatus(),
                    exception
            );
        }
    }

    @Override
    public void release(
            UUID transactionId,
            UUID userId,
            UUID auctionId,
            BigDecimal amount,
            String idempotencyKey
    ) {
        ReleaseDepositRequest request = ReleaseDepositRequest.newBuilder()
                .setIdempotencyKey(idempotencyKey)
                .setTransactionId(transactionId.toString())
                .setUserId(userId.toString())
                .setAuctionId(auctionId.toString())
                .setAmount(amount.toPlainString())
                .build();
        try {
            ReleaseDepositResponse response = walletStub
                    .withWaitForReady()
                    .withDeadlineAfter(3, TimeUnit.SECONDS)
                    .releaseDeposit(request);
            if (!response.getSuccess()) {
                throw new IllegalStateException(
                        "Wallet rejected deposit compensation: "
                                + response.getErrorCode() + " - " + response.getMessage()
                );
            }
        } catch (StatusRuntimeException exception) {
            throw new IllegalStateException(
                    "Wallet service is unavailable during deposit compensation: "
                            + exception.getStatus(),
                    exception
            );
        }
    }
}
