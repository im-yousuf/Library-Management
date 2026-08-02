package com.slms.services;

import com.slms.database.DatabaseManager;
import com.slms.models.Member;
import com.slms.models.Transaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberService {

    public List<Member> getAllMembers() {
        return searchMembers("");
    }

    public List<Member> searchMembers(String query) {
        List<Member> members = new ArrayList<>();
        String sql = """
            SELECT * FROM members 
            WHERE name LIKE ? 
               OR roll_number LIKE ? 
               OR department LIKE ? 
               OR course LIKE ? 
               OR phone LIKE ? 
               OR email LIKE ?
            ORDER BY name ASC
        """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            String likeQuery = "%" + (query == null ? "" : query.trim()) + "%";
            for (int i = 1; i <= 6; i++) {
                pstmt.setString(i, likeQuery);
            }
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    members.add(extractMemberFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    public boolean addMember(Member member) {
        String sql = """
            INSERT INTO members (photo_path, name, department, course, semester, roll_number, 
                                 phone, email, address, joining_date, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            setMemberParameters(pstmt, member);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateMember(Member member) {
        String sql = """
            UPDATE members SET photo_path=?, name=?, department=?, course=?, semester=?, roll_number=?, 
                               phone=?, email=?, address=?, joining_date=?, status=?
            WHERE member_id=?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            setMemberParameters(pstmt, member);
            pstmt.setInt(12, member.getMemberId());
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteMember(int memberId) throws Exception {
        // Check for active transactions
        String activeTxSql = "SELECT COUNT(*) AS cnt FROM transactions WHERE member_id = ? AND status = 'ISSUED'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(activeTxSql)) {
            stmt.setInt(1, memberId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getInt("cnt") > 0) {
                throw new Exception("Cannot delete member: There are books currently issued to this member.");
            }
        }

        // Check for unpaid fines
        double fines = getOutstandingFine(memberId);
        if (fines > 0) {
            throw new Exception("Cannot delete member: There are outstanding unpaid fines ($" + fines + ").");
        }

        String sql = "DELETE FROM members WHERE member_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Transaction> getMemberBorrowHistory(int memberId) {
        return getTransactionsForMember(memberId, false);
    }

    public List<Transaction> getCurrentBooks(int memberId) {
        return getTransactionsForMember(memberId, true);
    }

    private List<Transaction> getTransactionsForMember(int memberId, boolean onlyCurrent) {
        List<Transaction> transactions = new ArrayList<>();
        String sql = """
            SELECT t.*, b.title AS book_title, 
                   COALESCE((SELECT SUM(amount) FROM fines f WHERE f.transaction_id = t.transaction_id AND f.status = 'PENDING'), 0) AS fine_amount
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            WHERE t.member_id = ?
        """ + (onlyCurrent ? " AND t.status = 'ISSUED'" : "") + " ORDER BY t.issue_date DESC";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Transaction tx = new Transaction();
                    tx.setTransactionId(rs.getInt("transaction_id"));
                    tx.setBookId(rs.getInt("book_id"));
                    tx.setBookTitle(rs.getString("book_title"));
                    tx.setMemberId(rs.getInt("member_id"));
                    tx.setIssueDate(rs.getString("issue_date"));
                    tx.setDueDate(rs.getString("due_date"));
                    tx.setReturnDate(rs.getString("return_date"));
                    tx.setStatus(rs.getString("status"));
                    tx.setFineAmount(rs.getDouble("fine_amount"));
                    transactions.add(tx);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return transactions;
    }

    public double getOutstandingFine(int memberId) {
        String sql = """
            SELECT SUM(f.amount) AS total_fine
            FROM fines f
            JOIN transactions t ON f.transaction_id = t.transaction_id
            WHERE t.member_id = ? AND f.status = 'PENDING'
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, memberId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total_fine");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private void setMemberParameters(PreparedStatement pstmt, Member member) throws SQLException {
        pstmt.setString(1, member.getPhotoPath());
        pstmt.setString(2, member.getName());
        pstmt.setString(3, member.getDepartment());
        pstmt.setString(4, member.getCourse());
        pstmt.setString(5, member.getSemester());
        pstmt.setString(6, member.getRollNumber());
        pstmt.setString(7, member.getPhone());
        pstmt.setString(8, member.getEmail());
        pstmt.setString(9, member.getAddress());
        pstmt.setString(10, member.getJoiningDate() != null ? member.getJoiningDate() : new java.sql.Date(System.currentTimeMillis()).toString());
        pstmt.setString(11, member.getStatus() != null ? member.getStatus() : "ACTIVE");
    }

    private Member extractMemberFromResultSet(ResultSet rs) throws SQLException {
        Member m = new Member();
        m.setMemberId(rs.getInt("member_id"));
        m.setPhotoPath(rs.getString("photo_path"));
        m.setName(rs.getString("name"));
        m.setDepartment(rs.getString("department"));
        m.setCourse(rs.getString("course"));
        m.setSemester(rs.getString("semester"));
        m.setRollNumber(rs.getString("roll_number"));
        m.setPhone(rs.getString("phone"));
        m.setEmail(rs.getString("email"));
        m.setAddress(rs.getString("address"));
        m.setJoiningDate(rs.getString("joining_date"));
        m.setStatus(rs.getString("status"));
        return m;
    }
}
