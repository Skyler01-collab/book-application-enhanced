package com.skyler.book_application.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Body for updating a book: the same fields as BookRequest plus the id of the book to change. */
public record BookUpdateRequest(
        @NotNull(message = "Id is required")
        Integer id,

        @NotBlank(message = "Title is required")
        @Size(max = 150, message = "Title must be at most 150 characters")
        String title,

        @NotBlank(message = "Author is required")
        @Size(max = 100, message = "Author must be at most 100 characters")
        String author,

        @Size(max = 50, message = "Genre must be at most 50 characters")
        String genre) {
}
