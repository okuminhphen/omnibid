package com.omnibid.auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceBidRequest(
        @NotNull UUID bidderId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {
}
