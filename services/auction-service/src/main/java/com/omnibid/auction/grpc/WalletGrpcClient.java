package com.omnibid.auction.grpc;

import com.omnibid.contract.wallet.v1.FreezeRequest;
import com.omnibid.contract.wallet.v1.FreezeResponse;
import com.omnibid.contract.wallet.v1.WalletServiceGrpc;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class WalletGrpcClient {

    @GrpcClient("wallet-service")
    private WalletServiceGrpc.WalletServiceBlockingStub walletStub;

    public FreezeResponse freezeDeposit(
            String requestId,
            UUID auctionId,
            UUID bidderId,
            UUID bidId,
            BigDecimal amount
    ) {
        FreezeRequest request = FreezeRequest.newBuilder()
                .setRequestId(requestId)
                .setAuctionId(auctionId.toString())
                .setBidderId(bidderId.toString())
                .setBidId(bidId.toString())
                .setAmount(amount.toPlainString())
                .build();

        try {
            return walletStub
                    .withDeadlineAfter(2, TimeUnit.SECONDS)
                    .freezeDeposit(request);
        } catch (StatusRuntimeException exception) {
            throw new IllegalStateException("Wallet service is unavailable: " + exception.getStatus(), exception);
        }
    }
}
