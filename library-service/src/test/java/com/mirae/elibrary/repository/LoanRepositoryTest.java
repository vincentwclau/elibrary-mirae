package com.mirae.elibrary.repository;

import com.mirae.elibrary.domain.Book;
import com.mirae.elibrary.domain.Loan;
import com.mirae.elibrary.domain.LoanStatus;
import com.mirae.elibrary.domain.Role;
import com.mirae.elibrary.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class LoanRepositoryTest {

    @Autowired
    private LoanRepository loanRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BookRepository bookRepository;

    private User user;
    private Book bookA;
    private Book bookB;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("member@demo.io", "hash", "Member", Role.MEMBER));
        bookA = bookRepository.save(new Book("Clean Code", "Martin", "111", "d", "Software", 2008, 3));
        bookB = bookRepository.save(new Book("Sapiens", "Harari", "222", "d", "History", 2011, 5));
    }

    @Test
    void findByUserIdAndStatus_returnsOnlyMatchingStatus() {
        loanRepository.save(Loan.borrow(user, bookA, Instant.now(), 14));
        Loan returned = Loan.borrow(user, bookB, Instant.now(), 14);
        returned.markReturned(Instant.now());
        loanRepository.save(returned);

        List<Loan> active = loanRepository
                .findByUserIdAndStatusOrderByBorrowedAtDesc(user.getId(), LoanStatus.ACTIVE);

        assertThat(active).hasSize(1);
        assertThat(active.get(0).getBook().getTitle()).isEqualTo("Clean Code"); // book eagerly fetched
    }

    @Test
    void countByUserIdAndStatus() {
        loanRepository.save(Loan.borrow(user, bookA, Instant.now(), 14));
        loanRepository.save(Loan.borrow(user, bookB, Instant.now(), 14));

        assertThat(loanRepository.countByUserIdAndStatus(user.getId(), LoanStatus.ACTIVE)).isEqualTo(2);
        assertThat(loanRepository.countByUserIdAndStatus(user.getId(), LoanStatus.RETURNED)).isZero();
    }

    @Test
    void existsByUserIdAndBookIdAndStatus() {
        loanRepository.save(Loan.borrow(user, bookA, Instant.now(), 14));

        assertThat(loanRepository.existsByUserIdAndBookIdAndStatus(
                user.getId(), bookA.getId(), LoanStatus.ACTIVE)).isTrue();
        assertThat(loanRepository.existsByUserIdAndBookIdAndStatus(
                user.getId(), bookB.getId(), LoanStatus.ACTIVE)).isFalse();
    }

    @Test
    void findByIdAndUserId_scopesToOwner() {
        Loan loan = loanRepository.save(Loan.borrow(user, bookA, Instant.now(), 14));
        UUID otherUserId = UUID.randomUUID();

        assertThat(loanRepository.findByIdAndUserId(loan.getId(), user.getId())).isPresent();
        Optional<Loan> notOwned = loanRepository.findByIdAndUserId(loan.getId(), otherUserId);
        assertThat(notOwned).isEmpty();
    }
}
