package com.slms.services;

import com.slms.database.DatabaseManager;
import com.slms.models.Book;
import com.slms.models.Category;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookService {

    public List<Book> getAllBooks() {
        return searchBooks("");
    }

    public List<Book> searchBooks(String query) {
        List<Book> books = new ArrayList<>();
        String sql = """
            SELECT b.*, c.name AS category_name 
            FROM books b
            LEFT JOIN categories c ON b.category_id = c.category_id
            WHERE b.title LIKE ? 
               OR b.author LIKE ? 
               OR b.isbn LIKE ? 
               OR b.publisher LIKE ? 
               OR c.name LIKE ?
            ORDER BY b.title ASC
        """;
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            String likeQuery = "%" + (query == null ? "" : query.trim()) + "%";
            pstmt.setString(1, likeQuery);
            pstmt.setString(2, likeQuery);
            pstmt.setString(3, likeQuery);
            pstmt.setString(4, likeQuery);
            pstmt.setString(5, likeQuery);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    books.add(extractBookFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return books;
    }

    public boolean addBook(Book book) {
        String sql = """
            INSERT INTO books (title, subtitle, author, isbn, publisher, edition, category_id, language, 
                               publication_year, purchase_date, price, quantity, available_copies, 
                               shelf_id, position, cover_path, description, status, barcode, qr_code)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            setBookParameters(pstmt, book);
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateBook(Book book) {
        String sql = """
            UPDATE books SET title=?, subtitle=?, author=?, isbn=?, publisher=?, edition=?, category_id=?, 
                             language=?, publication_year=?, purchase_date=?, price=?, quantity=?, 
                             available_copies=?, shelf_id=?, position=?, cover_path=?, description=?, 
                             status=?, barcode=?, qr_code=?
            WHERE book_id=?
        """;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             
            setBookParameters(pstmt, book);
            pstmt.setInt(21, book.getBookId());
            return pstmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteBook(int bookId) throws Exception {
        // First check for active transactions
        String checkSql = "SELECT COUNT(*) AS active_count FROM transactions WHERE book_id = ? AND status = 'ISSUED'";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, bookId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt("active_count") > 0) {
                    throw new Exception("Cannot delete book: There are active transactions (ISSUED) for this book.");
                }
            }
        }
        
        String sql = "DELETE FROM books WHERE book_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, bookId);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Category> getAllCategories() {
        List<Category> categories = new ArrayList<>();
        String sql = "SELECT * FROM categories ORDER BY name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
             
            while (rs.next()) {
                categories.add(new Category(rs.getInt("category_id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return categories;
    }

    public Category addCategory(String name) {
        String sql = "INSERT INTO categories (name) VALUES (?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, name.trim());
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return new Category(rs.getInt(1), name.trim());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Could happen on unique constraint violation
    }

    private void setBookParameters(PreparedStatement pstmt, Book book) throws SQLException {
        pstmt.setString(1, book.getTitle());
        pstmt.setString(2, book.getSubtitle());
        pstmt.setString(3, book.getAuthor());
        pstmt.setString(4, book.getIsbn());
        pstmt.setString(5, book.getPublisher());
        pstmt.setString(6, book.getEdition());
        if (book.getCategoryId() != null) pstmt.setInt(7, book.getCategoryId()); else pstmt.setNull(7, Types.INTEGER);
        pstmt.setString(8, book.getLanguage());
        if (book.getPublicationYear() != null) pstmt.setInt(9, book.getPublicationYear()); else pstmt.setNull(9, Types.INTEGER);
        pstmt.setString(10, book.getPurchaseDate());
        pstmt.setDouble(11, book.getPrice());
        pstmt.setInt(12, book.getQuantity());
        pstmt.setInt(13, book.getAvailableCopies());
        if (book.getShelfId() != null) pstmt.setInt(14, book.getShelfId()); else pstmt.setNull(14, Types.INTEGER);
        pstmt.setString(15, book.getPosition());
        pstmt.setString(16, book.getCoverPath());
        pstmt.setString(17, book.getDescription());
        pstmt.setString(18, book.getStatus() != null ? book.getStatus() : "AVAILABLE");
        pstmt.setString(19, book.getBarcode());
        pstmt.setString(20, book.getQrCode());
    }

    private Book extractBookFromResultSet(ResultSet rs) throws SQLException {
        Book book = new Book();
        book.setBookId(rs.getInt("book_id"));
        book.setTitle(rs.getString("title"));
        book.setSubtitle(rs.getString("subtitle"));
        book.setAuthor(rs.getString("author"));
        book.setIsbn(rs.getString("isbn"));
        book.setPublisher(rs.getString("publisher"));
        book.setEdition(rs.getString("edition"));
        
        int catId = rs.getInt("category_id");
        if (!rs.wasNull()) {
            book.setCategoryId(catId);
        }
        book.setCategoryName(rs.getString("category_name"));
        
        book.setLanguage(rs.getString("language"));
        
        int pubYear = rs.getInt("publication_year");
        if (!rs.wasNull()) {
            book.setPublicationYear(pubYear);
        }
        
        book.setPurchaseDate(rs.getString("purchase_date"));
        book.setPrice(rs.getDouble("price"));
        book.setQuantity(rs.getInt("quantity"));
        book.setAvailableCopies(rs.getInt("available_copies"));
        
        int shelfId = rs.getInt("shelf_id");
        if (!rs.wasNull()) {
            book.setShelfId(shelfId);
        }
        
        book.setPosition(rs.getString("position"));
        book.setCoverPath(rs.getString("cover_path"));
        book.setDescription(rs.getString("description"));
        book.setStatus(rs.getString("status"));
        book.setBarcode(rs.getString("barcode"));
        book.setQrCode(rs.getString("qr_code"));
        return book;
    }
}
