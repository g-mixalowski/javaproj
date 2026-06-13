package com.example.library.dto;

import com.example.library.model.Book;

public record BookResponse(
        Long id,
        String title,
        String description,
        Integer year,
        Integer pageCount,
        Long authorId,
        String authorName,
        boolean available,
        boolean hasFb2,
        long ownersCount
) {

    public static BookResponse from(Book book, boolean available, long ownersCount) {
        String authorName = book.getAuthor().getFirstName() + " " + book.getAuthor().getLastName();
        boolean hasFb2 = book.getFb2Content() != null && !book.getFb2Content().isBlank();
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getDescription(),
                book.getYear(),
                book.getPageCount(),
                book.getAuthor().getId(),
                authorName,
                available,
                hasFb2,
                ownersCount
        );
    }
}
