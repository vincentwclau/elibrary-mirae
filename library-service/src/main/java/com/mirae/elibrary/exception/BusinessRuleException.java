package com.mirae.elibrary.exception;

/**
 * Thrown when a request is well-formed and authorized but violates a domain
 * rule (e.g. no copies available, loan limit reached, duplicate active loan).
 * Mapped to HTTP 409 Conflict.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
