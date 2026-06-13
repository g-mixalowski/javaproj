package com.example.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookRequest(
        @NotBlank String title,
        String description,
        @NotNull @Positive Integer year,
        @NotNull @Positive Integer pageCount,
        @NotNull Long authorId,
        String fb2Content
) {
}
