package com.example.library.dto;

import com.example.library.model.Loan;

import java.time.LocalDate;

public record LoanResponse(
        Long id,
        Long bookId,
        String bookTitle,
        Long readerId,
        String readerName,
        LocalDate loanDate,
        LocalDate returnDate,
        boolean active
) {

    public static LoanResponse from(Loan loan) {
        String readerName = loan.getReader().getFirstName() + " " + loan.getReader().getLastName();
        return new LoanResponse(
                loan.getId(),
                loan.getBook().getId(),
                loan.getBook().getTitle(),
                loan.getReader().getId(),
                readerName,
                loan.getLoanDate(),
                loan.getReturnDate(),
                loan.isActive()
        );
    }
}
