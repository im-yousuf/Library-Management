package com.slms.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/** Central place to swap the root scene/content on the primary Stage. */
public class SceneManager {

    private static Stage primaryStage;

    private SceneManager() {
    }

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    public static void switchTo(String fxmlPath, String title, int width, int height) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, width, height);
            scene.getStylesheets().add(SceneManager.class.getResource("/css/style.css").toExternalForm());
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            
            // App Branding Icon
            try {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(SceneManager.class.getResourceAsStream("/icons/app_icon.png"));
                primaryStage.getIcons().add(icon);
            } catch (Exception e) {
                // Ignore if icon is missing
            }
            
            primaryStage.centerOnScreen();
            primaryStage.show();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML: " + fxmlPath, e);
        }
    }

    public static FXMLLoader showModal(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(SceneManager.class.getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(SceneManager.class.getResource("/css/style.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setScene(scene);
            
            // App Branding Icon
            try {
                javafx.scene.image.Image icon = new javafx.scene.image.Image(SceneManager.class.getResourceAsStream("/icons/app_icon.png"));
                stage.getIcons().add(icon);
            } catch (Exception e) {
                // Ignore if icon is missing
            }
            
            stage.initOwner(primaryStage);
            stage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            stage.setResizable(false);
            
            // Allow the controller to access the stage to close itself if needed.
            // Or the caller can handle it. We return the loader so caller can call showAndWait() 
            // after getting the controller. Wait, showAndWait blocks.
            // Let's just return the loader and let the caller show it.
            return loader;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load FXML for modal: " + fxmlPath, e);
        }
    }
}
