package com.example.library.controller;

import com.example.library.repository.AuthorRepository;
import com.example.library.repository.BookRepository;
import com.example.library.repository.LoanRepository;
import com.example.library.repository.ReaderRepository;
import com.example.library.repository.ReadingProgressRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LibraryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private ReadingProgressRepository progressRepository;
    @Autowired private LoanRepository loanRepository;
    @Autowired private BookRepository bookRepository;
    @Autowired private ReaderRepository readerRepository;
    @Autowired private AuthorRepository authorRepository;

    @BeforeEach
    void cleanDatabase() {
        progressRepository.deleteAll();
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        readerRepository.deleteAll();
        authorRepository.deleteAll();
    }

    @Test
    void fullScenario_multipleOwnersAndFb2Reader() throws Exception {
        Long authorId = createAuthor("Лев", "Толстой");
        String fb2 = """
                <?xml version="1.0" encoding="UTF-8"?>
                <FictionBook xmlns="http://www.gribuser.ru/xml/fictionbook/2.0">
                  <description>
                    <title-info>
                      <book-title>Война и мир</book-title>
                      <author><first-name>Лев</first-name><last-name>Толстой</last-name></author>
                    </title-info>
                  </description>
                  <body>
                    <section>
                      <title><p>Глава 1</p></title>
                      <p>Текст книги.</p>
                    </section>
                  </body>
                </FictionBook>
                """;
        Long bookId = createBook("Война и мир", "Роман-эпопея", 1869, 1300, authorId, fb2);

        mockMvc.perform(get("/api/books/" + bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasFb2").value(true))
                .andExpect(jsonPath("$.ownersCount").value(0));

        Long reader1 = createReader("Иван", "Иванов", "ivan@example.com");
        Long reader2 = createReader("Пётр", "Петров", "petr@example.com");

        createOwnership(bookId, reader1);
        createOwnership(bookId, reader2);

        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": %d, "readerId": %d}
                                """.formatted(bookId, reader1)))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/books/" + bookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownersCount").value(2));

        mockMvc.perform(get("/api/books/" + bookId + "/fb2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fb2Content").value(org.hamcrest.Matchers.containsString("Текст книги")));

        mockMvc.perform(put("/api/readers/" + reader1 + "/progress/" + bookId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPage\": 9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(9));

        mockMvc.perform(delete("/api/books/" + bookId))
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/readers/" + reader1))
                .andExpect(status().isConflict());
    }

    @Test
    void books_filterByAuthorStillWorks() throws Exception {
        Long aId = createAuthor("Лев", "Толстой");
        createBook("A", null, 2000, 100, aId, null);
        createBook("B", null, 2000, 100, aId, null);

        mockMvc.perform(get("/api/books?authorId=" + aId))
                .andExpect(jsonPath("$.length()").value(2));
    }

    private Long createAuthor(String firstName, String lastName) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "%s", "lastName": "%s"}
                                """.formatted(firstName, lastName)))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(res);
    }

    private Long createBook(String title, String description, int year, int pageCount, Long authorId, String fb2Content) throws Exception {
        String descJson = (description == null) ? "null" : objectMapper.writeValueAsString(description);
        String fb2Json = (fb2Content == null) ? "null" : objectMapper.writeValueAsString(fb2Content);
        MvcResult res = mockMvc.perform(post("/api/books")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "description": %s, "year": %d, "pageCount": %d, "authorId": %d, "fb2Content": %s}
                                """.formatted(title, descJson, year, pageCount, authorId, fb2Json)))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(res);
    }

    private Long createReader(String firstName, String lastName, String email) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/readers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName": "%s", "lastName": "%s", "email": "%s"}
                                """.formatted(firstName, lastName, email)))
                .andExpect(status().isCreated())
                .andReturn();
        return idFrom(res);
    }

    private void createOwnership(Long bookId, Long readerId) throws Exception {
        mockMvc.perform(post("/api/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"bookId": %d, "readerId": %d}
                                """.formatted(bookId, readerId)))
                .andExpect(status().isCreated());
    }

    private Long idFrom(MvcResult res) throws Exception {
        JsonNode node = objectMapper.readTree(res.getResponse().getContentAsString());
        assertThat(node.has("id")).isTrue();
        return node.get("id").asLong();
    }
}
