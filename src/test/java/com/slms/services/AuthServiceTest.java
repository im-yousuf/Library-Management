package com.slms.services;

import com.slms.database.DatabaseManager;
import com.slms.models.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {

    private AuthService authService;

    @BeforeEach
    void setUp() {
        DatabaseManager.setTestMode(true);
        DatabaseManager.initializeDatabase(); // Create tables and admin user in memory
        authService = new AuthService();
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
    void testAuthenticateSuccess() {
        User user = authService.authenticate("admin", "admin123");
        assertNotNull(user, "User should be authenticated successfully.");
        assertEquals("admin", user.getUsername());
        assertEquals("ADMIN", user.getRole());
        assertTrue(user.isActive());
    }

    @Test
    void testAuthenticateInvalidPassword() {
        User user = authService.authenticate("admin", "wrongpassword");
        assertNull(user, "User should not be authenticated with invalid password.");
    }

    @Test
    void testAuthenticateNonExistentUser() {
        User user = authService.authenticate("unknown", "password");
        assertNull(user, "Non-existent user should return null.");
    }

    @Test
    void testCreateUserSuccess() {
        boolean created = authService.createUser("librarian1", "libpass", "Librarian One", "LIBRARIAN");
        assertTrue(created, "User should be created successfully.");

        User user = authService.authenticate("librarian1", "libpass");
        assertNotNull(user);
        assertEquals("Librarian One", user.getFullName());
        assertEquals("LIBRARIAN", user.getRole());
    }

    @Test
    void testCreateUserDuplicateUsername() {
        authService.createUser("testuser", "pass", "Test User", "LIBRARIAN");
        boolean createdAgain = authService.createUser("testuser", "pass2", "Test User 2", "ADMIN");
        assertFalse(createdAgain, "Should not be able to create user with duplicate username.");
    }

    @Test
    void testAuthenticateDisabledUser() {
        authService.createUser("disableduser", "pass", "Disabled", "LIBRARIAN");
        
        // Disable user manually for testing
        try (Connection conn = DatabaseManager.getConnection();
             var ps = conn.prepareStatement("UPDATE users SET is_active = 0 WHERE username = 'disableduser'")) {
            ps.executeUpdate();
        } catch (Exception e) {
            fail("Failed to setup test data.");
        }

        User user = authService.authenticate("disableduser", "pass");
        assertNull(user, "Disabled user should not be able to authenticate.");
    }
}
