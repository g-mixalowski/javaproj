package com.example.library.service;

import com.example.library.dto.ReaderRequest;
import com.example.library.dto.ReaderResponse;
import com.example.library.exception.ConflictException;
import com.example.library.exception.NotFoundException;
import com.example.library.model.Reader;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.ReaderRepository;
import com.example.library.repository.ReadingProgressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReaderServiceTest {

    @Mock private ReaderRepository readerRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private ReadingProgressRepository progressRepository;
    @InjectMocks private ReaderService service;

    @Test
    void create_throwsConflictOnDuplicateEmail() {
        ReaderRequest req = new ReaderRequest("A", "B", "a@b.c");
        when(readerRepository.existsByEmail("a@b.c")).thenReturn(true);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already exists");

        verify(readerRepository, never()).save(any());
    }

    @Test
    void create_savesReader() {
        ReaderRequest req = new ReaderRequest("A", "B", "a@b.c");
        when(readerRepository.existsByEmail("a@b.c")).thenReturn(false);
        when(readerRepository.save(any(Reader.class))).thenAnswer(inv -> {
            Reader r = inv.getArgument(0);
            r.setId(10L);
            return r;
        });

        ReaderResponse resp = service.create(req);

        assertThat(resp.id()).isEqualTo(10L);
        assertThat(resp.email()).isEqualTo("a@b.c");
    }

    @Test
    void update_throwsConflictWhenEmailTakenByAnotherReader() {
        Reader existing = new Reader("A", "B", "old@x.com");
        existing.setId(1L);
        Reader other = new Reader("C", "D", "new@x.com");
        other.setId(2L);
        when(readerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(readerRepository.findByEmail("new@x.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() ->
                service.update(1L, new ReaderRequest("A", "B", "new@x.com")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already taken");
    }

    @Test
    void update_allowsUsingOwnEmail() {
        Reader existing = new Reader("A", "B", "me@x.com");
        existing.setId(1L);
        when(readerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(readerRepository.findByEmail("me@x.com")).thenReturn(Optional.of(existing));

        ReaderResponse resp = service.update(1L, new ReaderRequest("Updated", "Name", "me@x.com"));

        assertThat(resp.firstName()).isEqualTo("Updated");
    }

    @Test
    void delete_throwsConflictWhenHasOwnershipRecords() {
        Reader r = new Reader("A", "B", "a@b.c");
        r.setId(1L);
        when(readerRepository.findById(1L)).thenReturn(Optional.of(r));
        when(loanRepository.existsByReaderId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("ownership records");
    }

    @Test
    void delete_throwsConflictWhenHasProgress() {
        Reader r = new Reader("A", "B", "a@b.c");
        r.setId(1L);
        when(readerRepository.findById(1L)).thenReturn(Optional.of(r));
        when(loanRepository.existsByReaderId(1L)).thenReturn(false);
        when(progressRepository.existsByReaderId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reading progress");
    }

    @Test
    void delete_worksWhenClean() {
        Reader r = new Reader("A", "B", "a@b.c");
        r.setId(1L);
        when(readerRepository.findById(1L)).thenReturn(Optional.of(r));
        when(loanRepository.existsByReaderId(1L)).thenReturn(false);
        when(progressRepository.existsByReaderId(1L)).thenReturn(false);

        service.delete(1L);

        verify(readerRepository).delete(r);
    }

    @Test
    void get_throwsNotFoundWhenMissing() {
        when(readerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Reader 99");
    }
}
