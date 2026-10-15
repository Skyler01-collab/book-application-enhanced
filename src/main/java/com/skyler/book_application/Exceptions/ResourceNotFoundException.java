package com.skyler.book_application.Exceptions;

/** Thrown when a book does not exist. Handled as HTTP 404. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
