package com.example.library.service;

import com.example.library.dto.ReadingProgressRequest;
import com.example.library.dto.ReadingProgressResponse;
import com.example.library.exception.ConflictException;
import com.example.library.exception.NotFoundException;
import com.example.library.model.Book;
import com.example.library.model.Reader;
import com.example.library.model.ReadingProgress;
import com.example.library.repository.BookRepository;
import com.example.library.repository.ReaderRepository;
import com.example.library.repository.ReadingProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReadingProgressService {

    private final ReadingProgressRepository progressRepository;
    private final ReaderRepository readerRepository;
    private final BookRepository bookRepository;

    public ReadingProgressService(ReadingProgressRepository progressRepository,
                                  ReaderRepository readerRepository,
                                  BookRepository bookRepository) {
        this.progressRepository = progressRepository;
        this.readerRepository = readerRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public List<ReadingProgressResponse> listByReader(Long readerId) {
        ensureReaderExists(readerId);
        return progressRepository.findByReaderId(readerId).stream()
                .map(ReadingProgressResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReadingProgressResponse get(Long readerId, Long bookId) {
        ensureReaderExists(readerId);
        ensureBookExists(bookId);
        ReadingProgress progress = progressRepository.findByReaderIdAndBookId(readerId, bookId)
                .orElseThrow(() -> new NotFoundException(
                        "Reading progress for reader " + readerId + " and book " + bookId + " not found"));
        return ReadingProgressResponse.from(progress);
    }

    public ReadingProgressResponse upsert(Long readerId, Long bookId, ReadingProgressRequest req) {
        Reader reader = readerRepository.findById(readerId)
                .orElseThrow(() -> NotFoundException.of("Reader", readerId));
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> NotFoundException.of("Book", bookId));

        if (req.currentPage() > book.getPageCount()) {
            throw new ConflictException(
                    "currentPage " + req.currentPage() + " exceeds book pageCount " + book.getPageCount());
        }

        Optional<ReadingProgress> existing =
                progressRepository.findByReaderIdAndBookId(readerId, bookId);

        ReadingProgress progress = existing.orElseGet(
                () -> new ReadingProgress(reader, book, req.currentPage(), Instant.now()));
        progress.setCurrentPage(req.currentPage());
        progress.setUpdatedAt(Instant.now());

        if (existing.isEmpty()) {
            progress = progressRepository.save(progress);
        }
        return ReadingProgressResponse.from(progress);
    }

    public void delete(Long readerId, Long bookId) {
        ensureReaderExists(readerId);
        ensureBookExists(bookId);
        ReadingProgress progress = progressRepository.findByReaderIdAndBookId(readerId, bookId)
                .orElseThrow(() -> new NotFoundException(
                        "Reading progress for reader " + readerId + " and book " + bookId + " not found"));
        progressRepository.delete(progress);
    }

    private void ensureReaderExists(Long readerId) {
        if (!readerRepository.existsById(readerId)) {
            throw NotFoundException.of("Reader", readerId);
        }
    }

    private void ensureBookExists(Long bookId) {
        if (!bookRepository.existsById(bookId)) {
            throw NotFoundException.of("Book", bookId);
        }
    }
}
