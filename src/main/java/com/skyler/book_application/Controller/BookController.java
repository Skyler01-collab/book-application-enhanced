package com.skyler.book_application.Controller;

import com.skyler.book_application.Dto.BookRequest;
import com.skyler.book_application.Dto.BookResponse;
import com.skyler.book_application.Dto.BookUpdateRequest;
import com.skyler.book_application.Dto.PageResponse;
import com.skyler.book_application.Service.BookService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/books/v1")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    // GET /books/v1?page=0&size=5&sort=title,asc
    @GetMapping
    public ResponseEntity<PageResponse<BookResponse>> getAllBooks(
            @PageableDefault(size = 10, sort = "id") Pageable pageable) {
        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    @PostMapping("/addBook")
    public ResponseEntity<BookResponse> addBook(@Valid @RequestBody BookRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookService.addBook(request));
    }

    @GetMapping("/getBook/{name}")
    public ResponseEntity<BookResponse> getBook(@PathVariable String name) {
        return ResponseEntity.ok(bookService.getBook(name));
    }

    @PutMapping("/updateBook")
    public ResponseEntity<BookResponse> updateBook(@Valid @RequestBody BookUpdateRequest request) {
        return ResponseEntity.ok(bookService.updateBook(request));
    }

    @DeleteMapping("/deleteBook/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Integer id) {
        bookService.deleteBook(id);
        return ResponseEntity.noContent().build();
    }
}
