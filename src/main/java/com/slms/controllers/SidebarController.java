package com.slms.controllers;

import com.slms.services.AuditService;
import com.slms.utils.SceneManager;
import com.slms.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

public class SidebarController {

    @FXML
    private void goToDashboard() {
        SceneManager.switchTo("/views/dashboard.fxml", "Smart Library Management System - Dashboard", 1000, 700);
    }

    @FXML
    private void goToBooks() {
        SceneManager.switchTo("/views/books.fxml", "SLMS - Book Management", 1000, 700);
    }

    @FXML
    private void goToMembers() {
        SceneManager.switchTo("/views/members.fxml", "SLMS - Member Management", 1000, 700);
    }
    @FXML private void goToIssue()   {
        SceneManager.switchTo("/views/issue-book.fxml", "SLMS - Issue Book", 1000, 700);
    }
    @FXML private void goToReturn()  {
        SceneManager.switchTo("/views/return-book.fxml", "SLMS - Return Book", 1000, 700);
    }
    @FXML private void goToReports() { notImplemented("Reports"); }
    @FXML private void goToLayout()  {
        SceneManager.switchTo("/views/library-layout.fxml", "SLMS - Library Layout", 1000, 700);
    }
    @FXML private void goToFines()   { notImplemented("Fine Management"); }
    @FXML private void goToSettings(){ notImplemented("Settings"); }
    @FXML private void goToBackup()  { notImplemented("Backup"); }

    @FXML
    private void handleLogout() {
        var user = Session.getCurrentUser();
        if (user != null) {
            AuditService.log(user.getUserId(), "LOGOUT", "User logged out");
        }
        Session.logout();
        SceneManager.switchTo("/views/login.fxml", "Smart Library Management System - Login", 900, 600);
    }

    private void notImplemented(String feature) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, feature + " is coming in a later phase.", ButtonType.OK);
        alert.setHeaderText("Coming Soon");
        alert.showAndWait();
    }
}
