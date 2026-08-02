package com.slms.services;

import com.slms.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuditService {

    public static void log(Integer userId, String action, String details) {
        String sql = "INSERT INTO audit_log (user_id, action, details) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) {
                ps.setInt(1, userId);
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            // Audit logging should never crash the app; just print for now.
            e.printStackTrace();
        }
    }
}
