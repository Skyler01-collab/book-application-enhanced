package com.skyler.book_application.Dto;

import com.skyler.book_application.Entity.Book;

/** What the API returns. Keeps the JPA entity internal to the application. */
public record BookResponse(Integer id, String title, String author, String genre) {

    public static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getTitle(), book.getAuthor(), book.getGenre());
    }
}
