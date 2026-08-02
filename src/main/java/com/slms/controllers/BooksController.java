package com.slms.controllers;

import com.slms.models.Book;
import com.slms.services.AuditService;
import com.slms.services.BookService;
import com.slms.utils.SceneManager;
import com.slms.utils.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Optional;

public class BooksController {

    @FXML private TextField searchField;
    @FXML private TableView<Book> booksTable;
    @FXML private TableColumn<Book, String> titleCol;
    @FXML private TableColumn<Book, String> authorCol;
    @FXML private TableColumn<Book, String> isbnCol;
    @FXML private TableColumn<Book, String> categoryCol;
    @FXML private TableColumn<Book, String> locationCol;
    @FXML private TableColumn<Book, String> copiesCol;
    @FXML private TableColumn<Book, String> statusCol;
    @FXML private TableColumn<Book, Void> actionsCol;

    private final BookService bookService = new BookService();
    private final com.slms.services.RackShelfService layoutService = new com.slms.services.RackShelfService();
    private ObservableList<Book> booksList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        loadBooks("");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            loadBooks(newVal);
        });
    }

    private void setupColumns() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        isbnCol.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        locationCol.setCellValueFactory(cellData -> {
            int bookId = cellData.getValue().getBookId();
            return new SimpleStringProperty(layoutService.getShelfLocationLabel(bookId));
        });

        copiesCol.setCellValueFactory(cellData -> {
            Book b = cellData.getValue();
            return new SimpleStringProperty(b.getAvailableCopies() + " / " + b.getQuantity());
        });

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(8, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("nav-button");
                deleteBtn.getStyleClass().add("logout-button");

                editBtn.setOnAction(e -> {
                    Book book = getTableView().getItems().get(getIndex());
                    openBookForm(book);
                });

                deleteBtn.setOnAction(e -> {
                    Book book = getTableView().getItems().get(getIndex());
                    handleDelete(book);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadBooks(String query) {
        List<Book> list = bookService.searchBooks(query);
        booksList.setAll(list);
        booksTable.setItems(booksList);
        // Auto-select the most recently added book (highest ID) if present
        if (!booksList.isEmpty()) {
            Book newest = booksList.stream()
                    .max((b1, b2) -> Integer.compare(b1.getBookId(), b2.getBookId()))
                    .orElse(null);
            if (newest != null) {
                booksTable.getSelectionModel().select(newest);
                booksTable.scrollTo(newest);
            }
        }
    }

    @FXML
    private void handleAddBook() {
        openBookForm(null);
    }

    private void openBookForm(Book book) {
        try {
            FXMLLoader loader = SceneManager.showModal("/views/book-form.fxml", book == null ? "Add Book" : "Edit Book");
            Parent root = loader.getRoot(); // Scene is already created in showModal
            
            BookFormController controller = loader.getController();
            controller.setBook(book);
            
            Stage stage = (Stage) root.getScene().getWindow();
            stage.showAndWait();
            
            // Reload after form is closed
            loadBooks(searchField.getText());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open book form.");
        }
    }

    private void handleDelete(Book book) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Book: " + book.getTitle());
        confirm.setContentText("Are you sure you want to delete this book? This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (bookService.deleteBook(book.getBookId())) {
                    AuditService.log(Session.getCurrentUser().getUserId(), "DELETE_BOOK", "Deleted book ID " + book.getBookId());
                    loadBooks(searchField.getText());
                } else {
                    showError("Failed to delete book.");
                }
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        }
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText("Error");
        alert.showAndWait();
    }
}
