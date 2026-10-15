package com.skyler.book_application.Service;

import com.skyler.book_application.Dto.BookRequest;
import com.skyler.book_application.Dto.BookResponse;
import com.skyler.book_application.Dto.BookUpdateRequest;
import com.skyler.book_application.Dto.PageResponse;
import com.skyler.book_application.Entity.Book;
import com.skyler.book_application.Exceptions.BadRequestException;
import com.skyler.book_application.Exceptions.DuplicateResourceException;
import com.skyler.book_application.Exceptions.ResourceNotFoundException;
import com.skyler.book_application.Repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Pure unit tests: the repository is mocked, so no Spring context and no database are started. */
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    // ---------- addBook ----------

    @Test
    void addBook_trimsInputSavesAndReturnsResponse() {
        when(bookRepository.existsByTitleIgnoreCase("Refactoring")).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> {
            Book book = invocation.getArgument(0);
            book.setId(13); // simulate the id the database would generate
            return book;
        });

        BookResponse result = bookService.addBook(new BookRequest("  Refactoring ", " Martin Fowler ", "Programming"));

        assertThat(result.id()).isEqualTo(13);
        assertThat(result.title()).isEqualTo("Refactoring");
        assertThat(result.author()).isEqualTo("Martin Fowler");
        assertThat(result.genre()).isEqualTo("Programming");
    }

    @Test
    void addBook_duplicateTitle_throwsAndDoesNotSave() {
        when(bookRepository.existsByTitleIgnoreCase("Clean Code")).thenReturn(true);

        assertThatThrownBy(() -> bookService.addBook(new BookRequest("Clean Code", "Robert C. Martin", "Programming")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Clean Code");

        verify(bookRepository, never()).save(any(Book.class));
    }

    // ---------- getBook ----------

    @Test
    void getBook_found_returnsResponse() {
        when(bookRepository.findByTitleIgnoreCase("Dune"))
                .thenReturn(Optional.of(new Book(1, "Dune", "Frank Herbert", "Sci-Fi")));

        BookResponse result = bookService.getBook(" Dune "); // spaces are trimmed

        assertThat(result.id()).isEqualTo(1);
        assertThat(result.author()).isEqualTo("Frank Herbert");
    }

    @Test
    void getBook_missing_throwsNotFound() {
        when(bookRepository.findByTitleIgnoreCase("Missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getBook("Missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Missing");
    }

    // ---------- updateBook ----------

    @Test
    void updateBook_changesAllFields() {
        Book existing = new Book(1, "Old Title", "Old Author", "Old Genre");
        when(bookRepository.findById(1)).thenReturn(Optional.of(existing));
        when(bookRepository.findByTitleIgnoreCase("New Title")).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse result = bookService.updateBook(new BookUpdateRequest(1, "New Title", "New Author", "New Genre"));

        assertThat(result.id()).isEqualTo(1);
        assertThat(result.title()).isEqualTo("New Title");
        assertThat(result.author()).isEqualTo("New Author");
        assertThat(result.genre()).isEqualTo("New Genre");
    }

    @Test
    void updateBook_keepingOwnTitleIsAllowed() {
        Book existing = new Book(1, "Dune", "Frank Herbert", "Sci-Fi");
        when(bookRepository.findById(1)).thenReturn(Optional.of(existing));
        when(bookRepository.findByTitleIgnoreCase("Dune")).thenReturn(Optional.of(existing));
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BookResponse result = bookService.updateBook(new BookUpdateRequest(1, "Dune", "Frank Herbert", "Classic Sci-Fi"));

        assertThat(result.genre()).isEqualTo("Classic Sci-Fi");
    }

    @Test
    void updateBook_missingId_throwsNotFound() {
        when(bookRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.updateBook(new BookUpdateRequest(99, "Any", "Any", null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void updateBook_titleBelongsToAnotherBook_throwsDuplicate() {
        Book existing = new Book(1, "Book One", "Author", "Genre");
        Book other = new Book(2, "Book Two", "Author", "Genre");
        when(bookRepository.findById(1)).thenReturn(Optional.of(existing));
        when(bookRepository.findByTitleIgnoreCase("Book Two")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> bookService.updateBook(new BookUpdateRequest(1, "Book Two", "Author", "Genre")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(bookRepository, never()).save(any(Book.class));
    }

    // ---------- deleteBook ----------

    @Test
    void deleteBook_existing_deletesById() {
        when(bookRepository.existsById(1)).thenReturn(true);

        bookService.deleteBook(1);

        verify(bookRepository).deleteById(1);
    }

    @Test
    void deleteBook_missing_throwsNotFound() {
        when(bookRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> bookService.deleteBook(99))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(bookRepository, never()).deleteById(any(Integer.class));
    }

    // ---------- getAllBooks (pagination) ----------

    @Test
    void getAllBooks_returnsPageMetadata() {
        PageRequest pageRequest = PageRequest.of(0, 5, Sort.by("title"));
        List<Book> books = List.of(
                new Book(1, "1984", "George Orwell", "Dystopian"),
                new Book(2, "Atomic Habits", "James Clear", "Self-help"));
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(books, pageRequest, 12));

        PageResponse<BookResponse> result = bookService.getAllBooks(pageRequest);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).title()).isEqualTo("1984");
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalElements()).isEqualTo(12);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.last()).isFalse();
    }

    @Test
    void getAllBooks_capsPageSizeAt50() {
        when(bookRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<Book>(List.of()));

        bookService.getAllBooks(PageRequest.of(0, 500));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void getAllBooks_unknownSortField_throwsBadRequest() {
        assertThatThrownBy(() -> bookService.getAllBooks(PageRequest.of(0, 5, Sort.by("password"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("password");

        verifyNoInteractions(bookRepository);
    }
}
