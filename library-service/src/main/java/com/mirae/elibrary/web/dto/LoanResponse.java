package com.mirae.elibrary.web.dto;

import com.mirae.elibrary.domain.LoanStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * A loan with an embedded book summary, so the "my loans" view can render each
 * row without additional book lookups.
 */
public record LoanResponse(
        UUID id,
        LoanStatus status,
        Instant borrowedAt,
        Instant dueAt,
        Instant returnedAt,
        BookSummary book
) {
    public record BookSummary(
            UUID id,
            String title,
            String author,
            String isbn
    ) {
    }
}
