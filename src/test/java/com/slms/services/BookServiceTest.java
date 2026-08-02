package com.slms.services;

import com.slms.database.DatabaseManager;
import com.slms.models.Book;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BookServiceTest {

    private BookService bookService;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.setTestMode(true);
        DatabaseManager.initializeDatabase();
        bookService = new BookService();

        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO categories (category_id, name) VALUES (1, 'Fiction')")) {
                ps.executeUpdate();
            }
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
        DatabaseManager.setTestMode(false);
    }

    @Test
    void testAddAndSearchBook() {
        Book b = new Book();
        b.setTitle("The Great Gatsby");
        b.setAuthor("F. Scott Fitzgerald");
        b.setIsbn("978-0743273565");
        b.setPublisher("Scribner");
        b.setCategoryId(1);
        b.setQuantity(5);
        b.setAvailableCopies(5);
        
        boolean added = bookService.addBook(b);
        assertTrue(added, "Book should be added successfully");

        List<Book> books = bookService.searchBooks("Gatsby");
        assertEquals(1, books.size());
        assertEquals("The Great Gatsby", books.get(0).getTitle());
        
        List<Book> noMatch = bookService.searchBooks("NonExistent");
        assertTrue(noMatch.isEmpty());
    }

    @Test
    void testUpdateBook() {
        Book b = new Book();
        b.setTitle("Original Title");
        b.setCategoryId(1);
        bookService.addBook(b);
        
        List<Book> books = bookService.searchBooks("Original");
        Book savedBook = books.get(0);
        
        savedBook.setTitle("Updated Title");
        boolean updated = bookService.updateBook(savedBook);
        assertTrue(updated);
        
        List<Book> results = bookService.searchBooks("Updated");
        assertEquals(1, results.size());
        assertEquals("Updated Title", results.get(0).getTitle());
    }

    @Test
    void testDeleteBookSuccess() throws Exception {
        Book b = new Book();
        b.setTitle("To Be Deleted");
        b.setCategoryId(1);
        bookService.addBook(b);
        
        List<Book> books = bookService.searchBooks("To Be Deleted");
        Book savedBook = books.get(0);
        
        boolean deleted = bookService.deleteBook(savedBook.getBookId());
        assertTrue(deleted);
        
        assertTrue(bookService.searchBooks("To Be Deleted").isEmpty());
    }

    @Test
    void testDeleteBookFailsWhenInUse() throws Exception {
        Book b = new Book();
        b.setTitle("In Use Book");
        b.setCategoryId(1);
        bookService.addBook(b);
        
        Book savedBook = bookService.searchBooks("In Use Book").get(0);
        
        // Simulate a transaction for this book
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO members (member_id, name, status) VALUES (1, 'Test', 'ACTIVE');")) {
            ps.executeUpdate();
            try (PreparedStatement ps2 = conn.prepareStatement("INSERT INTO transactions (book_id, member_id, status) VALUES (?, 1, 'ISSUED')")) {
                ps2.setInt(1, savedBook.getBookId());
                ps2.executeUpdate();
            }
        }
        
        boolean deleted = bookService.deleteBook(savedBook.getBookId());
        assertFalse(deleted, "Book should not be deleted if it is currently issued or in transactions.");
    }
}
