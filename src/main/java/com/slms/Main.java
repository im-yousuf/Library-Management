package com.slms;

import com.slms.database.DatabaseManager;
import com.slms.utils.SceneManager;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Create tables + seed default admin (admin / admin123) on first run.
        DatabaseManager.initializeDatabase();

        SceneManager.setPrimaryStage(primaryStage);
        primaryStage.setResizable(true);
        SceneManager.switchTo("/views/login.fxml", "Smart Library Management System - Login", 900, 600);
    }

    @Override
    public void stop() {
        DatabaseManager.closeConnection();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
