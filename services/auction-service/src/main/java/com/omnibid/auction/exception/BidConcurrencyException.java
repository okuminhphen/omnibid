package com.omnibid.auction.exception;

public class BidConcurrencyException extends RuntimeException {

    public BidConcurrencyException(String message) {
        super(message);
    }

    public BidConcurrencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
