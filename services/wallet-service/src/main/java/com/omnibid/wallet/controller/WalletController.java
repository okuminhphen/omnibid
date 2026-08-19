package com.omnibid.wallet.controller;

import com.omnibid.wallet.dto.TopUpRequest;
import com.omnibid.wallet.dto.WalletResponse;
import com.omnibid.wallet.service.WalletAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletAccountService walletAccountService;

    @GetMapping("/{userId}")
    public WalletResponse getWallet(@PathVariable UUID userId) {
        return WalletResponse.from(walletAccountService.getWallet(userId));
    }

    @PostMapping("/{userId}/top-up")
    public WalletResponse topUp(
            @PathVariable UUID userId,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TopUpRequest request
    ) {
        String effectiveIdempotencyKey = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString()
                : idempotencyKey.trim();
        return WalletResponse.from(
                walletAccountService.topUp(userId, request.amount(), effectiveIdempotencyKey)
        );
    }
}
