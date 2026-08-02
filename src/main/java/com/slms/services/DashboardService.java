package com.slms.services;

import com.slms.database.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DashboardService {

    public record DashboardStats(
            int totalBooks,
            int booksIssued,
            int availableBooks,
            int totalMembers,
            int overdueBooks,
            int lostBooks
    ) {}

    public DashboardStats getStats() {
        int totalBooks = countSum("SELECT COALESCE(SUM(quantity),0) AS n FROM books");
        int availableBooks = countSum("SELECT COALESCE(SUM(available_copies),0) AS n FROM books");
        int booksIssued = countRows("SELECT COUNT(*) AS n FROM transactions WHERE status = 'ISSUED' OR status = 'OVERDUE'");
        int totalMembers = countRows("SELECT COUNT(*) AS n FROM members WHERE status = 'ACTIVE'");
        int overdueBooks = countRows("SELECT COUNT(*) AS n FROM transactions WHERE status = 'ISSUED' AND due_date < date('now')");
        int lostBooks = countRows("SELECT COUNT(*) AS n FROM lost_damaged_books WHERE type = 'LOST'");

        return new DashboardStats(totalBooks, booksIssued, availableBooks, totalMembers, overdueBooks, lostBooks);
    }

    private int countRows(String sql) {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("n") : 0;
        } catch (SQLException e) {
            throw new RuntimeException("Dashboard stat query failed: " + sql, e);
        }
    }

    private int countSum(String sql) {
        return countRows(sql);
    }
}
