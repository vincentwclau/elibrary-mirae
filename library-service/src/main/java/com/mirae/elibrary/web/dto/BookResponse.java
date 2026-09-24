package com.mirae.elibrary.web.dto;

import java.util.UUID;

/**
 * Book representation used for both browse listings and the detail view.
 */
public record BookResponse(
        UUID id,
        String title,
        String author,
        String isbn,
        String description,
        String category,
        Integer publishedYear,
        int totalCopies,
        int availableCopies,
        boolean available
) {
}
