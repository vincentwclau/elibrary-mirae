package com.mirae.elibrary.web.controller;

import com.mirae.elibrary.domain.LoanStatus;
import com.mirae.elibrary.security.AppUserDetails;
import com.mirae.elibrary.service.LoanService;
import com.mirae.elibrary.web.dto.BorrowRequest;
import com.mirae.elibrary.web.dto.LoanResponse;
import com.mirae.elibrary.web.mapper.LoanMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Loan operations for the authenticated member. The current user is resolved
 * from the JWT principal, so a member can only ever act on their own loans.
 */
@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @GetMapping("/me")
    public List<LoanResponse> myLoans(@AuthenticationPrincipal AppUserDetails principal,
                                      @RequestParam(required = false) LoanStatus status) {
        return loanService.getUserLoans(principal.getId(), status).stream()
                .map(LoanMapper::toResponse)
                .toList();
    }

    @PostMapping
    public ResponseEntity<LoanResponse> borrow(@AuthenticationPrincipal AppUserDetails principal,
                                               @Valid @RequestBody BorrowRequest request) {
        LoanResponse response = LoanMapper.toResponse(
                loanService.borrow(principal.getId(), request.bookId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/return")
    public LoanResponse returnLoan(@AuthenticationPrincipal AppUserDetails principal,
                                   @PathVariable UUID id) {
        return LoanMapper.toResponse(loanService.returnLoan(principal.getId(), id));
    }
}
