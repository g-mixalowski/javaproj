package com.example.library.dto;

import com.example.library.model.Author;

public record AuthorResponse(Long id, String firstName, String lastName) {

    public static AuthorResponse from(Author author) {
        return new AuthorResponse(author.getId(), author.getFirstName(), author.getLastName());
    }
}
