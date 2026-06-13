package com.example.library.repository;

import com.example.library.model.ReadingProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReadingProgressRepository extends JpaRepository<ReadingProgress, Long> {

    List<ReadingProgress> findByReaderId(Long readerId);

    Optional<ReadingProgress> findByReaderIdAndBookId(Long readerId, Long bookId);

    void deleteByReaderIdAndBookId(Long readerId, Long bookId);

    boolean existsByReaderId(Long readerId);

    boolean existsByBookId(Long bookId);
}
