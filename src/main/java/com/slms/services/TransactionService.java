package com.slms.services;

import com.slms.database.DatabaseManager;
import com.slms.models.Transaction;
import com.slms.models.Fine;

import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class TransactionService {

    public Transaction issueBook(int bookId, int memberId, int issuedByUserId) throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false); // start transaction
            try {
                // 1. Validate Member
                String memberSql = "SELECT status FROM members WHERE member_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(memberSql)) {
                    pstmt.setInt(1, memberId);
                    ResultSet rs = pstmt.executeQuery();
                    if (!rs.next()) throw new Exception("Member not found.");
                    if (!"ACTIVE".equals(rs.getString("status"))) throw new Exception("Member is not ACTIVE.");
                }

                // 2. Validate outstanding fines
                MemberService memberService = new MemberService();
                double fine = memberService.getOutstandingFine(memberId);
                if (fine > 50.0) { // $50 hardcoded threshold, could be in settings
                    throw new Exception("Member has unpaid fines exceeding limit: $" + fine);
                }

                // 3. Validate max books
                String activeSql = "SELECT COUNT(*) AS active_issues FROM transactions WHERE member_id = ? AND status = 'ISSUED'";
                try (PreparedStatement pstmt = conn.prepareStatement(activeSql)) {
                    pstmt.setInt(1, memberId);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next() && rs.getInt("active_issues") >= 5) { // default 5 limit
                        throw new Exception("Member has reached maximum allowed issued books (5).");
                    }
                }

                // 4. Validate book availability
                String bookSql = "SELECT available_copies FROM books WHERE book_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(bookSql)) {
                    pstmt.setInt(1, bookId);
                    ResultSet rs = pstmt.executeQuery();
                    if (!rs.next()) throw new Exception("Book not found.");
                    if (rs.getInt("available_copies") <= 0) throw new Exception("Book is currently out of stock.");
                }

                // 5. Decrement available copies
                String updateBookSql = "UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(updateBookSql)) {
                    pstmt.setInt(1, bookId);
                    pstmt.executeUpdate();
                }

                // 6. Get settings for loan period
                int loanPeriodDays = getSettingInt(conn, "loan_period_days", 14);

                // 7. Insert transaction
                String issueDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
                String dueDate = LocalDate.now().plusDays(loanPeriodDays).format(DateTimeFormatter.ISO_LOCAL_DATE);

                String insertTxSql = "INSERT INTO transactions (book_id, member_id, issue_date, due_date, status, issued_by) VALUES (?, ?, ?, ?, 'ISSUED', ?)";
                int txId = -1;
                try (PreparedStatement pstmt = conn.prepareStatement(insertTxSql, Statement.RETURN_GENERATED_KEYS)) {
                    pstmt.setInt(1, bookId);
                    pstmt.setInt(2, memberId);
                    pstmt.setString(3, issueDate);
                    pstmt.setString(4, dueDate);
                    pstmt.setInt(5, issuedByUserId);
                    pstmt.executeUpdate();

                    ResultSet rs = pstmt.getGeneratedKeys();
                    if (rs.next()) {
                        txId = rs.getInt(1);
                    }
                }

                conn.commit();
                
                Transaction tx = new Transaction();
                tx.setTransactionId(txId);
                tx.setBookId(bookId);
                tx.setMemberId(memberId);
                tx.setIssueDate(issueDate);
                tx.setDueDate(dueDate);
                tx.setStatus("ISSUED");
                tx.setIssuedBy(issuedByUserId);
                return tx;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Fine returnBook(int transactionId) throws Exception {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Get transaction info
                int bookId = -1;
                String dueDateStr = null;
                try (PreparedStatement pstmt = conn.prepareStatement("SELECT book_id, due_date FROM transactions WHERE transaction_id = ? AND status = 'ISSUED'")) {
                    pstmt.setInt(1, transactionId);
                    ResultSet rs = pstmt.executeQuery();
                    if (!rs.next()) throw new Exception("Active transaction not found.");
                    bookId = rs.getInt("book_id");
                    dueDateStr = rs.getString("due_date");
                }

                // 2. Update transaction
                String returnDateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE transactions SET status = 'RETURNED', return_date = ? WHERE transaction_id = ?")) {
                    pstmt.setString(1, returnDateStr);
                    pstmt.setInt(2, transactionId);
                    pstmt.executeUpdate();
                }

                // 3. Increment book availability
                try (PreparedStatement pstmt = conn.prepareStatement("UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ?")) {
                    pstmt.setInt(1, bookId);
                    pstmt.executeUpdate();
                }

                // 4. Calculate fines
                LocalDate dueDate = LocalDate.parse(dueDateStr);
                LocalDate returnDate = LocalDate.parse(returnDateStr);
                int graceDays = getSettingInt(conn, "grace_days", 2);
                double finePerDay = getSettingDouble(conn, "fine_per_day", 5.0);

                long overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
                Fine resultFine = null;

                if (overdueDays > graceDays) {
                    double fineAmount = (overdueDays - graceDays) * finePerDay;
                    if (fineAmount > 0) {
                        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO fines (transaction_id, amount, status) VALUES (?, ?, 'PENDING')", Statement.RETURN_GENERATED_KEYS)) {
                            pstmt.setInt(1, transactionId);
                            pstmt.setDouble(2, fineAmount);
                            pstmt.executeUpdate();
                            
                            ResultSet rs = pstmt.getGeneratedKeys();
                            if (rs.next()) {
                                resultFine = new Fine(rs.getInt(1), transactionId, fineAmount, "PENDING", null);
                            }
                        }
                    }
                }

                conn.commit();
                return resultFine;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void payFine(int fineId) throws Exception {
        String sql = "UPDATE fines SET status = 'PAID', paid_date = ? WHERE fine_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            pstmt.setInt(2, fineId);
            if (pstmt.executeUpdate() == 0) {
                throw new Exception("Fine not found.");
            }
        }
    }

    private int getSettingInt(Connection conn, String key, int def) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Integer.parseInt(rs.getString("setting_value"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }

    private double getSettingDouble(Connection conn, String key, double def) {
        String sql = "SELECT setting_value FROM settings WHERE setting_key = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Double.parseDouble(rs.getString("setting_value"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return def;
    }
}
