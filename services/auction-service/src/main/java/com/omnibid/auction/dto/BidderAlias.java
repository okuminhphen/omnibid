package com.omnibid.auction.dto;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

final class BidderAlias {
    private BidderAlias() {
    }

    static String from(UUID auctionId, UUID bidderId) {
        if (bidderId == null) {
            return null;
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                    (auctionId + ":" + bidderId).getBytes(StandardCharsets.UTF_8)
            );
            return "Bidder-" + HexFormat.of().withUpperCase().formatHex(digest, 0, 4);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
