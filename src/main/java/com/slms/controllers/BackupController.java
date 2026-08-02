package com.slms.controllers;

import com.slms.services.AuditService;
import com.slms.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class BackupController {

    @FXML
    public void initialize() {
    }

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
                e.printStackTrace();
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

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Restoring a backup will overwrite current data. The application will close after restoration. Are you sure?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Select Backup File");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("SQLite Database", "*.db"));
                
                File sourceFile = fileChooser.showOpenDialog(null);
                if (sourceFile != null) {
                    try {
                        com.slms.database.DatabaseManager.closeConnection(); // Close DB before overwriting
                        File destFile = new File("data/slms.db");
                        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        new Alert(Alert.AlertType.INFORMATION, "Restore successful! The application will now exit. Please restart.", ButtonType.OK).showAndWait();
                        System.exit(0);
                    } catch (Exception e) {
                        e.printStackTrace();
                        new Alert(Alert.AlertType.ERROR, "Failed to restore backup: " + e.getMessage(), ButtonType.OK).showAndWait();
                    }
                }
            }
        });
    }
}
