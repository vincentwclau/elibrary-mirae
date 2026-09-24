package com.mirae.elibrary.web.mapper;

import com.mirae.elibrary.domain.Book;
import com.mirae.elibrary.web.dto.BookResponse;

/**
 * Maps {@link Book} entities to their API representation. Stateless; exposed as
 * static methods to keep call sites terse.
 */
public final class BookMapper {

    private BookMapper() {
    }

    public static BookResponse toResponse(Book book) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getDescription(),
                book.getCategory(),
                book.getPublishedYear(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                book.isAvailable());
    }
}
