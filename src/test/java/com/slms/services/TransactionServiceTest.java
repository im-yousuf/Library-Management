package com.slms.services;

import com.slms.database.DatabaseManager;
import com.slms.models.Fine;
import com.slms.models.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private TransactionService transactionService;

    @BeforeEach
    void setUp() throws Exception {
        DatabaseManager.setTestMode(true);
        DatabaseManager.initializeDatabase(); // Create tables
        transactionService = new TransactionService();

        // Setup base data
        try (Connection conn = DatabaseManager.getConnection()) {
            // Add category
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO categories (category_id, name) VALUES (1, 'Test Category')")) {
                ps.executeUpdate();
            }
            // Add book
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO books (book_id, title, author, isbn, publisher, category_id, total_copies, available_copies) VALUES (1, 'Test Book', 'Author', '12345', 'Pub', 1, 2, 2)")) {
                ps.executeUpdate();
            }
            // Add member
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO members (member_id, full_name, email, phone, status) VALUES (1, 'Test Member', 'test@test.com', '1234567890', 'ACTIVE')")) {
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
    void testIssueBookSuccess() throws Exception {
        Transaction tx = transactionService.issueBook(1, 1, 1);
        assertNotNull(tx, "Transaction should be created");
        assertEquals("ISSUED", tx.getStatus());

        // Verify available copies decreased
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = 1")) {
            var rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(1, rs.getInt("available_copies"));
        }
    }

    @Test
    void testIssueBookFailsWhenNoCopies() throws Exception {
        // Issue both copies
        transactionService.issueBook(1, 1, 1);
        
        // Let's create another member to issue the second copy
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO members (member_id, full_name, status) VALUES (2, 'Member 2', 'ACTIVE')")) {
            ps.executeUpdate();
        }
        transactionService.issueBook(1, 2, 1);

        // Attempting to issue the third time should fail
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("INSERT INTO members (member_id, full_name, status) VALUES (3, 'Member 3', 'ACTIVE')")) {
            ps.executeUpdate();
        }

        Exception exception = assertThrows(Exception.class, () -> {
            transactionService.issueBook(1, 3, 1);
        });
        assertTrue(exception.getMessage().contains("out of stock") || exception.getMessage().contains("not found"));
    }

    @Test
    void testReturnBookOnTime() throws Exception {
        Transaction tx = transactionService.issueBook(1, 1, 1);
        Fine fine = transactionService.returnBook(tx.getTransactionId());
        
        assertNull(fine, "No fine should be generated for on-time return");

        // Verify available copies increased back to 2
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT available_copies FROM books WHERE book_id = 1")) {
            var rs = ps.executeQuery();
            assertTrue(rs.next());
            assertEquals(2, rs.getInt("available_copies"));
        }
    }

    @Test
    void testReturnBookLate() throws Exception {
        Transaction tx = transactionService.issueBook(1, 1, 1);
        
        // Manually update the due_date to a past date to simulate late return
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE transactions SET due_date = date('now', '-5 days') WHERE transaction_id = ?")) {
            ps.setInt(1, tx.getTransactionId());
            ps.executeUpdate();
        }

        Fine fine = transactionService.returnBook(tx.getTransactionId());
        
        assertNotNull(fine, "A fine should be generated for late return");
        assertEquals("PENDING", fine.getStatus());
        assertTrue(fine.getAmount() > 0);
    }
}
