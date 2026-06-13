package com.example.library.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthorRequest(
        @NotBlank String firstName,
        @NotBlank String lastName
) {
}
