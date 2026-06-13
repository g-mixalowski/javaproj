package com.example.library.dto;

import jakarta.validation.constraints.NotNull;

public record LoanCreateRequest(
        @NotNull Long bookId,
        @NotNull Long readerId
) {
}
