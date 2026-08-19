package com.omnibid.identity.exception;

public class AccountLinkRequiredException extends RuntimeException {
    public AccountLinkRequiredException(String message) {
        super(message);
    }
}
