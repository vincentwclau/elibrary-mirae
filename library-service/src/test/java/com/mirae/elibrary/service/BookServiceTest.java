package com.mirae.elibrary.service;

import com.mirae.elibrary.domain.Book;
import com.mirae.elibrary.exception.ResourceNotFoundException;
import com.mirae.elibrary.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @org.mockito.InjectMocks
    private BookService bookService;

    @Test
    void browse_blankFiltersAreNormalizedToNull() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Book> page = new PageImpl<>(List.of());
        when(bookRepository.search(null, null, pageable)).thenReturn(page);

        Page<Book> result = bookService.browse("  ", "", pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void browse_trimsFilters() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Book> page = new PageImpl<>(List.of());
        when(bookRepository.search("clean", "Software", pageable)).thenReturn(page);

        Page<Book> result = bookService.browse("  clean ", " Software ", pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void getById_found_returnsBook() {
        UUID id = UUID.randomUUID();
        Book book = new Book("Clean Code", "Martin", "111", "d", "Software", 2008, 3);
        when(bookRepository.findById(id)).thenReturn(Optional.of(book));

        assertThat(bookService.getById(id)).isSameAs(book);
    }

    @Test
    void getById_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(bookRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
