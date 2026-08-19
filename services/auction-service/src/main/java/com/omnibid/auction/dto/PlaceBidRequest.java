package com.omnibid.auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PlaceBidRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal bidAmount
) {
}
