package com.skyler.book_application.Repository;

import com.skyler.book_application.Entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Integer> {

    // Derived queries: Spring Data builds the SQL from the method name.
    // Optional replaces "null if not found"; IgnoreCase makes "clean code" match "Clean Code".
    Optional<Book> findByTitleIgnoreCase(String title);

    boolean existsByTitleIgnoreCase(String title);
}
