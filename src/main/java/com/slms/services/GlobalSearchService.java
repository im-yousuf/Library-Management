package com.slms.services;

import com.slms.database.DatabaseManager;
import com.slms.models.SearchResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class GlobalSearchService {

    public List<SearchResult> search(String query) {
        List<SearchResult> results = new ArrayList<>();
        if (query == null || query.trim().isEmpty()) {
            return results;
        }

        String likeQuery = "%" + query.trim() + "%";

        try (Connection conn = DatabaseManager.getConnection()) {
            
            // 1. Search Books
            String bookSql = """
                SELECT b.book_id, b.title, b.author, r.rack_name, s.shelf_name, b.position
                FROM books b
                LEFT JOIN shelves s ON b.shelf_id = s.shelf_id
                LEFT JOIN racks r ON s.rack_id = r.rack_id
                WHERE b.title LIKE ? OR b.author LIKE ? OR b.isbn LIKE ? OR b.publisher LIKE ?
                LIMIT 5
            """;
            try (PreparedStatement pstmt = conn.prepareStatement(bookSql)) {
                for (int i = 1; i <= 4; i++) pstmt.setString(i, likeQuery);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        String loc = "";
                        if (rs.getString("rack_name") != null) {
                            loc = rs.getString("rack_name") + " \u2192 " + rs.getString("shelf_name");
                            if (rs.getString("position") != null && !rs.getString("position").isEmpty()) {
                                loc += " (" + rs.getString("position") + ")";
                            }
                        } else {
                            loc = "Unassigned";
                        }
                        results.add(new SearchResult(SearchResult.ResultType.BOOK, rs.getInt("book_id"), rs.getString("title") + " by " + rs.getString("author"), loc));
                    }
                }
            }

            // 2. Search Members
            String memberSql = "SELECT member_id, name, roll_number FROM members WHERE name LIKE ? OR roll_number LIKE ? LIMIT 5";
            try (PreparedStatement pstmt = conn.prepareStatement(memberSql)) {
                pstmt.setString(1, likeQuery);
                pstmt.setString(2, likeQuery);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(new SearchResult(SearchResult.ResultType.MEMBER, rs.getInt("member_id"), rs.getString("name"), "Roll: " + rs.getString("roll_number")));
                    }
                }
            }

            // 3. Search Racks/Shelves
            String shelfSql = """
                SELECT s.shelf_id, s.shelf_name, r.rack_name 
                FROM shelves s 
                JOIN racks r ON s.rack_id = r.rack_id 
                WHERE s.shelf_name LIKE ? OR r.rack_name LIKE ?
                LIMIT 5
            """;
            try (PreparedStatement pstmt = conn.prepareStatement(shelfSql)) {
                pstmt.setString(1, likeQuery);
                pstmt.setString(2, likeQuery);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        results.add(new SearchResult(SearchResult.ResultType.SHELF, rs.getInt("shelf_id"), rs.getString("rack_name") + " \u2192 " + rs.getString("shelf_name"), "Library Layout"));
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return results;
    }
}
