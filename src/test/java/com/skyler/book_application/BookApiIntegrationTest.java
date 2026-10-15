package com.skyler.book_application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack test: the real controller, service, repository and H2 database, including the mock data
 * loaded by DataInitializer (12 books). These tests only read data, so they don't affect each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
class BookApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mockBooksAreLoadedAndPaginated() throws Exception {
        mockMvc.perform(get("/books/v1").param("page", "0").param("size", "5").param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.content[0].title").value("1984"))
                .andExpect(jsonPath("$.totalElements").value(12))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void titleLookupIsCaseInsensitive() throws Exception {
        mockMvc.perform(get("/books/v1/getBook/{name}", "clean code"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("Robert C. Martin"));
    }

    @Test
    void unknownTitleReturns404Json() throws Exception {
        mockMvc.perform(get("/books/v1/getBook/{name}", "Does Not Exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void sortingByUnknownFieldReturns400() throws Exception {
        mockMvc.perform(get("/books/v1").param("sort", "hacker,asc"))
                .andExpect(status().isBadRequest());
    }
}
