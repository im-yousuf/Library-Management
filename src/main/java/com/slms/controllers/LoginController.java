package com.slms.controllers;

import com.slms.models.User;
import com.slms.services.AuthService;
import com.slms.utils.SceneManager;
import com.slms.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private TextField passwordVisibleField; // shown when "show password" is checked
    @FXML private CheckBox showPasswordCheck;
    @FXML private CheckBox rememberMeCheck;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        errorLabel.setText("");

        // Keep the hidden/visible password fields in sync
        passwordVisibleField.textProperty().bindBidirectional(passwordField.textProperty());
        passwordVisibleField.setManaged(false);
        passwordVisibleField.setVisible(false);

        showPasswordCheck.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            passwordVisibleField.setManaged(isSelected);
            passwordVisibleField.setVisible(isSelected);
            passwordField.setManaged(!isSelected);
            passwordField.setVisible(!isSelected);
        });

        loginButton.setDefaultButton(true);
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            return;
        }

        User user = authService.authenticate(username, password);
        if (user == null) {
            errorLabel.setText("Invalid username or password.");
            return;
        }

        Session.login(user);
        SceneManager.switchTo("/views/dashboard.fxml", "Smart Library Management System - Dashboard", 1200, 750);
    }

    @FXML
    private void handleForgotPassword() {
        errorLabel.setStyle("-fx-text-fill: #2563eb;");
        errorLabel.setText("Please contact your system administrator to reset the password.");
    }
}
