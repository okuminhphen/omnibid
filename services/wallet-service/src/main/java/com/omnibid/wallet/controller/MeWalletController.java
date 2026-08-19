package com.omnibid.wallet.controller;

import com.omnibid.wallet.dto.TopUpRequest;
import com.omnibid.wallet.dto.WalletResponse;
import com.omnibid.wallet.dto.WalletTransactionResponse;
import com.omnibid.wallet.service.WalletAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/me/wallet")
@RequiredArgsConstructor
public class MeWalletController {

    private final WalletAccountService walletAccountService;

    @GetMapping
    public WalletResponse wallet(@AuthenticationPrincipal Jwt jwt) {
        return WalletResponse.from(walletAccountService.getWallet(userId(jwt)));
    }

    @GetMapping("/transactions")
    public List<WalletTransactionResponse> transactions(@AuthenticationPrincipal Jwt jwt) {
        return walletAccountService.transactionHistory(userId(jwt)).stream()
                .map(WalletTransactionResponse::from)
                .toList();
    }

    @PostMapping("/top-ups")
    public WalletResponse topUp(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TopUpRequest request
    ) {
        return WalletResponse.from(walletAccountService.topUp(
                userId(jwt),
                request.amount(),
                idempotencyKey.trim()
        ));
    }

    @PostMapping("/withdrawals")
    public WalletResponse withdraw(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader("X-Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TopUpRequest request
    ) {
        return WalletResponse.from(walletAccountService.withdraw(
                userId(jwt),
                request.amount(),
                idempotencyKey.trim()
        ));
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
