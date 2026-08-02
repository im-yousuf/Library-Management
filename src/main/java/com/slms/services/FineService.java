package com.slms.services;

import com.slms.database.DatabaseManager;
import com.slms.models.Fine;

import com.slms.utils.Session;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FineService {

    public List<Fine> getAllFines(String statusFilter) {
        List<Fine> fines = new ArrayList<>();
        String sql = "SELECT * FROM fines";
        if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL")) {
            sql += " WHERE status = ?";
        }
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL")) {
                pstmt.setString(1, statusFilter);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    fines.add(mapFine(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fines;
    }

    public List<Fine> getFineHistoryForMember(int memberId) {
        List<Fine> fines = new ArrayList<>();
        String sql = """
            SELECT f.* FROM fines f
            JOIN transactions t ON f.transaction_id = t.transaction_id
            WHERE t.member_id = ?
            """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    fines.add(mapFine(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fines;
    }

    public boolean markFinePaid(int fineId) {
        String sql = "UPDATE fines SET status = 'PAID', paid_date = datetime('now') WHERE fine_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fineId);
            boolean updated = pstmt.executeUpdate() > 0;
            if (updated) {
                AuditService.log(Session.getCurrentUser().getUserId(), "FINE_PAID", "Fine ID " + fineId);
            }
            return updated;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean waiveFine(int fineId, String reason) {
        String sql = "UPDATE fines SET status = 'PAID', paid_date = datetime('now') WHERE fine_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, fineId);
            boolean updated = pstmt.executeUpdate() > 0;
            if (updated) {
                AuditService.log(Session.getCurrentUser().getUserId(), "FINE_WAIVED", "Fine ID " + fineId + ", reason: " + reason);
            }
            return updated;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateFineSettings(double finePerDay, int graceDays) {
        String sql = "UPDATE settings SET fine_per_day = ?, grace_days = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, finePerDay);
            pstmt.setInt(2, graceDays);
            boolean updated = pstmt.executeUpdate() > 0;
            if (updated) {
                AuditService.log(Session.getCurrentUser().getUserId(), "FINE_SETTINGS_UPDATE", "finePerDay=" + finePerDay + ", graceDays=" + graceDays);
            }
            return updated;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private Fine mapFine(ResultSet rs) throws SQLException {
        Fine f = new Fine();
        f.setFineId(rs.getInt("fine_id"));
        f.setTransactionId(rs.getInt("transaction_id"));
        f.setAmount(rs.getDouble("amount"));
        f.setStatus(rs.getString("status"));
        f.setPaidDate(rs.getString("paid_date"));
        return f;
    }
}
