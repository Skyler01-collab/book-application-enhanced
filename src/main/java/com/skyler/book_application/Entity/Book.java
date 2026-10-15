package com.skyler.book_application.Entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // unique = true adds a database-level UNIQUE constraint (the service also checks it first
    // so we can return a friendly 409 instead of a raw database error)
    @Column(nullable = false, unique = true, length = 150)
    private String title;

    @Column(nullable = false, length = 100)
    private String author;

    @Column(length = 50)
    private String genre;
}
