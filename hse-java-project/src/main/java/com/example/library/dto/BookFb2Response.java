package com.example.library.dto;

import com.example.library.model.Book;

public record BookFb2Response(
        Long bookId,
        String title,
        String fb2Content
) {
    public static BookFb2Response from(Book book) {
        return new BookFb2Response(book.getId(), book.getTitle(), book.getFb2Content());
    }
}
