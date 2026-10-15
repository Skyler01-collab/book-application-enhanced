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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final List<String> SORTABLE_FIELDS = List.of("id", "title", "author", "genre");

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<BookResponse> getAllBooks(Pageable pageable) {
        validateSort(pageable.getSort());
        // Never let a client ask for thousands of rows at once
        Pageable safePageable = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), MAX_PAGE_SIZE),
                pageable.getSort());
        return PageResponse.from(bookRepository.findAll(safePageable).map(BookResponse::from));
    }

    @Transactional(readOnly = true)
    public BookResponse getBook(String title) {
        return bookRepository.findByTitleIgnoreCase(title.trim())
                .map(BookResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Book with title '" + title + "' not found"));
    }

    @Transactional
    public BookResponse addBook(BookRequest request) {
        String title = request.title().trim();
        if (bookRepository.existsByTitleIgnoreCase(title)) {
            throw new DuplicateResourceException("A book titled '" + title + "' already exists");
        }
        Book saved = bookRepository.save(new Book(null, title, request.author().trim(), blankToNull(request.genre())));
        return BookResponse.from(saved);
    }

    @Transactional
    public BookResponse updateBook(BookUpdateRequest request) {
        Book book = bookRepository.findById(request.id())
                .orElseThrow(() -> new ResourceNotFoundException("Book with id " + request.id() + " not found"));

        String title = request.title().trim();
        // The title may stay the same, but it must not belong to a different book
        bookRepository.findByTitleIgnoreCase(title)
                .filter(other -> !other.getId().equals(book.getId()))
                .ifPresent(other -> {
                    throw new DuplicateResourceException("A book titled '" + title + "' already exists");
                });

        book.setTitle(title);
        book.setAuthor(request.author().trim());
        book.setGenre(blankToNull(request.genre()));
        return BookResponse.from(bookRepository.save(book));
    }

    @Transactional
    public void deleteBook(Integer id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Book with id " + id + " not found");
        }
        bookRepository.deleteById(id);
    }

    private void validateSort(Sort sort) {
        sort.forEach(order -> {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Cannot sort by '" + order.getProperty()
                        + "'. Allowed fields: " + String.join(", ", SORTABLE_FIELDS));
            }
        });
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
