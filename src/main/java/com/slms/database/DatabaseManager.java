package com.slms.database;

import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 * Central database access point for SLMS.
 * Uses a single shared SQLite connection (fine for a single-user offline desktop app).
 * The DB file lives next to the app so it works fully offline with no server.
 */
public class DatabaseManager {

    private static final String DB_FOLDER = "data";
    private static final String DB_FILE = DB_FOLDER + File.separator + "slms.db";
    private static final String URL = "jdbc:sqlite:" + DB_FILE;
    private static final String TEST_URL = "jdbc:sqlite::memory:";
    private static boolean testMode = false;

    private static Connection connection;

    private DatabaseManager() {
    }

    public static void setTestMode(boolean isTestMode) {
        testMode = isTestMode;
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            connection = null;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                if (!testMode) {
                    new File(DB_FOLDER).mkdirs();
                }
                Class.forName("org.sqlite.JDBC");
                String activeUrl = testMode ? TEST_URL : URL;
                connection = DriverManager.getConnection(activeUrl);
                // Enforce FK constraints (SQLite has them off by default)
                try (Statement s = connection.createStatement()) {
                    s.execute("PRAGMA foreign_keys = ON;");
                }
            }
        } catch (ClassNotFoundException | SQLException e) {
            throw new RuntimeException("Failed to connect to SQLite database", e);
        }
        return connection;
    }

    /**
     * Creates every table the app needs if it doesn't already exist,
     * and seeds a default admin account on first run.
     */
    public static void initializeDatabase() {
        Connection conn = getConnection();
        try (Statement stmt = conn.createStatement()) {

            // ---------- USERS ----------
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    full_name TEXT,
                    role TEXT NOT NULL CHECK(role IN ('ADMIN','LIBRARIAN')),
                    is_active INTEGER NOT NULL DEFAULT 1,
                    created_at TEXT NOT NULL DEFAULT (datetime('now'))
                );
            """);

            // ---------- CATEGORIES ----------
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS categories (
                    category_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE
                );
            """);

            // ---------- RACKS / SHELVES (Library Layout) ----------
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS racks (
                    rack_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rack_name TEXT NOT NULL UNIQUE
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS shelves (
                    shelf_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    rack_id INTEGER NOT NULL,
                    shelf_name TEXT NOT NULL,
                    FOREIGN KEY (rack_id) REFERENCES racks(rack_id) ON DELETE CASCADE
                );
            """);

            // ---------- BOOKS ----------
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS books (
                    book_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    subtitle TEXT,
                    author TEXT,
                    isbn TEXT,
                    publisher TEXT,
                    edition TEXT,
                    category_id INTEGER,
                    language TEXT,
                    publication_year INTEGER,
                    purchase_date TEXT,
                    price REAL DEFAULT 0,
                    quantity INTEGER NOT NULL DEFAULT 1,
                    total_copies INTEGER NOT NULL DEFAULT 1,
                    available_copies INTEGER NOT NULL DEFAULT 1,
                    shelf_id INTEGER,
                    position TEXT,
                    cover_path TEXT,
                    description TEXT,
                    status TEXT NOT NULL DEFAULT 'AVAILABLE',
                    barcode TEXT,
                    qr_code TEXT,
                    created_at TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL,
                    FOREIGN KEY (shelf_id) REFERENCES shelves(shelf_id) ON DELETE SET NULL
                );
            """);

            // ---------- MEMBERS ----------
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS members (
                    member_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    photo_path TEXT,
                    name TEXT NOT NULL,
                    department TEXT,
                    course TEXT,
                    semester TEXT,
                    roll_number TEXT,
                    phone TEXT,
                    email TEXT,
                    address TEXT,
                    joining_date TEXT NOT NULL DEFAULT (date('now')),
                    status TEXT NOT NULL DEFAULT 'ACTIVE',
                    created_at TEXT NOT NULL DEFAULT (datetime('now'))
                );
            """);

            // ---------- TRANSACTIONS (Issue / Return) ----------
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS transactions (
                    transaction_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id INTEGER NOT NULL,
                    member_id INTEGER NOT NULL,
                    issue_date TEXT NOT NULL DEFAULT (date('now')),
                    due_date TEXT NOT NULL,
                    return_date TEXT,
                    status TEXT NOT NULL DEFAULT 'ISSUED' CHECK(status IN ('ISSUED','RETURNED','OVERDUE','LOST')),
                    issued_by INTEGER,
                    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
                    FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE,
                    FOREIGN KEY (issued_by) REFERENCES users(user_id) ON DELETE SET NULL
                );
            """);

            // ---------- FINES ----------
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS fines (
                    fine_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    transaction_id INTEGER NOT NULL,
                    amount REAL NOT NULL DEFAULT 0,
                    status TEXT NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','PAID')),
                    paid_date TEXT,
                    FOREIGN KEY (transaction_id) REFERENCES transactions(transaction_id) ON DELETE CASCADE
                );
            """);

            // ---------- RESERVATIONS ----------
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    reservation_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id INTEGER NOT NULL,
                    member_id INTEGER NOT NULL,
                    reserved_date TEXT NOT NULL DEFAULT (date('now')),
                    status TEXT NOT NULL DEFAULT 'WAITING' CHECK(status IN ('WAITING','FULFILLED','CANCELLED')),
                    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
                    FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE CASCADE
                );
            """);

            // ---------- LOST / DAMAGED ----------
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS lost_damaged_books (
                    record_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    book_id INTEGER NOT NULL,
                    member_id INTEGER,
                    type TEXT NOT NULL CHECK(type IN ('LOST','DAMAGED')),
                    replacement_cost REAL DEFAULT 0,
                    reported_date TEXT NOT NULL DEFAULT (date('now')),
                    resolved INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (book_id) REFERENCES books(book_id) ON DELETE CASCADE,
                    FOREIGN KEY (member_id) REFERENCES members(member_id) ON DELETE SET NULL
                );
            """);

            // ---------- AUDIT LOG ----------
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_log (
                    log_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    action TEXT NOT NULL,
                    details TEXT,
                    timestamp TEXT NOT NULL DEFAULT (datetime('now')),
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL
                );
            """);

            // ---------- SETTINGS (single row, key-value) ----------
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    setting_key TEXT PRIMARY KEY,
                    setting_value TEXT
                );
            """);

            seedDefaults(conn);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }

    /** Seeds a default admin user (admin/admin123) and default settings on first run only. */
    private static void seedDefaults(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM users")) {
            if (rs.next() && rs.getInt("cnt") == 0) {
                String hash = BCrypt.hashpw("admin123", BCrypt.gensalt());
                try (var ps = conn.prepareStatement(
                        "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)")) {
                    ps.setString(1, "admin");
                    ps.setString(2, hash);
                    ps.setString(3, "Administrator");
                    ps.setString(4, "ADMIN");
                    ps.executeUpdate();
                }
            }
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("INSERT OR IGNORE INTO settings (setting_key, setting_value) VALUES ('library_name', 'My Library')");
            stmt.execute("INSERT OR IGNORE INTO settings (setting_key, setting_value) VALUES ('fine_per_day', '5')");
            stmt.execute("INSERT OR IGNORE INTO settings (setting_key, setting_value) VALUES ('grace_days', '2')");
            stmt.execute("INSERT OR IGNORE INTO settings (setting_key, setting_value) VALUES ('loan_period_days', '14')");
            stmt.execute("INSERT OR IGNORE INTO settings (setting_key, setting_value) VALUES ('theme', 'light')");
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
