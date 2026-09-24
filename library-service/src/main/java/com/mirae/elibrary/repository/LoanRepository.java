package com.mirae.elibrary.repository;

import com.mirae.elibrary.domain.Loan;
import com.mirae.elibrary.domain.LoanStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    // book is fetched eagerly so loans can be mapped to DTOs outside the
    // transaction (open-in-view is disabled).
    @EntityGraph(attributePaths = "book")
    List<Loan> findByUserIdOrderByBorrowedAtDesc(UUID userId);

    @EntityGraph(attributePaths = "book")
    List<Loan> findByUserIdAndStatusOrderByBorrowedAtDesc(UUID userId, LoanStatus status);

    long countByUserIdAndStatus(UUID userId, LoanStatus status);

    boolean existsByUserIdAndBookIdAndStatus(UUID userId, UUID bookId, LoanStatus status);

    @EntityGraph(attributePaths = "book")
    Optional<Loan> findByIdAndUserId(UUID id, UUID userId);
}
