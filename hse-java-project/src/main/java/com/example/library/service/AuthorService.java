package com.example.library.service;

import com.example.library.dto.AuthorRequest;
import com.example.library.dto.AuthorResponse;
import com.example.library.exception.ConflictException;
import com.example.library.exception.NotFoundException;
import com.example.library.model.Author;
import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;

    public AuthorService(AuthorRepository authorRepository, BookRepository bookRepository) {
        this.authorRepository = authorRepository;
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public List<AuthorResponse> list() {
        return authorRepository.findAll().stream().map(AuthorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AuthorResponse get(Long id) {
        return AuthorResponse.from(findOrThrow(id));
    }

    public AuthorResponse create(AuthorRequest req) {
        Author author = new Author(req.firstName(), req.lastName());
        return AuthorResponse.from(authorRepository.save(author));
    }

    public AuthorResponse update(Long id, AuthorRequest req) {
        Author author = findOrThrow(id);
        author.setFirstName(req.firstName());
        author.setLastName(req.lastName());
        return AuthorResponse.from(author);
    }

    public void delete(Long id) {
        Author author = findOrThrow(id);
        if (bookRepository.existsByAuthorId(id)) {
            throw new ConflictException("Author " + id + " has books and cannot be deleted");
        }
        authorRepository.delete(author);
    }

    private Author findOrThrow(Long id) {
        return authorRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Author", id));
    }
}
