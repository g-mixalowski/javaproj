package com.example.library.service;

import com.example.library.dto.AuthorRequest;
import com.example.library.dto.AuthorResponse;
import com.example.library.exception.ConflictException;
import com.example.library.exception.NotFoundException;
import com.example.library.model.Author;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
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
class AuthorServiceTest {

    @Mock private AuthorRepository authorRepository;
    @Mock private BookRepository bookRepository;
    @InjectMocks private AuthorService service;

    @Test
    void list_returnsAllAuthors() {
        Author a = new Author("Лев", "Толстой");
        a.setId(1L);
        when(authorRepository.findAll()).thenReturn(List.of(a));

        List<AuthorResponse> result = service.list();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).firstName()).isEqualTo("Лев");
    }

    @Test
    void get_throwsNotFoundWhenMissing() {
        when(authorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Author 99");
    }

    @Test
    void create_savesAndReturnsAuthor() {
        AuthorRequest req = new AuthorRequest("Антон", "Чехов");
        when(authorRepository.save(any(Author.class))).thenAnswer(inv -> {
            Author a = inv.getArgument(0);
            a.setId(5L);
            return a;
        });

        AuthorResponse resp = service.create(req);

        assertThat(resp.id()).isEqualTo(5L);
        assertThat(resp.firstName()).isEqualTo("Антон");
        assertThat(resp.lastName()).isEqualTo("Чехов");
    }

    @Test
    void update_changesFields() {
        Author a = new Author("Old", "Name");
        a.setId(1L);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(a));

        AuthorResponse resp = service.update(1L, new AuthorRequest("New", "Surname"));

        assertThat(resp.firstName()).isEqualTo("New");
        assertThat(resp.lastName()).isEqualTo("Surname");
        assertThat(a.getFirstName()).isEqualTo("New");
    }

    @Test
    void delete_worksWhenNoBooks() {
        Author a = new Author("A", "B");
        a.setId(1L);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(a));
        when(bookRepository.existsByAuthorId(1L)).thenReturn(false);

        service.delete(1L);

        verify(authorRepository).delete(a);
    }

    @Test
    void delete_throwsConflictWhenAuthorHasBooks() {
        Author a = new Author("A", "B");
        a.setId(1L);
        when(authorRepository.findById(1L)).thenReturn(Optional.of(a));
        when(bookRepository.existsByAuthorId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("has books");

        verify(authorRepository, never()).delete(any());
    }
}
