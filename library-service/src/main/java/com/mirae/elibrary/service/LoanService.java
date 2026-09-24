package com.mirae.elibrary.service;

import com.mirae.elibrary.config.AppProperties;
import com.mirae.elibrary.domain.Book;
import com.mirae.elibrary.domain.Loan;
import com.mirae.elibrary.domain.LoanStatus;
import com.mirae.elibrary.domain.User;
import com.mirae.elibrary.exception.BusinessRuleException;
import com.mirae.elibrary.exception.ResourceNotFoundException;
import com.mirae.elibrary.repository.BookRepository;
import com.mirae.elibrary.repository.LoanRepository;
import com.mirae.elibrary.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates borrowing and returning. This is where the library's business
 * rules live; entities keep their own invariants but the service coordinates
 * the loan, the book's copy count, and the per-user limits within one
 * transaction. Book copy updates rely on the {@code @Version} optimistic lock
 * so concurrent borrows of the last copy cannot both succeed.
 */
@Service
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final AppProperties properties;
    private final Clock clock;

    public LoanService(LoanRepository loanRepository,
                       BookRepository bookRepository,
                       UserRepository userRepository,
                       AppProperties properties,
                       Clock clock) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public Loan borrow(UUID userId, UUID bookId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> ResourceNotFoundException.of("Book", bookId));

        if (loanRepository.existsByUserIdAndBookIdAndStatus(userId, bookId, LoanStatus.ACTIVE)) {
            throw new BusinessRuleException("You already have an active loan for this book");
        }
        long activeLoans = loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE);
        if (activeLoans >= properties.getLoan().getMaxActiveLoans()) {
            throw new BusinessRuleException(
                    "Active loan limit reached (" + properties.getLoan().getMaxActiveLoans() + ")");
        }
        if (!book.isAvailable()) {
            throw new BusinessRuleException("No copies available to borrow");
        }

        book.decrementAvailable();
        Loan loan = Loan.borrow(user, book, Instant.now(clock), properties.getLoan().getPeriodDays());
        return loanRepository.save(loan);
    }

    @Transactional
    public Loan returnLoan(UUID userId, UUID loanId) {
        Loan loan = loanRepository.findByIdAndUserId(loanId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Loan", loanId));

        if (!loan.isActive()) {
            throw new BusinessRuleException("Loan has already been returned");
        }

        loan.markReturned(Instant.now(clock));
        loan.getBook().incrementAvailable();
        return loan;
    }

    @Transactional(readOnly = true)
    public List<Loan> getUserLoans(UUID userId, LoanStatus statusFilter) {
        if (statusFilter != null) {
            return loanRepository.findByUserIdAndStatusOrderByBorrowedAtDesc(userId, statusFilter);
        }
        return loanRepository.findByUserIdOrderByBorrowedAtDesc(userId);
    }
}
