package com.mirae.elibrary.service;

import com.mirae.elibrary.config.AppProperties;
import com.mirae.elibrary.domain.Book;
import com.mirae.elibrary.domain.Loan;
import com.mirae.elibrary.domain.LoanStatus;
import com.mirae.elibrary.domain.Role;
import com.mirae.elibrary.domain.User;
import com.mirae.elibrary.exception.BusinessRuleException;
import com.mirae.elibrary.exception.ResourceNotFoundException;
import com.mirae.elibrary.repository.BookRepository;
import com.mirae.elibrary.repository.LoanRepository;
import com.mirae.elibrary.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");
    private static final int MAX_ACTIVE = 5;
    private static final int PERIOD_DAYS = 14;

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserRepository userRepository;

    private LoanService loanService;

    private final UUID userId = UUID.randomUUID();
    private final UUID bookId = UUID.randomUUID();
    private User user;
    private Book book;

    @BeforeEach
    void setUp() {
        AppProperties properties = new AppProperties(
                new AppProperties.Cors(List.of("http://localhost:5173")),
                new AppProperties.Jwt("secret", 3600000),
                new AppProperties.Loan(MAX_ACTIVE, PERIOD_DAYS));
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        loanService = new LoanService(loanRepository, bookRepository, userRepository, properties, clock);

        user = new User("member@demo.io", "hash", "Member", Role.MEMBER);
        book = new Book("Clean Code", "Robert C. Martin", "9780132350884",
                "desc", "Software", 2008, 3);
    }

    @Test
    void borrow_success_decrementsCopiesAndCreatesActiveLoan() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(loanRepository.existsByUserIdAndBookIdAndStatus(userId, bookId, LoanStatus.ACTIVE))
                .thenReturn(false);
        when(loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE)).thenReturn(0L);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        Loan loan = loanService.borrow(userId, bookId);

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getBorrowedAt()).isEqualTo(NOW);
        assertThat(loan.getDueAt()).isEqualTo(NOW.plus(Duration.ofDays(PERIOD_DAYS)));
        assertThat(loan.getBook()).isSameAs(book);
        assertThat(book.getAvailableCopies()).isEqualTo(2); // 3 -> 2
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void borrow_userNotFound_throwsNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.borrow(userId, bookId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void borrow_bookNotFound_throwsNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.borrow(userId, bookId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void borrow_duplicateActiveLoan_throwsBusinessRule() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(loanRepository.existsByUserIdAndBookIdAndStatus(userId, bookId, LoanStatus.ACTIVE))
                .thenReturn(true);

        assertThatThrownBy(() -> loanService.borrow(userId, bookId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already have an active loan");
        assertThat(book.getAvailableCopies()).isEqualTo(3); // unchanged
    }

    @Test
    void borrow_loanLimitReached_throwsBusinessRule() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));
        when(loanRepository.existsByUserIdAndBookIdAndStatus(userId, bookId, LoanStatus.ACTIVE))
                .thenReturn(false);
        when(loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE))
                .thenReturn((long) MAX_ACTIVE);

        assertThatThrownBy(() -> loanService.borrow(userId, bookId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("limit");
    }

    @Test
    void borrow_noCopiesAvailable_throwsBusinessRule() {
        Book soldOut = new Book("Rare", "Author", "111", "d", "Cat", 2000, 0);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(bookRepository.findById(bookId)).thenReturn(Optional.of(soldOut));
        when(loanRepository.existsByUserIdAndBookIdAndStatus(userId, bookId, LoanStatus.ACTIVE))
                .thenReturn(false);
        when(loanRepository.countByUserIdAndStatus(userId, LoanStatus.ACTIVE)).thenReturn(0L);

        assertThatThrownBy(() -> loanService.borrow(userId, bookId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("No copies available");
    }

    @Test
    void returnLoan_success_marksReturnedAndRestoresCopy() {
        book.decrementAvailable(); // simulate it was borrowed (3 -> 2)
        Loan loan = Loan.borrow(user, book, NOW.minus(Duration.ofDays(1)), PERIOD_DAYS);
        UUID loanId = UUID.randomUUID();
        when(loanRepository.findByIdAndUserId(loanId, userId)).thenReturn(Optional.of(loan));

        Loan returned = loanService.returnLoan(userId, loanId);

        assertThat(returned.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(returned.getReturnedAt()).isEqualTo(NOW);
        assertThat(book.getAvailableCopies()).isEqualTo(3); // restored
    }

    @Test
    void returnLoan_notFound_throwsNotFound() {
        UUID loanId = UUID.randomUUID();
        when(loanRepository.findByIdAndUserId(loanId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.returnLoan(userId, loanId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void returnLoan_alreadyReturned_throwsBusinessRule() {
        Loan loan = Loan.borrow(user, book, NOW.minus(Duration.ofDays(1)), PERIOD_DAYS);
        loan.markReturned(NOW.minus(Duration.ofHours(1)));
        UUID loanId = UUID.randomUUID();
        when(loanRepository.findByIdAndUserId(loanId, userId)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.returnLoan(userId, loanId))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been returned");
    }

    @Test
    void getUserLoans_withStatus_usesStatusQuery() {
        when(loanRepository.findByUserIdAndStatusOrderByBorrowedAtDesc(userId, LoanStatus.ACTIVE))
                .thenReturn(List.of());

        loanService.getUserLoans(userId, LoanStatus.ACTIVE);

        verify(loanRepository).findByUserIdAndStatusOrderByBorrowedAtDesc(userId, LoanStatus.ACTIVE);
        verify(loanRepository, never()).findByUserIdOrderByBorrowedAtDesc(any());
    }

    @Test
    void getUserLoans_withoutStatus_usesAllQuery() {
        when(loanRepository.findByUserIdOrderByBorrowedAtDesc(userId)).thenReturn(List.of());

        loanService.getUserLoans(userId, null);

        verify(loanRepository).findByUserIdOrderByBorrowedAtDesc(userId);
    }
}
