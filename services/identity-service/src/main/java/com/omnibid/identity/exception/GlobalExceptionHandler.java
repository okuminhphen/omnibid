package com.omnibid.identity.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({AuthenticationException.class, JwtException.class})
    ResponseEntity<ApiError> unauthorized(RuntimeException exception) {
        return error(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED", exception.getMessage());
    }

    @ExceptionHandler(AccountLinkRequiredException.class)
    ResponseEntity<ApiError> conflict(AccountLinkRequiredException exception) {
        return error(HttpStatus.CONFLICT, "ACCOUNT_LINK_REQUIRED", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    ResponseEntity<ApiError> notFound(NoSuchElementException exception) {
        return error(HttpStatus.NOT_FOUND, "NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    ResponseEntity<ApiError> badRequest(Exception exception) {
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", exception.getMessage());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(),
                status.value(),
                code,
                message
        ));
    }
}
