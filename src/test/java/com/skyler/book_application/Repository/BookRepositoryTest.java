package com.skyler.book_application.Repository;

import com.skyler.book_application.Entity.Book;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** JPA slice test: starts only JPA and an embedded H2 database, so it checks the real queries and constraints. */
@DataJpaTest
class BookRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Test
    void findByTitleIgnoreCase_matchesRegardlessOfCase() {
        bookRepository.save(new Book(null, "Clean Code", "Robert C. Martin", "Programming"));

        assertThat(bookRepository.findByTitleIgnoreCase("clean code")).isPresent();
        assertThat(bookRepository.findByTitleIgnoreCase("Unknown title")).isEmpty();
    }

    @Test
    void existsByTitleIgnoreCase_worksForDifferentCase() {
        bookRepository.save(new Book(null, "Dune", "Frank Herbert", "Sci-Fi"));

        assertThat(bookRepository.existsByTitleIgnoreCase("DUNE")).isTrue();
        assertThat(bookRepository.existsByTitleIgnoreCase("Emma")).isFalse();
    }

    @Test
    void save_generatesIdentityId() {
        Book saved = bookRepository.save(new Book(null, "Emma", "Jane Austen", "Romance"));

        assertThat(saved.getId()).isNotNull();
    }

    @Test
    void databaseRejectsDuplicateTitle() {
        bookRepository.saveAndFlush(new Book(null, "Dune", "Frank Herbert", "Sci-Fi"));

        assertThatThrownBy(() -> bookRepository.saveAndFlush(new Book(null, "Dune", "Someone Else", "Other")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
