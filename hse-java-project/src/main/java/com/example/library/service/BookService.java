package com.example.library.service;

import com.example.library.dto.BookFb2Response;
import com.example.library.dto.BookRequest;
import com.example.library.dto.BookResponse;
import com.example.library.exception.ConflictException;
import com.example.library.exception.NotFoundException;
import com.example.library.model.Author;
import com.example.library.model.Book;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.ReadingProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;

@Service
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final LoanRepository loanRepository;
    private final ReadingProgressRepository progressRepository;

    public BookService(BookRepository bookRepository,
                       AuthorRepository authorRepository,
                       LoanRepository loanRepository,
                       ReadingProgressRepository progressRepository) {
        this.bookRepository = bookRepository;
        this.authorRepository = authorRepository;
        this.loanRepository = loanRepository;
        this.progressRepository = progressRepository;
    }

    @Transactional(readOnly = true)
    public List<BookResponse> list(Long authorId, Boolean availableOnly) {
        List<Book> books = (authorId != null)
                ? bookRepository.findByAuthorId(authorId)
                : bookRepository.findAll();

        return books.stream()
                .map(b -> BookResponse.from(b, true, loanRepository.countByBookId(b.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public BookResponse get(Long id) {
        Book book = findOrThrow(id);
        return BookResponse.from(book, true, loanRepository.countByBookId(id));
    }

    @Transactional(readOnly = true)
    public BookFb2Response getFb2(Long id) {
        Book book = findOrThrow(id);
        if (book.getFb2Content() == null || book.getFb2Content().isBlank()) {
            throw NotFoundException.of("FB2 content for book", id);
        }
        return BookFb2Response.from(book);
    }

    public BookResponse create(BookRequest req) {
        validateYear(req.year());
        Author author = authorRepository.findById(req.authorId())
                .orElseThrow(() -> NotFoundException.of("Author", req.authorId()));
        Book book = new Book(req.title(), req.description(), req.year(), req.pageCount(), author, req.fb2Content());
        return BookResponse.from(bookRepository.save(book), true, 0L);
    }

    public BookResponse update(Long id, BookRequest req) {
        validateYear(req.year());
        Book book = findOrThrow(id);
        Author author = authorRepository.findById(req.authorId())
                .orElseThrow(() -> NotFoundException.of("Author", req.authorId()));
        book.setTitle(req.title());
        book.setDescription(req.description());
        book.setYear(req.year());
        book.setPageCount(req.pageCount());
        book.setAuthor(author);
        if (req.fb2Content() != null) {
            book.setFb2Content(req.fb2Content());
        }
        return BookResponse.from(book, true, loanRepository.countByBookId(id));
    }

    public void delete(Long id) {
        Book book = findOrThrow(id);
        if (loanRepository.existsByBookId(id)) {
            throw new ConflictException("Book " + id + " has ownership records and cannot be deleted");
        }
        if (progressRepository.existsByBookId(id)) {
            throw new ConflictException(
                    "Book " + id + " has reading progress entries and cannot be deleted");
        }
        bookRepository.delete(book);
    }

    private void validateYear(Integer year) {
        int currentYear = Year.now().getValue();
        if (year > currentYear) {
            throw new ConflictException("Year " + year + " must not be in the future (current year " + currentYear + ")");
        }
    }

    private Book findOrThrow(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Book", id));
    }
}
