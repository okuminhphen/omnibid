package com.omnibid.wallet.grpc;

import com.omnibid.contract.wallet.v1.FreezeDepositRequest;
import com.omnibid.contract.wallet.v1.FreezeDepositResponse;
import com.omnibid.contract.wallet.v1.WalletServiceGrpc;
import com.omnibid.wallet.service.FreezeResult;
import com.omnibid.wallet.service.WalletAccountService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class WalletGrpcServiceImpl extends WalletServiceGrpc.WalletServiceImplBase {

    private final WalletAccountService walletAccountService;

    @Override
    public void freezeDeposit(
            FreezeDepositRequest request,
            StreamObserver<FreezeDepositResponse> responseObserver
    ) {
        try {
            // WalletAccountService owns the @Transactional boundary, including
            // the wallet row lock, balance validation and transaction insert.
            FreezeResult result = walletAccountService.freezeDeposit(
                    requireIdempotencyKey(request.getIdempotencyKey()),
                    UUID.fromString(request.getUserId()),
                    UUID.fromString(request.getAuctionId()),
                    new BigDecimal(request.getAmount())
            );

            FreezeDepositResponse.Builder response = FreezeDepositResponse.newBuilder()
                    .setSuccess(result.success())
                    .setMessage(result.message())
                    .setErrorCode(result.errorCode());
            if (result.transactionId() != null) {
                response.setTransactionId(result.transactionId().toString());
            }
            responseObserver.onNext(response.build());
            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            completeRejected(responseObserver, "INVALID_REQUEST", exception.getMessage());
        } catch (NoSuchElementException exception) {
            completeRejected(responseObserver, "WALLET_NOT_FOUND", exception.getMessage());
        } catch (Exception exception) {
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Could not freeze deposit")
                    .withCause(exception)
                    .asRuntimeException());
        }
    }

    private String requireIdempotencyKey(String key) {
        if (key == null || key.isBlank() || key.length() > 120) {
            throw new IllegalArgumentException("idempotency_key must contain 1-120 characters");
        }
        return key;
    }

    private void completeRejected(
            StreamObserver<FreezeDepositResponse> observer,
            String errorCode,
            String message
    ) {
        observer.onNext(FreezeDepositResponse.newBuilder()
                .setSuccess(false)
                .setMessage(message == null ? errorCode : message)
                .setErrorCode(errorCode)
                .build());
        observer.onCompleted();
    }
}
