package com.mirae.elibrary.service;

import com.mirae.elibrary.domain.Book;
import com.mirae.elibrary.exception.ResourceNotFoundException;
import com.mirae.elibrary.repository.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

/**
 * Read access to the book catalogue: paginated browse with optional
 * search/category filtering, and detail lookup.
 */
@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Page<Book> browse(String search, String category, Pageable pageable) {
        return bookRepository.search(
                normalize(search),
                normalize(category),
                pageable);
    }

    public Book getById(UUID id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", id));
    }

    /** Blank filters are treated as "no filter" (null) by the repository query. */
    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
