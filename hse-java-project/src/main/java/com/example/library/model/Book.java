package com.example.library.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @NotNull
    @Positive
    @Column(name = "publication_year", nullable = false)
    private Integer year;

    @NotNull
    @Positive
    @Column(nullable = false)
    private Integer pageCount;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Author author;

    @Lob
    @Column(name = "fb2_content")
    private String fb2Content;

    public Book() {
    }

    public Book(String title, String description, Integer year, Integer pageCount, Author author) {
        this(title, description, year, pageCount, author, null);
    }

    public Book(String title, String description, Integer year, Integer pageCount, Author author, String fb2Content) {
        this.title = title;
        this.description = description;
        this.year = year;
        this.pageCount = pageCount;
        this.author = author;
        this.fb2Content = fb2Content;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getPageCount() {
        return pageCount;
    }

    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }

    public Author getAuthor() {
        return author;
    }

    public void setAuthor(Author author) {
        this.author = author;
    }

    public String getFb2Content() {
        return fb2Content;
    }

    public void setFb2Content(String fb2Content) {
        this.fb2Content = fb2Content;
    }
}
