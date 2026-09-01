package com.omnibid.auction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateAuctionRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull @DecimalMin("0.00") BigDecimal startingPrice,
        @NotNull @DecimalMin("0.01") BigDecimal stepPrice,
        @NotNull @DecimalMin("0.01") BigDecimal depositAmount,
        @NotNull Instant startTime,
        @NotNull @Future Instant endTime
) {
}
