package com.mirae.elibrary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A borrowing record linking a {@link User} to a {@link Book}. Creating and
 * closing loans is orchestrated by the service layer, which also adjusts the
 * book's available-copy count. The {@code borrow}/{@code markReturned} factory
 * and mutator here keep the loan's own invariants (status/timestamps) together.
 */
@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(nullable = false)
    private Instant borrowedAt;

    @Column(nullable = false)
    private Instant dueAt;

    private Instant returnedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status;

    protected Loan() {
        // for JPA
    }

    private Loan(User user, Book book, Instant borrowedAt, Instant dueAt) {
        this.user = user;
        this.book = book;
        this.borrowedAt = borrowedAt;
        this.dueAt = dueAt;
        this.status = LoanStatus.ACTIVE;
    }

    /**
     * Opens a new active loan for the given period. Does not touch the book's
     * copy count — the caller (service) owns that transactionally.
     */
    public static Loan borrow(User user, Book book, Instant when, int periodDays) {
        return new Loan(user, book, when, when.plus(java.time.Duration.ofDays(periodDays)));
    }

    /**
     * Closes this loan. Idempotency is the caller's concern; this enforces that
     * an already-returned loan is not returned twice.
     */
    public void markReturned(Instant when) {
        if (status == LoanStatus.RETURNED) {
            throw new IllegalStateException("Loan already returned");
        }
        this.returnedAt = when;
        this.status = LoanStatus.RETURNED;
    }

    public boolean isActive() {
        return status == LoanStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Book getBook() {
        return book;
    }

    public Instant getBorrowedAt() {
        return borrowedAt;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getReturnedAt() {
        return returnedAt;
    }

    public LoanStatus getStatus() {
        return status;
    }
}
