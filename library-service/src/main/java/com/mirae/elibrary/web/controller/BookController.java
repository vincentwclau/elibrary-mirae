package com.mirae.elibrary.web.controller;

import com.mirae.elibrary.service.BookService;
import com.mirae.elibrary.web.dto.BookResponse;
import com.mirae.elibrary.web.dto.PageResponse;
import com.mirae.elibrary.web.mapper.BookMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mirae.elibrary.domain.Book;

import java.util.UUID;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public PageResponse<BookResponse> browse(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<Book> books = bookService.browse(search, category, pageable);
        return PageResponse.from(books, BookMapper::toResponse);
    }

    @GetMapping("/{id}")
    public BookResponse getById(@PathVariable UUID id) {
        return BookMapper.toResponse(bookService.getById(id));
    }
}
