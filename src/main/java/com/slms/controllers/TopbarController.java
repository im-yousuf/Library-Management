package com.slms.controllers;

import com.slms.models.Book;
import com.slms.models.Member;
import com.slms.models.SearchResult;
import com.slms.services.BookService;
import com.slms.services.GlobalSearchService;
import com.slms.services.MemberService;
import com.slms.utils.SceneManager;
import com.slms.utils.Session;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Side;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class TopbarController {

    @FXML private Label welcomeLabel;
    @FXML private Label roleLabel;
    @FXML private TextField globalSearchField;

    private final GlobalSearchService searchService = new GlobalSearchService();
    private ContextMenu searchPopup;

    @FXML
    public void initialize() {
        var user = Session.getCurrentUser();
        if (user != null) {
            welcomeLabel.setText("Welcome, " + user.getFullName());
            roleLabel.setText(user.getRole());
        }

        setupGlobalSearch();
    }

    private void setupGlobalSearch() {
        searchPopup = new ContextMenu();
        
        globalSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                searchPopup.hide();
            } else {
                List<SearchResult> results = searchService.search(newVal);
                populatePopup(results);
            }
        });

        globalSearchField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal) {
                // slightly delay hide to allow clicks to register
                new Thread(() -> {
                    try { Thread.sleep(200); } catch (Exception e) {}
                    Platform.runLater(searchPopup::hide);
                }).start();
            } else if (!globalSearchField.getText().trim().isEmpty() && !searchPopup.getItems().isEmpty()) {
                searchPopup.show(globalSearchField, Side.BOTTOM, 0, 0);
            }
        });
    }

    private void populatePopup(List<SearchResult> results) {
        searchPopup.getItems().clear();
        
        if (results.isEmpty()) {
            CustomMenuItem item = new CustomMenuItem(new Label("No results found"), false);
            searchPopup.getItems().add(item);
        } else {
            for (SearchResult res : results) {
                VBox box = new VBox(2);
                Label title = new Label("[" + res.getType() + "] " + res.getLabel());
                title.setStyle("-fx-font-weight: bold;");
                Label sub = new Label(res.getSubtitle());
                sub.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");
                box.getChildren().addAll(title, sub);
                
                CustomMenuItem item = new CustomMenuItem(box, true);
                item.setOnAction(e -> handleSearchSelection(res));
                searchPopup.getItems().add(item);
            }
        }

        if (!searchPopup.isShowing() && globalSearchField.getScene() != null && globalSearchField.getScene().getWindow() != null) {
            searchPopup.show(globalSearchField, Side.BOTTOM, 0, 0);
        }
    }

    private void handleSearchSelection(SearchResult res) {
        globalSearchField.clear();
        searchPopup.hide();

        try {
            switch (res.getType()) {
                case BOOK:
                    BookService bs = new BookService();
                    List<Book> books = bs.searchBooks("");
                    Book targetBook = books.stream().filter(b -> b.getBookId() == res.getId()).findFirst().orElse(null);
                    if (targetBook != null) {
                        FXMLLoader loader = SceneManager.showModal("/views/book-form.fxml", "Edit Book");
                        BookFormController controller = loader.getController();
                        controller.setBook(targetBook);
                        Parent root = loader.getRoot();
                        Stage stage = (Stage) root.getScene().getWindow();
                        stage.showAndWait();
                    }
                    break;
                case MEMBER:
                    MemberService ms = new MemberService();
                    List<Member> members = ms.searchMembers("");
                    Member targetMember = members.stream().filter(m -> m.getMemberId() == res.getId()).findFirst().orElse(null);
                    if (targetMember != null) {
                        FXMLLoader loader = SceneManager.showModal("/views/member-detail.fxml", "Member Detail");
                        MemberDetailController controller = loader.getController();
                        controller.setMember(targetMember);
                        Parent root = loader.getRoot();
                        Stage stage = (Stage) root.getScene().getWindow();
                        stage.showAndWait();
                    }
                    break;
                case SHELF:
                    // Navigate to Layout screen
                    SceneManager.switchTo("/views/library-layout.fxml", "SLMS - Library Layout", 1000, 700);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
