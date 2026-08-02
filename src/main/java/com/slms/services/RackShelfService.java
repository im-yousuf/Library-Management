package com.slms.services;

import com.slms.database.DatabaseManager;
import com.slms.models.Book;
import com.slms.models.Rack;
import com.slms.models.Shelf;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RackShelfService {

    public Rack addRack(String name) throws Exception {
        String sql = "INSERT INTO racks (rack_name) VALUES (?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name.trim());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Rack(rs.getInt(1), name.trim());
                }
            }
        }
        throw new Exception("Failed to add rack.");
    }

    public void renameRack(int rackId, String newName) throws Exception {
        String sql = "UPDATE racks SET rack_name = ? WHERE rack_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName.trim());
            pstmt.setInt(2, rackId);
            if (pstmt.executeUpdate() == 0) {
                throw new Exception("Rack not found.");
            }
        }
    }

    public void deleteRack(int rackId) throws Exception {
        // Check if any shelf under it has books
        String checkSql = "SELECT COUNT(*) AS cnt FROM books b JOIN shelves s ON b.shelf_id = s.shelf_id WHERE s.rack_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setInt(1, rackId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt("cnt") > 0) {
                    throw new Exception("Cannot delete rack: Some shelves under this rack still have books assigned.");
                }
            }
        }
        
        // Due to ON DELETE CASCADE on shelves(rack_id), empty shelves will be auto-deleted.
        String sql = "DELETE FROM racks WHERE rack_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rackId);
            pstmt.executeUpdate();
        }
    }

    public Shelf addShelf(int rackId, String shelfName) throws Exception {
        String sql = "INSERT INTO shelves (rack_id, shelf_name) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, rackId);
            pstmt.setString(2, shelfName.trim());
            pstmt.executeUpdate();
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return new Shelf(rs.getInt(1), rackId, shelfName.trim());
                }
            }
        }
        throw new Exception("Failed to add shelf.");
    }

    public void renameShelf(int shelfId, String newName) throws Exception {
        String sql = "UPDATE shelves SET shelf_name = ? WHERE shelf_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newName.trim());
            pstmt.setInt(2, shelfId);
            if (pstmt.executeUpdate() == 0) {
                throw new Exception("Shelf not found.");
            }
        }
    }

    public void deleteShelf(int shelfId) throws Exception {
        String checkSql = "SELECT COUNT(*) AS cnt FROM books WHERE shelf_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(checkSql)) {
            pstmt.setInt(1, shelfId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next() && rs.getInt("cnt") > 0) {
                    throw new Exception("Cannot delete shelf: Books are currently assigned to this shelf.");
                }
            }
        }
        
        String sql = "DELETE FROM shelves WHERE shelf_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, shelfId);
            pstmt.executeUpdate();
        }
    }

    public List<Rack> getAllRacksWithShelves() {
        Map<Integer, Rack> rackMap = new LinkedHashMap<>();
        String sql = """
            SELECT r.rack_id, r.rack_name, s.shelf_id, s.shelf_name 
            FROM racks r 
            LEFT JOIN shelves s ON r.rack_id = s.rack_id 
            ORDER BY r.rack_name ASC, s.shelf_name ASC
        """;
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
             
            while (rs.next()) {
                int rackId = rs.getInt("rack_id");
                Rack rack = rackMap.computeIfAbsent(rackId, id -> {
                    try {
                        return new Rack(id, rs.getString("rack_name"));
                    } catch (Exception e) { return null; }
                });
                
                int shelfId = rs.getInt("shelf_id");
                if (!rs.wasNull()) {
                    Shelf shelf = new Shelf(shelfId, rackId, rs.getString("shelf_name"));
                    shelf.setParentRack(rack);
                    rack.getShelves().add(shelf);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new ArrayList<>(rackMap.values());
    }

    public List<Book> getBooksOnShelf(int shelfId) {
        List<Book> books = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE shelf_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, shelfId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Book b = new Book();
                    b.setBookId(rs.getInt("book_id"));
                    b.setTitle(rs.getString("title"));
                    b.setAuthor(rs.getString("author"));
                    b.setPosition(rs.getString("position"));
                    books.add(b);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return books;
    }

    public void assignBookToShelf(int bookId, Integer shelfId, String position) throws Exception {
        String sql = "UPDATE books SET shelf_id = ?, position = ? WHERE book_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (shelfId == null) pstmt.setNull(1, java.sql.Types.INTEGER);
            else pstmt.setInt(1, shelfId);
            
            pstmt.setString(2, position);
            pstmt.setInt(3, bookId);
            pstmt.executeUpdate();
        }
    }

    public String getShelfLocationLabel(int bookId) {
        String sql = """
            SELECT r.rack_name, s.shelf_name, b.position 
            FROM books b
            JOIN shelves s ON b.shelf_id = s.shelf_id
            JOIN racks r ON s.rack_id = r.rack_id
            WHERE b.book_id = ?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String rack = rs.getString("rack_name");
                    String shelf = rs.getString("shelf_name");
                    String pos = rs.getString("position");
                    
                    String label = rack + " \u2192 " + shelf;
                    if (pos != null && !pos.isEmpty()) {
                        label += " (" + pos + ")";
                    }
                    return label;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Unassigned";
    }
}
