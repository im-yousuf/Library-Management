package com.slms.controllers;

import com.slms.database.DatabaseManager;
import com.slms.models.User;
import com.slms.services.AuditService;
import com.slms.utils.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import org.mindrot.jbcrypt.BCrypt;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SettingsController {

    // Library config fields
    @FXML private TextField libraryNameField;
    @FXML private TextField loanPeriodField;
    @FXML private TextField finePerDayField;
    @FXML private TextField graceDaysField;

    // User management fields
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> nameCol;
    @FXML private TableColumn<User, String> roleCol;
    @FXML private TableColumn<User, Void> actionsCol;

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField fullNameField;
    @FXML private ComboBox<String> roleCombo;
    @FXML private Button addUserBtn;

    private ObservableList<User> usersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Load library settings
        loadSettings();

        // Setup user table columns
        usernameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUsername()));
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFullName()));
        roleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRole()));

        roleCombo.setItems(FXCollections.observableArrayList("LIBRARIAN", "ADMIN"));
        roleCombo.getSelectionModel().selectFirst();

        setupActions();
        loadUsers();

        // Disable user management for non-admins
        boolean isAdmin = "ADMIN".equals(Session.getCurrentUser().getRole());
        usernameField.setDisable(!isAdmin);
        passwordField.setDisable(!isAdmin);
        fullNameField.setDisable(!isAdmin);
        roleCombo.setDisable(!isAdmin);
        addUserBtn.setDisable(!isAdmin);
    }

    // ======================== LIBRARY SETTINGS ========================

    private void loadSettings() {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT setting_key, setting_value FROM settings")) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String key = rs.getString("setting_key");
                String val = rs.getString("setting_value");
                switch (key) {
                    case "library_name" -> libraryNameField.setText(val);
                    case "loan_period_days" -> loanPeriodField.setText(val);
                    case "fine_per_day" -> finePerDayField.setText(val);
                    case "grace_days" -> graceDaysField.setText(val);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void saveSettings() {
        try (Connection conn = DatabaseManager.getConnection()) {
            updateSetting(conn, "library_name", libraryNameField.getText().trim());
            updateSetting(conn, "loan_period_days", loanPeriodField.getText().trim());
            updateSetting(conn, "fine_per_day", finePerDayField.getText().trim());
            updateSetting(conn, "grace_days", graceDaysField.getText().trim());

            AuditService.log(Session.getCurrentUser().getUserId(), "SETTINGS", "Library settings updated");
            new Alert(Alert.AlertType.INFORMATION, "Settings saved successfully!", ButtonType.OK).showAndWait();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save settings: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }

    private void updateSetting(Connection conn, String key, String value) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT OR REPLACE INTO settings (setting_key, setting_value) VALUES (?, ?)")) {
            pstmt.setString(1, key);
            pstmt.setString(2, value);
            pstmt.executeUpdate();
        }
    }

    // ======================== BACKUP & RESTORE ========================

    @FXML
    private void createBackup() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Backup File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db"));
        fileChooser.setInitialFileName("slms_backup.db");

        File destFile = fileChooser.showSaveDialog(null);
        if (destFile != null) {
            try {
                File sourceFile = new File("data/slms.db");
                Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                AuditService.log(Session.getCurrentUser().getUserId(), "BACKUP", "Created backup at " + destFile.getAbsolutePath());
                new Alert(Alert.AlertType.INFORMATION, "Backup created successfully!", ButtonType.OK).showAndWait();
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR, "Failed to create backup: " + e.getMessage(), ButtonType.OK).showAndWait();
            }
        }
    }

    @FXML
    private void restoreBackup() {
        if (!"ADMIN".equals(Session.getCurrentUser().getRole())) {
            new Alert(Alert.AlertType.ERROR, "Only Administrators can restore backups.", ButtonType.OK).showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Restoring will overwrite ALL current data. The application will close after restoration. Are you sure?",
                ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Backup File");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db"));

                File sourceFile = fileChooser.showOpenDialog(null);
                if (sourceFile != null) {
                    try {
                        DatabaseManager.closeConnection();
                        File destFile = new File("data/slms.db");
                        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        new Alert(Alert.AlertType.INFORMATION, "Restore successful! The application will now exit. Please restart.", ButtonType.OK).showAndWait();
                        System.exit(0);
                    } catch (Exception e) {
                        new Alert(Alert.AlertType.ERROR, "Failed to restore: " + e.getMessage(), ButtonType.OK).showAndWait();
                    }
                }
            }
        });
    }

    // ======================== USER MANAGEMENT ========================

    private void loadUsers() {
        usersList.clear();
        String sql = "SELECT * FROM users WHERE is_active = 1";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                User u = new User(
                    rs.getInt("user_id"),
                    rs.getString("username"),
                    rs.getString("password_hash"),
                    rs.getString("full_name"),
                    rs.getString("role"),
                    true
                );
                usersList.add(u);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        usersTable.setItems(usersList);
    }

    @FXML
    private void addUser() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String fullName = fullNameField.getText().trim();
        String role = roleCombo.getValue();

        if (username.isEmpty() || password.isEmpty() || fullName.isEmpty()) {
            new Alert(Alert.AlertType.ERROR, "Please fill all fields.", ButtonType.OK).showAndWait();
            return;
        }

        String hash = BCrypt.hashpw(password, BCrypt.gensalt());
        String sql = "INSERT INTO users (username, password_hash, full_name, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hash);
            pstmt.setString(3, fullName);
            pstmt.setString(4, role);
            pstmt.executeUpdate();
            AuditService.log(Session.getCurrentUser().getUserId(), "USER_ADDED", "Added user: " + username);
            new Alert(Alert.AlertType.INFORMATION, "User added successfully.", ButtonType.OK).showAndWait();

            usernameField.clear();
            passwordField.clear();
            fullNameField.clear();
            loadUsers();
        } catch (SQLException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to add user. Username might already exist.", ButtonType.OK).showAndWait();
        }
    }

    private void setupActions() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");

            {
                deleteBtn.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    if (u.getUserId() == Session.getCurrentUser().getUserId()) {
                        new Alert(Alert.AlertType.ERROR, "Cannot delete yourself.", ButtonType.OK).showAndWait();
                        return;
                    }
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                            "Delete user '" + u.getUsername() + "'?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(resp -> {
                        if (resp == ButtonType.YES) {
                            String sql = "UPDATE users SET is_active = 0 WHERE user_id = ?";
                            try (Connection conn = DatabaseManager.getConnection();
                                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                                pstmt.setInt(1, u.getUserId());
                                pstmt.executeUpdate();
                                AuditService.log(Session.getCurrentUser().getUserId(), "USER_DELETED", "Deactivated user: " + u.getUsername());
                                loadUsers();
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    boolean isAdmin = "ADMIN".equals(Session.getCurrentUser().getRole());
                    setGraphic(isAdmin ? deleteBtn : null);
                }
            }
        });
    }
}
