package com.mirae.elibrary.config;

import com.mirae.elibrary.domain.Book;
import com.mirae.elibrary.domain.Role;
import com.mirae.elibrary.domain.User;
import com.mirae.elibrary.repository.BookRepository;
import com.mirae.elibrary.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds a small set of demo users and books on startup when the tables are
 * empty, so the app is usable immediately. Idempotent: does nothing if data
 * already exists. Demo credentials are documented in the README. Disabled under
 * the {@code test} profile so tests control their own fixtures.
 */
@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final String DEMO_PASSWORD = "password123";

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      BookRepository bookRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedBooks();
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        userRepository.save(new User("member@demo.io",
                passwordEncoder.encode(DEMO_PASSWORD), "Demo Member", Role.MEMBER));
        userRepository.save(new User("admin@demo.io",
                passwordEncoder.encode(DEMO_PASSWORD), "Demo Admin", Role.ADMIN));
        log.info("Seeded {} demo users (password: '{}')", 2, DEMO_PASSWORD);
    }

    private void seedBooks() {
        if (bookRepository.count() > 0) {
            return;
        }
        List<Book> books = List.of(
                new Book("Clean Code", "Robert C. Martin", "9780132350884",
                        "A handbook of agile software craftsmanship.", "Software", 2008, 3),
                new Book("The Pragmatic Programmer", "Andrew Hunt, David Thomas", "9780201616224",
                        "Your journey to mastery, from journeyman to master.", "Software", 1999, 2),
                new Book("Effective Java", "Joshua Bloch", "9780134685991",
                        "Best practices for the Java platform.", "Software", 2018, 4),
                new Book("Designing Data-Intensive Applications", "Martin Kleppmann", "9781449373320",
                        "The big ideas behind reliable, scalable, and maintainable systems.", "Software", 2017, 2),
                new Book("Domain-Driven Design", "Eric Evans", "9780321125217",
                        "Tackling complexity in the heart of software.", "Software", 2003, 1),
                new Book("Sapiens", "Yuval Noah Harari", "9780062316097",
                        "A brief history of humankind.", "History", 2011, 5),
                new Book("Thinking, Fast and Slow", "Daniel Kahneman", "9780374533557",
                        "How the two systems that drive the way we think shape our judgments.", "Psychology", 2011, 3),
                new Book("The Selfish Gene", "Richard Dawkins", "9780198788607",
                        "A gene-centred view of evolution.", "Science", 1976, 2),
                new Book("A Brief History of Time", "Stephen Hawking", "9780553380163",
                        "From the Big Bang to black holes.", "Science", 1988, 4),
                new Book("Dune", "Frank Herbert", "9780441013593",
                        "A science-fiction epic set on the desert planet Arrakis.", "Fiction", 1965, 3));
        bookRepository.saveAll(books);
        log.info("Seeded {} books", books.size());
    }
}
