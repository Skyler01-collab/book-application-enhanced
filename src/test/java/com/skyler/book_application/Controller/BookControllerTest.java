package com.skyler.book_application.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skyler.book_application.Dto.BookRequest;
import com.skyler.book_application.Dto.BookResponse;
import com.skyler.book_application.Dto.BookUpdateRequest;
import com.skyler.book_application.Dto.PageResponse;
import com.skyler.book_application.Exceptions.BadRequestException;
import com.skyler.book_application.Exceptions.DuplicateResourceException;
import com.skyler.book_application.Exceptions.ResourceNotFoundException;
import com.skyler.book_application.Service.BookService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests: only the controller, validation and the exception handler are loaded.
 * The service is a mock, so these tests check HTTP behaviour (status codes and JSON), not business logic.
 */
@WebMvcTest(BookController.class)
class BookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookService bookService;

    // ---------- POST /books/v1/addBook ----------

    @Test
    void addBook_validRequest_returns201() throws Exception {
        BookRequest request = new BookRequest("Refactoring", "Martin Fowler", "Programming");
        when(bookService.addBook(any(BookRequest.class)))
                .thenReturn(new BookResponse(13, "Refactoring", "Martin Fowler", "Programming"));

        mockMvc.perform(post("/books/v1/addBook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(13))
                .andExpect(jsonPath("$.title").value("Refactoring"));
    }

    @Test
    void addBook_blankTitle_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/books/v1/addBook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"author\":\"Someone\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.title").value("Title is required"));

        verifyNoInteractions(bookService);
    }

    @Test
    void addBook_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/books/v1/addBook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(bookService);
    }

    @Test
    void addBook_duplicateTitle_returns409() throws Exception {
        when(bookService.addBook(any(BookRequest.class)))
                .thenThrow(new DuplicateResourceException("A book titled 'Clean Code' already exists"));

        mockMvc.perform(post("/books/v1/addBook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Clean Code\",\"author\":\"Robert C. Martin\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("A book titled 'Clean Code' already exists"))
                .andExpect(jsonPath("$.path").value("/books/v1/addBook"));
    }

    // ---------- GET /books/v1/getBook/{name} ----------

    @Test
    void getBook_found_returns200() throws Exception {
        when(bookService.getBook("Dune")).thenReturn(new BookResponse(1, "Dune", "Frank Herbert", "Sci-Fi"));

        mockMvc.perform(get("/books/v1/getBook/{name}", "Dune"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.author").value("Frank Herbert"));
    }

    @Test
    void getBook_missing_returns404() throws Exception {
        when(bookService.getBook("Nope")).thenThrow(new ResourceNotFoundException("Book with title 'Nope' not found"));

        mockMvc.perform(get("/books/v1/getBook/{name}", "Nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Book with title 'Nope' not found"));
    }

    // ---------- PUT /books/v1/updateBook ----------

    @Test
    void updateBook_validRequest_returns200() throws Exception {
        BookUpdateRequest request = new BookUpdateRequest(1, "Dune", "Frank Herbert", "Classic");
        when(bookService.updateBook(any(BookUpdateRequest.class)))
                .thenReturn(new BookResponse(1, "Dune", "Frank Herbert", "Classic"));

        mockMvc.perform(put("/books/v1/updateBook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.genre").value("Classic"));
    }

    @Test
    void updateBook_missingId_returns400() throws Exception {
        mockMvc.perform(put("/books/v1/updateBook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Dune\",\"author\":\"Frank Herbert\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.id").value("Id is required"));

        verifyNoInteractions(bookService);
    }

    @Test
    void updateBook_unknownId_returns404() throws Exception {
        when(bookService.updateBook(any(BookUpdateRequest.class)))
                .thenThrow(new ResourceNotFoundException("Book with id 99 not found"));

        mockMvc.perform(put("/books/v1/updateBook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":99,\"title\":\"Any\",\"author\":\"Any\"}"))
                .andExpect(status().isNotFound());
    }

    // ---------- DELETE /books/v1/deleteBook/{id} ----------

    @Test
    void deleteBook_existing_returns204() throws Exception {
        mockMvc.perform(delete("/books/v1/deleteBook/{id}", 1))
                .andExpect(status().isNoContent());

        verify(bookService).deleteBook(1);
    }

    @Test
    void deleteBook_missing_returns404() throws Exception {
        doThrow(new ResourceNotFoundException("Book with id 99 not found")).when(bookService).deleteBook(99);

        mockMvc.perform(delete("/books/v1/deleteBook/{id}", 99))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBook_nonNumericId_returns400() throws Exception {
        mockMvc.perform(delete("/books/v1/deleteBook/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter 'id' must be Integer"));

        verifyNoInteractions(bookService);
    }

    // ---------- GET /books/v1 (pagination) ----------

    @Test
    void getAllBooks_returnsPage() throws Exception {
        PageResponse<BookResponse> page = new PageResponse<>(
                List.of(new BookResponse(1, "1984", "George Orwell", "Dystopian")), 0, 10, 1, 1, true);
        when(bookService.getAllBooks(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/books/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("1984"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void getAllBooks_passesPageSizeAndSortToService() throws Exception {
        when(bookService.getAllBooks(any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 1, 5, 0, 0, true));

        mockMvc.perform(get("/books/v1").param("page", "1").param("size", "5").param("sort", "title,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookService).getAllBooks(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getSort().getOrderFor("title").getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    void getAllBooks_invalidSort_returns400() throws Exception {
        when(bookService.getAllBooks(any(Pageable.class)))
                .thenThrow(new BadRequestException("Cannot sort by 'hacker'"));

        mockMvc.perform(get("/books/v1").param("sort", "hacker,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot sort by 'hacker'"));
    }
}
