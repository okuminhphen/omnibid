package com.omnibid.auction.exception;

public class LockUnavailableException extends RuntimeException {
    public LockUnavailableException(String message) {
        super(message);
    }
}
