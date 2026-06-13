package com.example.library.controller;

import com.example.library.dto.LoanCreateRequest;
import com.example.library.dto.LoanResponse;
import com.example.library.service.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService service;

    public LoanController(LoanService service) {
        this.service = service;
    }

    @GetMapping
    public List<LoanResponse> list(@RequestParam(required = false) Long readerId,
                                   @RequestParam(required = false) Boolean active) {
        return service.list(readerId, active);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LoanResponse create(@Valid @RequestBody LoanCreateRequest req) {
        return service.create(req);
    }

    @PostMapping("/{id}/return")
    public LoanResponse markReturned(@PathVariable Long id) {
        return service.markReturned(id);
    }
}
