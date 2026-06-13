package com.example.library.service;

import com.example.library.dto.LoanCreateRequest;
import com.example.library.dto.LoanResponse;
import com.example.library.exception.ConflictException;
import com.example.library.exception.NotFoundException;
import com.example.library.model.Book;
import com.example.library.model.Loan;
import com.example.library.model.Reader;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.ReaderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;

    public LoanService(LoanRepository loanRepository,
                       BookRepository bookRepository,
                       ReaderRepository readerRepository) {
        this.loanRepository = loanRepository;
        this.bookRepository = bookRepository;
        this.readerRepository = readerRepository;
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> list(Long readerId, Boolean activeOnly) {
        boolean active = activeOnly != null && activeOnly;
        List<Loan> loans;
        if (readerId != null && active) {
            loans = loanRepository.findByReaderIdAndReturnDateIsNull(readerId);
        } else if (readerId != null) {
            loans = loanRepository.findByReaderId(readerId);
        } else if (active) {
            loans = loanRepository.findByReturnDateIsNull();
        } else {
            loans = loanRepository.findAll();
        }
        return loans.stream().map(LoanResponse::from).toList();
    }

    public LoanResponse create(LoanCreateRequest req) {
        Book book = bookRepository.findById(req.bookId())
                .orElseThrow(() -> NotFoundException.of("Book", req.bookId()));
        Reader reader = readerRepository.findById(req.readerId())
                .orElseThrow(() -> NotFoundException.of("Reader", req.readerId()));

        if (loanRepository.existsByBookIdAndReaderId(req.bookId(), req.readerId())) {
            throw new ConflictException("Reader " + req.readerId() + " already owns book " + req.bookId());
        }

        Loan loan = new Loan(book, reader, LocalDate.now());
        return LoanResponse.from(loanRepository.save(loan));
    }

    public LoanResponse markReturned(Long loanId) {
        throw new ConflictException("Ownership is perpetual and cannot be returned");
    }
}
