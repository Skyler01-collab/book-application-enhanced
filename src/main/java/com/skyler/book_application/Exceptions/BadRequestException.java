package com.skyler.book_application.Exceptions;

/** Thrown for invalid request parameters (for example sorting by an unknown field). Handled as HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
