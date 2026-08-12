package com.omnibid.auction.grpc;

import com.omnibid.contract.wallet.v1.FreezeDepositRequest;
import com.omnibid.contract.wallet.v1.FreezeDepositResponse;
import com.omnibid.contract.wallet.v1.WalletServiceGrpc;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class WalletClient {

    @GrpcClient("wallet-service")
    private WalletServiceGrpc.WalletServiceBlockingStub walletStub;

    public FreezeDepositResponse freezeDeposit(
            UUID userId,
            BigDecimal amount,
            UUID auctionId,
            String idempotencyKey
    ) {
        FreezeDepositRequest request = FreezeDepositRequest.newBuilder()
                .setIdempotencyKey(idempotencyKey)
                .setUserId(userId.toString())
                .setAmount(amount.toPlainString())
                .setAuctionId(auctionId.toString())
                .build();

        try {
            return walletStub
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .freezeDeposit(request);
        } catch (StatusRuntimeException exception) {
            throw new IllegalStateException(
                    "Wallet service is unavailable: " + exception.getStatus(),
                    exception
            );
        }
    }
}
