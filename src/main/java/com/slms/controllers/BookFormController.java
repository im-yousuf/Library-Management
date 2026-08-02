package com.slms.controllers;

import com.slms.models.Book;
import com.slms.models.Category;
import com.slms.services.AuditService;
import com.slms.services.BookService;
import com.slms.utils.Session;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class BookFormController {

    @FXML private Label formTitle;
    @FXML private TextField titleField;
    @FXML private TextField subtitleField;
    @FXML private TextField authorField;
    @FXML private TextField isbnField;
    @FXML private TextField publisherField;
    @FXML private TextField editionField;
    @FXML private ComboBox<Category> categoryCombo;
    @FXML private TextField languageField;
    @FXML private TextField yearField;
    @FXML private TextField priceField;
    @FXML private TextField quantityField;
    @FXML private DatePicker purchaseDatePicker;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<com.slms.models.Shelf> shelfCombo;
    @FXML private TextField positionField;

    private final BookService bookService = new BookService();
    private final com.slms.services.RackShelfService layoutService = new com.slms.services.RackShelfService();
    private Book currentBook;

    @FXML
    public void initialize() {
        loadCategories();
        loadShelves();
    }

    private void loadShelves() {
        java.util.List<com.slms.models.Rack> racks = layoutService.getAllRacksWithShelves();
        java.util.List<com.slms.models.Shelf> allShelves = new java.util.ArrayList<>();
        for (com.slms.models.Rack r : racks) {
            allShelves.addAll(r.getShelves());
        }
        shelfCombo.getItems().setAll(allShelves);
    }

    private void loadCategories() {
        categoryCombo.getItems().setAll(bookService.getAllCategories());
    }

    public void setBook(Book book) {
        this.currentBook = book;
        if (book != null) {
            formTitle.setText("Edit Book");
            titleField.setText(book.getTitle());
            subtitleField.setText(book.getSubtitle());
            authorField.setText(book.getAuthor());
            isbnField.setText(book.getIsbn());
            publisherField.setText(book.getPublisher());
            editionField.setText(book.getEdition());
            
            if (book.getCategoryId() != null) {
                categoryCombo.getItems().stream()
                    .filter(c -> c.getCategoryId() == book.getCategoryId())
                    .findFirst()
                    .ifPresent(c -> categoryCombo.getSelectionModel().select(c));
            }

            languageField.setText(book.getLanguage());
            yearField.setText(book.getPublicationYear() != null ? String.valueOf(book.getPublicationYear()) : "");
            priceField.setText(String.valueOf(book.getPrice()));
            quantityField.setText(String.valueOf(book.getQuantity()));
            
            if (book.getPurchaseDate() != null && !book.getPurchaseDate().isEmpty()) {
                try {
                    purchaseDatePicker.setValue(LocalDate.parse(book.getPurchaseDate()));
                } catch (Exception ignored) {}
            }
            
            descriptionArea.setText(book.getDescription());
            
            if (book.getShelfId() != null) {
                shelfCombo.getItems().stream()
                    .filter(s -> s.getShelfId() == book.getShelfId())
                    .findFirst()
                    .ifPresent(s -> shelfCombo.getSelectionModel().select(s));
            }
            positionField.setText(book.getPosition());
        }
    }

    @FXML
    private void handleAddCategory() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Category");
        dialog.setHeaderText("Add a new book category");
        dialog.setContentText("Category Name:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                Category newCat = bookService.addCategory(name.trim());
                if (newCat != null) {
                    loadCategories();
                    categoryCombo.getSelectionModel().select(newCat);
                } else {
                    showError("Failed to add category. It might already exist.");
                }
            }
        });
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        boolean isNew = (currentBook == null);
        if (isNew) {
            currentBook = new Book();
        }

        currentBook.setTitle(titleField.getText().trim());
        currentBook.setSubtitle(subtitleField.getText().trim());
        currentBook.setAuthor(authorField.getText().trim());
        currentBook.setIsbn(isbnField.getText().trim());
        currentBook.setPublisher(publisherField.getText().trim());
        currentBook.setEdition(editionField.getText().trim());
        
        Category selectedCat = categoryCombo.getSelectionModel().getSelectedItem();
        currentBook.setCategoryId(selectedCat != null ? selectedCat.getCategoryId() : null);
        
        currentBook.setLanguage(languageField.getText().trim());
        
        String yearText = yearField.getText().trim();
        currentBook.setPublicationYear(yearText.isEmpty() ? null : Integer.parseInt(yearText));
        
        try {
            currentBook.setPrice(Double.parseDouble(priceField.getText().trim()));
        } catch (NumberFormatException e) {
            currentBook.setPrice(0.0);
        }

        int newQty = Integer.parseInt(quantityField.getText().trim());
        if (isNew) {
            currentBook.setQuantity(newQty);
            currentBook.setAvailableCopies(newQty);
        } else {
            // Adjust available copies based on quantity change
            int diff = newQty - currentBook.getQuantity();
            currentBook.setQuantity(newQty);
            currentBook.setAvailableCopies(currentBook.getAvailableCopies() + diff);
        }

        LocalDate date = purchaseDatePicker.getValue();
        currentBook.setPurchaseDate(date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : null);
        
        currentBook.setDescription(descriptionArea.getText().trim());
        
        com.slms.models.Shelf selectedShelf = shelfCombo.getSelectionModel().getSelectedItem();
        currentBook.setShelfId(selectedShelf != null ? selectedShelf.getShelfId() : null);
        currentBook.setPosition(positionField.getText().trim());

        boolean success = isNew ? bookService.addBook(currentBook) : bookService.updateBook(currentBook);

        if (success) {
            String action = isNew ? "ADD_BOOK" : "EDIT_BOOK";
            String details = (isNew ? "Added" : "Edited") + " book: " + currentBook.getTitle();
            AuditService.log(Session.getCurrentUser().getUserId(), action, details);
            closeStage();
        } else {
            showError("Failed to save the book.");
        }
    }

    private boolean validateInput() {
        if (titleField.getText().trim().isEmpty()) {
            showError("Title is required.");
            return false;
        }

        String qtyText = quantityField.getText().trim();
        if (qtyText.isEmpty()) {
            showError("Quantity is required.");
            return false;
        }

        try {
            int qty = Integer.parseInt(qtyText);
            if (qty < 0) {
                showError("Quantity must be a positive integer.");
                return false;
            }
        } catch (NumberFormatException e) {
            showError("Quantity must be a valid integer.");
            return false;
        }
        
        String yearText = yearField.getText().trim();
        if (!yearText.isEmpty()) {
            try {
                Integer.parseInt(yearText);
            } catch (NumberFormatException e) {
                showError("Publication Year must be a valid integer.");
                return false;
            }
        }
        
        String priceText = priceField.getText().trim();
        if (!priceText.isEmpty()) {
            try {
                Double.parseDouble(priceText);
            } catch (NumberFormatException e) {
                showError("Price must be a valid number.");
                return false;
            }
        }

        String isbn = isbnField.getText().trim();
        if (!isbn.isEmpty() && !isbn.matches("^[0-9-X]{10,17}$")) {
            showError("ISBN format is invalid.");
            return false;
        }

        return true;
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) titleField.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText("Validation Error");
        alert.showAndWait();
    }
}
