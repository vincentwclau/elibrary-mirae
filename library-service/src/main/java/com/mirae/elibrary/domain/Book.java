package com.mirae.elibrary.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String author;

    @Column(nullable = false, unique = true)
    private String isbn;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String category;

    private Integer publishedYear;

    @Column(nullable = false)
    private int totalCopies;

    @Column(nullable = false)
    private int availableCopies;

    /**
     * Optimistic lock guard so two concurrent borrow requests cannot both
     * decrement availableCopies from the same starting value.
     */
    @Version
    private long version;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Book() {
        // for JPA
    }

    public Book(String title, String author, String isbn, String description,
                String category, Integer publishedYear, int totalCopies) {
        this.title = title;
        this.author = author;
        this.isbn = isbn;
        this.description = description;
        this.category = category;
        this.publishedYear = publishedYear;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    @jakarta.persistence.PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    /**
     * @return true if at least one copy is available to borrow.
     */
    public boolean isAvailable() {
        return availableCopies > 0;
    }

    public void decrementAvailable() {
        if (availableCopies <= 0) {
            throw new IllegalStateException("No available copies to borrow");
        }
        availableCopies--;
    }

    public void incrementAvailable() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("Available copies cannot exceed total copies");
        }
        availableCopies++;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public Integer getPublishedYear() {
        return publishedYear;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
