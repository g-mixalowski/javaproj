package com.example.library.service;

import com.example.library.dto.LoanCreateRequest;
import com.example.library.exception.ConflictException;
import com.example.library.exception.NotFoundException;
import com.example.library.model.Author;
import com.example.library.model.Book;
import com.example.library.model.Loan;
import com.example.library.model.Reader;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.ReaderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private BookRepository bookRepository;
    @Mock private ReaderRepository readerRepository;
    @InjectMocks private LoanService service;

    private Book book;
    private Reader reader;
    private Reader otherReader;

    @BeforeEach
    void setUp() {
        Author author = new Author("A", "B");
        author.setId(1L);
        book = new Book("Title", null, 2000, 100, author);
        book.setId(1L);
        reader = new Reader("R", "L", "r@x.com");
        reader.setId(1L);
        otherReader = new Reader("R2", "L2", "r2@x.com");
        otherReader.setId(2L);
    }

    @Test
    void create_throwsNotFoundWhenBookMissing() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new LoanCreateRequest(1L, 1L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Book 1");
    }

    @Test
    void create_throwsNotFoundWhenReaderMissing() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(readerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new LoanCreateRequest(1L, 1L)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Reader 1");
    }

    @Test
    void create_allowsSeveralReadersForSameBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(loanRepository.existsByBookIdAndReaderId(1L, 1L)).thenReturn(false);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setId(42L);
            return l;
        });

        var resp = service.create(new LoanCreateRequest(1L, 1L));

        assertThat(resp.id()).isEqualTo(42L);
        assertThat(resp.active()).isTrue();
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    void create_throwsConflictWhenReaderAlreadyOwnsSameBook() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(loanRepository.existsByBookIdAndReaderId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.create(new LoanCreateRequest(1L, 1L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already owns");

        verify(loanRepository, never()).save(any());
    }

    @Test
    void create_doesNotBlockSecondReader() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(readerRepository.findById(2L)).thenReturn(Optional.of(otherReader));
        when(loanRepository.existsByBookIdAndReaderId(1L, 2L)).thenReturn(false);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.create(new LoanCreateRequest(1L, 2L));

        assertThat(resp.readerId()).isEqualTo(2L);
    }

    @Test
    void markReturned_isNotSupported() {
        assertThatThrownBy(() -> service.markReturned(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("perpetual");
    }
}
