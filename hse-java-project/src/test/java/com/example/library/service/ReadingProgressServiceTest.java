package com.example.library.service;

import com.example.library.dto.ReadingProgressRequest;
import com.example.library.dto.ReadingProgressResponse;
import com.example.library.exception.ConflictException;
import com.example.library.exception.NotFoundException;
import com.example.library.model.Author;
import com.example.library.model.Book;
import com.example.library.model.Reader;
import com.example.library.model.ReadingProgress;
import com.example.library.repository.BookRepository;
import com.example.library.repository.ReaderRepository;
import com.example.library.repository.ReadingProgressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingProgressServiceTest {

    @Mock private ReadingProgressRepository progressRepository;
    @Mock private ReaderRepository readerRepository;
    @Mock private BookRepository bookRepository;
    @InjectMocks private ReadingProgressService service;

    private Reader reader;
    private Book book;

    @BeforeEach
    void setUp() {
        Author author = new Author("A", "B");
        author.setId(1L);
        reader = new Reader("R", "L", "r@x.com");
        reader.setId(1L);
        book = new Book("Война и мир", null, 1869, 1300, author);
        book.setId(1L);
    }

    @Test
    void listByReader_throwsWhenReaderMissing() {
        when(readerRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> service.listByReader(1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void get_throwsWhenProgressMissing() {
        when(readerRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.existsById(1L)).thenReturn(true);
        when(progressRepository.findByReaderIdAndBookId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Reading progress");
    }

    @Test
    void upsert_throwsWhenReaderMissing() {
        when(readerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(1L, 1L, new ReadingProgressRequest(10)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Reader 1");
    }

    @Test
    void upsert_throwsWhenBookMissing() {
        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(1L, 1L, new ReadingProgressRequest(10)))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Book 1");
    }

    @Test
    void upsert_throwsConflictWhenPageExceedsPageCount() {
        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> service.upsert(1L, 1L, new ReadingProgressRequest(5000)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("exceeds book pageCount");

        verify(progressRepository, never()).save(any());
    }

    @Test
    void upsert_createsNewIfNotExists() {
        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(progressRepository.findByReaderIdAndBookId(1L, 1L)).thenReturn(Optional.empty());
        when(progressRepository.save(any(ReadingProgress.class))).thenAnswer(inv -> {
            ReadingProgress p = inv.getArgument(0);
            p.setId(7L);
            return p;
        });

        ReadingProgressResponse resp = service.upsert(1L, 1L, new ReadingProgressRequest(9));

        assertThat(resp.id()).isEqualTo(7L);
        assertThat(resp.currentPage()).isEqualTo(9);
        assertThat(resp.totalPages()).isEqualTo(1300);
        assertThat(resp.updatedAt()).isNotNull();
    }

    @Test
    void upsert_updatesExisting() {
        ReadingProgress existing = new ReadingProgress(reader, book, 5, Instant.now().minusSeconds(60));
        existing.setId(3L);
        when(readerRepository.findById(1L)).thenReturn(Optional.of(reader));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(progressRepository.findByReaderIdAndBookId(1L, 1L)).thenReturn(Optional.of(existing));

        ReadingProgressResponse resp = service.upsert(1L, 1L, new ReadingProgressRequest(20));

        assertThat(resp.currentPage()).isEqualTo(20);
        assertThat(existing.getCurrentPage()).isEqualTo(20);
        verify(progressRepository, never()).save(any());
    }

    @Test
    void delete_throwsWhenMissing() {
        when(readerRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.existsById(1L)).thenReturn(true);
        when(progressRepository.findByReaderIdAndBookId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void delete_works() {
        ReadingProgress p = new ReadingProgress(reader, book, 9, Instant.now());
        p.setId(3L);
        when(readerRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.existsById(1L)).thenReturn(true);
        when(progressRepository.findByReaderIdAndBookId(1L, 1L)).thenReturn(Optional.of(p));

        service.delete(1L, 1L);

        verify(progressRepository).delete(p);
    }
}
