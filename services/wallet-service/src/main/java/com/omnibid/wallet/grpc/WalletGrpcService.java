package com.omnibid.wallet.grpc;

import com.omnibid.contract.wallet.v1.FreezeRequest;
import com.omnibid.contract.wallet.v1.FreezeResponse;
import com.omnibid.contract.wallet.v1.WalletServiceGrpc;
import com.omnibid.wallet.service.FreezeResult;
import com.omnibid.wallet.service.WalletAccountService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class WalletGrpcService extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletAccountService walletAccountService;

    @Override
    public void freezeDeposit(FreezeRequest request, StreamObserver<FreezeResponse> responseObserver) {
        try {
            FreezeResult result = walletAccountService.freezeDeposit(
                    requireRequestId(request.getRequestId()),
                    UUID.fromString(request.getAuctionId()),
                    UUID.fromString(request.getBidderId()),
                    UUID.fromString(request.getBidId()),
                    new BigDecimal(request.getAmount())
            );

            FreezeResponse.Builder response = FreezeResponse.newBuilder()
                    .setSuccess(result.success())
                    .setErrorCode(result.errorCode())
                    .setMessage(result.message());
            if (result.transactionId() != null) {
                response.setTransactionId(result.transactionId().toString());
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onNext(FreezeResponse.newBuilder()
                    .setSuccess(false)
                    .setErrorCode("INVALID_REQUEST")
                    .setMessage(exception.getMessage())
                    .build());
            responseObserver.onCompleted();
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Could not freeze deposit")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    private String requireRequestId(String requestId) {
        if (requestId == null || requestId.isBlank() || requestId.length() > 100) {
            throw new IllegalArgumentException("request_id must contain 1-100 characters");
        }
        return requestId;
    }
}
