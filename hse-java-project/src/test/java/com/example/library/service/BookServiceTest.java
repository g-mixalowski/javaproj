package com.example.library.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Year;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private AuthorRepository authorRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private ReadingProgressRepository progressRepository;
    @InjectMocks private BookService service;

    private Author author;

    @BeforeEach
    void setUp() {
        author = new Author("Лев", "Толстой");
        author.setId(1L);
    }

    @Test
    void list_returnsAllBooksWithOwnershipCounts() {
        Book b = new Book("Война и мир", null, 1869, 1300, author, "<fb2/>\n");
        b.setId(1L);
        when(bookRepository.findAll()).thenReturn(List.of(b));
        when(loanRepository.countByBookId(1L)).thenReturn(2L);

        List<BookResponse> result = service.list(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Война и мир");
        assertThat(result.get(0).hasFb2()).isTrue();
        assertThat(result.get(0).ownersCount()).isEqualTo(2L);
    }

    @Test
    void getFb2_returnsContent() {
        Book b = new Book("Война и мир", null, 1869, 1300, author, "<fictionbook/>");
        b.setId(1L);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(b));

        assertThat(service.getFb2(1L).fb2Content()).isEqualTo("<fictionbook/>");
    }

    @Test
    void getFb2_throwsWhenMissing() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFb2(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Book 99");
    }

    @Test
    void create_throwsConflictWhenYearInFuture() {
        BookRequest req = new BookRequest("X", null, Year.now().getValue() + 1, 100, 1L, null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("must not be in the future");

        verify(bookRepository, never()).save(any());
    }

    @Test
    void create_throwsNotFoundWhenAuthorMissing() {
        BookRequest req = new BookRequest("X", null, 2000, 100, 99L, null);
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Author 99");
    }

    @Test
    void create_savesBook() {
        BookRequest req = new BookRequest("X", "desc", 2000, 100, 1L, "<fb2/>");
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> {
            Book b = inv.getArgument(0);
            b.setId(7L);
            return b;
        });

        BookResponse resp = service.create(req);

        assertThat(resp.id()).isEqualTo(7L);
        assertThat(resp.authorId()).isEqualTo(1L);
        assertThat(resp.hasFb2()).isTrue();
        assertThat(resp.available()).isTrue();
    }

    @Test
    void update_preservesExistingFb2WhenNotProvided() {
        Book b = new Book("Old", null, 2000, 100, author, "<fb2-old/>");
        b.setId(1L);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(b));
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        when(loanRepository.countByBookId(1L)).thenReturn(0L);

        service.update(1L, new BookRequest("New", null, 2001, 120, 1L, null));

        assertThat(b.getFb2Content()).isEqualTo("<fb2-old/>");
    }

    @Test
    void delete_throwsConflictWhenBookHasOwnerships() {
        Book b = new Book("X", null, 2000, 100, author);
        b.setId(1L);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(b));
        when(loanRepository.existsByBookId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ownership records");

        verify(bookRepository, never()).delete(any());
    }

    @Test
    void delete_throwsConflictWhenHasProgress() {
        Book b = new Book("X", null, 2000, 100, author);
        b.setId(1L);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(b));
        when(loanRepository.existsByBookId(1L)).thenReturn(false);
        when(progressRepository.existsByBookId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reading progress");
    }
}
