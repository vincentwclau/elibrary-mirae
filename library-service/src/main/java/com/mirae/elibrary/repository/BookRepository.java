package com.mirae.elibrary.repository;

import com.mirae.elibrary.domain.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface BookRepository extends JpaRepository<Book, UUID> {

    boolean existsByIsbn(String isbn);

    /**
     * Free-text browse over title/author plus optional category filter. Both
     * parameters are optional (null = ignored), keeping a single query for the
     * browse endpoint's search + filter combinations.
     */
    @Query("""
            SELECT b FROM Book b
            WHERE (:search IS NULL
                   OR LOWER(b.title) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(b.author) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:category IS NULL OR LOWER(b.category) = LOWER(:category))
            """)
    Page<Book> search(@Param("search") String search,
                      @Param("category") String category,
                      Pageable pageable);
}
