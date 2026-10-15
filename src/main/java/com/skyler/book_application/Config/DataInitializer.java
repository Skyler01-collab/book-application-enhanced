package com.skyler.book_application.Config;

import com.skyler.book_application.Entity.Book;
import com.skyler.book_application.Repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Inserts mock books into the in-memory H2 database when the app starts,
 * so GET /books/v1 and pagination have something to show.
 * Turn it off with app.seed-data=false in application.properties.
 */
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BookRepository bookRepository;

    public DataInitializer(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public void run(String... args) {
        if (bookRepository.count() > 0) {
            return;
        }
        List<Book> books = List.of(
                new Book(null, "Clean Code", "Robert C. Martin", "Programming"),
                new Book(null, "Effective Java", "Joshua Bloch", "Programming"),
                new Book(null, "Head First Java", "Kathy Sierra and Bert Bates", "Programming"),
                new Book(null, "Spring in Action", "Craig Walls", "Programming"),
                new Book(null, "Design Patterns", "Erich Gamma, Richard Helm, Ralph Johnson, John Vlissides", "Programming"),
                new Book(null, "The Pragmatic Programmer", "Andrew Hunt and David Thomas", "Programming"),
                new Book(null, "1984", "George Orwell", "Dystopian"),
                new Book(null, "To Kill a Mockingbird", "Harper Lee", "Fiction"),
                new Book(null, "The Alchemist", "Paulo Coelho", "Fiction"),
                new Book(null, "Pride and Prejudice", "Jane Austen", "Romance"),
                new Book(null, "Sapiens", "Yuval Noah Harari", "History"),
                new Book(null, "Atomic Habits", "James Clear", "Self-help"));
        bookRepository.saveAll(books);
        log.info("Loaded {} mock books into H2", books.size());
    }
}
