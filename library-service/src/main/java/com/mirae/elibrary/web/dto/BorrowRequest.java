package com.mirae.elibrary.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BorrowRequest(
        @NotNull UUID bookId
) {
}
