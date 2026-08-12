package com.omnibid.auction.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceBidRequest(
        @NotNull @JsonAlias("bidderId") UUID userId,
        @NotNull @DecimalMin(value = "0.01") @JsonAlias("amount") BigDecimal bidAmount
) {
}
