package com.mirae.elibrary.web.mapper;

import com.mirae.elibrary.domain.Book;
import com.mirae.elibrary.domain.Loan;
import com.mirae.elibrary.web.dto.LoanResponse;

public final class LoanMapper {

    private LoanMapper() {
    }

    public static LoanResponse toResponse(Loan loan) {
        Book book = loan.getBook();
        return new LoanResponse(
                loan.getId(),
                loan.getStatus(),
                loan.getBorrowedAt(),
                loan.getDueAt(),
                loan.getReturnedAt(),
                new LoanResponse.BookSummary(
                        book.getId(),
                        book.getTitle(),
                        book.getAuthor(),
                        book.getIsbn()));
    }
}
