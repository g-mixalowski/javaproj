package com.example.library.service;

import com.example.library.dto.ReaderRequest;
import com.example.library.dto.ReaderResponse;
import com.example.library.exception.ConflictException;
import com.example.library.exception.NotFoundException;
import com.example.library.model.Reader;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.ReaderRepository;
import com.example.library.repository.ReadingProgressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ReaderService {

    private final ReaderRepository readerRepository;
    private final LoanRepository loanRepository;
    private final ReadingProgressRepository progressRepository;

    public ReaderService(ReaderRepository readerRepository,
                         LoanRepository loanRepository,
                         ReadingProgressRepository progressRepository) {
        this.readerRepository = readerRepository;
        this.loanRepository = loanRepository;
        this.progressRepository = progressRepository;
    }

    @Transactional(readOnly = true)
    public List<ReaderResponse> list() {
        return readerRepository.findAll().stream().map(ReaderResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ReaderResponse get(Long id) {
        return ReaderResponse.from(findOrThrow(id));
    }

    public ReaderResponse create(ReaderRequest req) {
        if (readerRepository.existsByEmail(req.email())) {
            throw new ConflictException("Reader with email " + req.email() + " already exists");
        }
        Reader reader = new Reader(req.firstName(), req.lastName(), req.email());
        return ReaderResponse.from(readerRepository.save(reader));
    }

    public ReaderResponse update(Long id, ReaderRequest req) {
        Reader reader = findOrThrow(id);
        Optional<Reader> sameEmail = readerRepository.findByEmail(req.email());
        if (sameEmail.isPresent() && !sameEmail.get().getId().equals(id)) {
            throw new ConflictException("Email " + req.email() + " is already taken");
        }
        reader.setFirstName(req.firstName());
        reader.setLastName(req.lastName());
        reader.setEmail(req.email());
        return ReaderResponse.from(reader);
    }

    public void delete(Long id) {
        Reader reader = findOrThrow(id);
        if (loanRepository.existsByReaderId(id)) {
            throw new ConflictException(
                    "Reader " + id + " has ownership records and cannot be deleted");
        }
        if (progressRepository.existsByReaderId(id)) {
            throw new ConflictException(
                    "Reader " + id + " has reading progress entries and cannot be deleted");
        }
        readerRepository.delete(reader);
    }

    private Reader findOrThrow(Long id) {
        return readerRepository.findById(id)
                .orElseThrow(() -> NotFoundException.of("Reader", id));
    }
}
