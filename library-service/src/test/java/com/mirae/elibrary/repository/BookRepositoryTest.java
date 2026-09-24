package com.mirae.elibrary.repository;

import com.mirae.elibrary.domain.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @BeforeEach
    void seed() {
        bookRepository.save(new Book("Clean Code", "Robert C. Martin", "111",
                "d", "Software", 2008, 3));
        bookRepository.save(new Book("Effective Java", "Joshua Bloch", "222",
                "d", "Software", 2018, 4));
        bookRepository.save(new Book("Sapiens", "Yuval Noah Harari", "333",
                "d", "History", 2011, 5));
    }

    @Test
    void search_noFilters_returnsAll() {
        Page<Book> result = bookRepository.search(null, null, PageRequest.of(0, 10));
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    void search_byTitleCaseInsensitive() {
        Page<Book> result = bookRepository.search("clean", null, PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(Book::getTitle).containsExactly("Clean Code");
    }

    @Test
    void search_byAuthor() {
        Page<Book> result = bookRepository.search("bloch", null, PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(Book::getTitle).containsExactly("Effective Java");
    }

    @Test
    void search_byCategory() {
        Page<Book> result = bookRepository.search(null, "software", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void search_combinesSearchAndCategory() {
        Page<Book> result = bookRepository.search("java", "Software", PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(Book::getTitle).containsExactly("Effective Java");
    }

    @Test
    void search_paginates() {
        Page<Book> first = bookRepository.search(null, null, PageRequest.of(0, 2));
        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.isLast()).isFalse();
    }

    @Test
    void existsByIsbn() {
        assertThat(bookRepository.existsByIsbn("111")).isTrue();
        assertThat(bookRepository.existsByIsbn("999")).isFalse();
    }
}
