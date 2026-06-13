package com.example.library.repository;

import com.example.library.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByReaderId(Long readerId);

    List<Loan> findByReturnDateIsNull();

    List<Loan> findByReaderIdAndReturnDateIsNull(Long readerId);

    Optional<Loan> findByBookIdAndReturnDateIsNull(Long bookId);

    boolean existsByBookIdAndReturnDateIsNull(Long bookId);

    boolean existsByBookId(Long bookId);

    boolean existsByReaderId(Long readerId);

    boolean existsByBookIdAndReaderId(Long bookId, Long readerId);

    long countByBookId(Long bookId);

    long countByReaderId(Long readerId);
}
