package com.slms.services;

import com.slms.database.DatabaseManager;
import com.slms.models.User;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AuthService {

    /**
     * Verifies credentials against the users table.
     * @return the authenticated User, or null if username/password is invalid or the account is disabled.
     */
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean active = rs.getInt("is_active") == 1;
                    String storedHash = rs.getString("password_hash");

                    if (active && BCrypt.checkpw(password, storedHash)) {
                        User user = new User(
                                rs.getInt("user_id"),
                                rs.getString("username"),
                                storedHash,
                                rs.getString("full_name"),
                                rs.getString("role"),
                                active
                        );
                        AuditService.log(user.getUserId(), "LOGIN", "User logged in");
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Login query failed", e);
        }
        return null;
    }

    public boolean createUser(String username, String rawPassword, String fullName, String role) {
        String sql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, BCrypt.hashpw(rawPassword, BCrypt.gensalt()));
            ps.setString(3, fullName);
            ps.setString(4, role);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            // Likely a UNIQUE constraint violation on username
            return false;
        }
    }
}
