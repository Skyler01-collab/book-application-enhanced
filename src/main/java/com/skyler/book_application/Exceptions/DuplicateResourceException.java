package com.skyler.book_application.Exceptions;

/** Thrown when a book with the same title already exists. Handled as HTTP 409. */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
