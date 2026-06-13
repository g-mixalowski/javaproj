package com.example.library.controller;

import com.example.library.dto.ReadingProgressRequest;
import com.example.library.dto.ReadingProgressResponse;
import com.example.library.service.ReadingProgressService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/readers/{readerId}/progress")
public class ReadingProgressController {

    private final ReadingProgressService service;

    public ReadingProgressController(ReadingProgressService service) {
        this.service = service;
    }

    @GetMapping
    public List<ReadingProgressResponse> list(@PathVariable Long readerId) {
        return service.listByReader(readerId);
    }

    @GetMapping("/{bookId}")
    public ReadingProgressResponse get(@PathVariable Long readerId, @PathVariable Long bookId) {
        return service.get(readerId, bookId);
    }

    @PutMapping("/{bookId}")
    public ReadingProgressResponse upsert(@PathVariable Long readerId,
                                          @PathVariable Long bookId,
                                          @Valid @RequestBody ReadingProgressRequest req) {
        return service.upsert(readerId, bookId, req);
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> delete(@PathVariable Long readerId, @PathVariable Long bookId) {
        service.delete(readerId, bookId);
        return ResponseEntity.noContent().build();
    }
}
