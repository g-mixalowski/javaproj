package com.example.library.dto;

import com.example.library.model.ReadingProgress;

import java.time.Instant;

public record ReadingProgressResponse(
        Long id,
        Long readerId,
        Long bookId,
        String bookTitle,
        Integer currentPage,
        Integer totalPages,
        Instant updatedAt
) {

    public static ReadingProgressResponse from(ReadingProgress progress) {
        return new ReadingProgressResponse(
                progress.getId(),
                progress.getReader().getId(),
                progress.getBook().getId(),
                progress.getBook().getTitle(),
                progress.getCurrentPage(),
                progress.getBook().getPageCount(),
                progress.getUpdatedAt()
        );
    }
}
