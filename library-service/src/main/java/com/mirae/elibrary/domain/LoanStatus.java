package com.mirae.elibrary.domain;

/**
 * Lifecycle of a loan. A book is "currently borrowed" while its loan is ACTIVE.
 */
public enum LoanStatus {
    ACTIVE,
    RETURNED
}
