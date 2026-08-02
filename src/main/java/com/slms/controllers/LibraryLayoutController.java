package com.slms.controllers;

import com.slms.models.Book;
import com.slms.models.Rack;
import com.slms.models.Shelf;
import com.slms.services.AuditService;
import com.slms.services.BookService;
import com.slms.services.RackShelfService;
import com.slms.utils.Session;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.util.List;


public class LibraryLayoutController {

    @FXML private TreeView<Object> layoutTreeView;
    
    @FXML private Label shelfTitleLabel;
    @FXML private TableView<Book> booksTable;
    @FXML private TableColumn<Book, String> titleCol;
    @FXML private TableColumn<Book, String> authorCol;
    @FXML private TableColumn<Book, String> positionCol;
    @FXML private TableColumn<Book, Void> actionsCol;
    
    @FXML private TextField bookSearchField;
    @FXML private ComboBox<Book> bookCombo;
    @FXML private TextField positionField;

    private final RackShelfService layoutService = new RackShelfService();
    private final BookService bookService = new BookService();
    
    private Shelf selectedShelf;

    @FXML
    public void initialize() {
        setupTreeView();
        setupBooksTable();
        setupBookSearch();
        loadLayout();
    }

    private void setupTreeView() {
        layoutTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() instanceof Shelf) {
                selectedShelf = (Shelf) newVal.getValue();
                shelfTitleLabel.setText("Contents of " + selectedShelf.getParentRack().getRackName() + " \u2192 " + selectedShelf.getShelfName());
                loadBooksOnShelf();
            } else {
                selectedShelf = null;
                shelfTitleLabel.setText("Select a Shelf");
                booksTable.getItems().clear();
            }
        });
    }

    private void setupBooksTable() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        authorCol.setCellValueFactory(new PropertyValueFactory<>("author"));
        positionCol.setCellValueFactory(new PropertyValueFactory<>("position"));
        
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");
            {
                removeBtn.getStyleClass().add("logout-button");
                removeBtn.setOnAction(e -> {
                    Book b = getTableView().getItems().get(getIndex());
                    handleRemoveBookFromShelf(b);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : removeBtn);
            }
        });
    }

    private void setupBookSearch() {
        bookCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Book b) {
                if (b == null) return "";
                return b.getTitle() + " (" + b.getAuthor() + ")";
            }
            @Override
            public Book fromString(String string) { return null; }
        });
        
        bookSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().length() > 0) {
                List<Book> res = bookService.searchBooks(newVal);
                bookCombo.setItems(FXCollections.observableArrayList(res));
                if (!res.isEmpty()) bookCombo.show();
            } else {
                bookCombo.getItems().clear();
            }
        });
    }

    private void loadLayout() {
        TreeItem<Object> root = new TreeItem<>("Root");
        List<Rack> racks = layoutService.getAllRacksWithShelves();
        
        for (Rack rack : racks) {
            TreeItem<Object> rackItem = new TreeItem<>(rack);
            rackItem.setExpanded(true);
            for (Shelf shelf : rack.getShelves()) {
                rackItem.getChildren().add(new TreeItem<>(shelf));
            }
            root.getChildren().add(rackItem);
        }
        layoutTreeView.setRoot(root);
    }

    private void loadBooksOnShelf() {
        if (selectedShelf != null) {
            List<Book> books = layoutService.getBooksOnShelf(selectedShelf.getShelfId());
            booksTable.setItems(FXCollections.observableArrayList(books));
        }
    }

    @FXML
    private void handleAddRack() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Rack");
        dialog.setHeaderText("Create a new Rack");
        dialog.setContentText("Rack Name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    layoutService.addRack(name);
                    log("ADD_RACK", "Added rack: " + name);
                    loadLayout();
                } catch (Exception e) { showError(e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleAddShelf() {
        TreeItem<Object> selected = layoutTreeView.getSelectionModel().getSelectedItem();
        if (selected == null || !(selected.getValue() instanceof Rack)) {
            showError("Please select a Rack to add a shelf to.");
            return;
        }
        Rack rack = (Rack) selected.getValue();
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Shelf");
        dialog.setHeaderText("Create a new Shelf in " + rack.getRackName());
        dialog.setContentText("Shelf Name:");
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    layoutService.addShelf(rack.getRackId(), name);
                    log("ADD_SHELF", "Added shelf " + name + " to rack " + rack.getRackName());
                    loadLayout();
                } catch (Exception e) { showError(e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleRename() {
        TreeItem<Object> selectedItem = layoutTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;
        
        Object val = selectedItem.getValue();
        TextInputDialog dialog = new TextInputDialog(val.toString());
        dialog.setTitle("Rename");
        dialog.setHeaderText("Rename " + (val instanceof Rack ? "Rack" : "Shelf"));
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    if (val instanceof Rack) {
                        layoutService.renameRack(((Rack) val).getRackId(), name);
                        log("RENAME_RACK", "Renamed rack to " + name);
                    } else if (val instanceof Shelf) {
                        layoutService.renameShelf(((Shelf) val).getShelfId(), name);
                        log("RENAME_SHELF", "Renamed shelf to " + name);
                    }
                    loadLayout();
                } catch (Exception e) { showError(e.getMessage()); }
            }
        });
    }

    @FXML
    private void handleDelete() {
        TreeItem<Object> selectedItem = layoutTreeView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) return;
        
        Object val = selectedItem.getValue();
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this?", ButtonType.YES, ButtonType.NO);
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            try {
                if (val instanceof Rack) {
                    layoutService.deleteRack(((Rack) val).getRackId());
                    log("DELETE_RACK", "Deleted rack: " + ((Rack) val).getRackName());
                } else if (val instanceof Shelf) {
                    layoutService.deleteShelf(((Shelf) val).getShelfId());
                    log("DELETE_SHELF", "Deleted shelf: " + ((Shelf) val).getShelfName());
                }
                loadLayout();
                selectedShelf = null;
                booksTable.getItems().clear();
                shelfTitleLabel.setText("Select a Shelf");
            } catch (Exception e) { showError(e.getMessage()); }
        }
    }

    @FXML
    private void handleAssignBook() {
        if (selectedShelf == null) {
            showError("Please select a shelf first.");
            return;
        }
        Book b = bookCombo.getValue();
        if (b == null) {
            showError("Please select a book.");
            return;
        }
        try {
            layoutService.assignBookToShelf(b.getBookId(), selectedShelf.getShelfId(), positionField.getText().trim());
            log("ASSIGN_SHELF", "Assigned book ID " + b.getBookId() + " to shelf ID " + selectedShelf.getShelfId());
            
            bookSearchField.clear();
            bookCombo.getSelectionModel().clearSelection();
            positionField.clear();
            loadBooksOnShelf();
        } catch (Exception e) {
            showError("Failed to assign book.");
        }
    }
    
    private void handleRemoveBookFromShelf(Book b) {
        try {
            layoutService.assignBookToShelf(b.getBookId(), null, null);
            log("REMOVE_SHELF", "Removed book ID " + b.getBookId() + " from shelf");
            loadBooksOnShelf();
        } catch (Exception e) {
            showError("Failed to unassign book.");
        }
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
    
    private void log(String action, String details) {
        int uid = Session.getCurrentUser() != null ? Session.getCurrentUser().getUserId() : 1;
        AuditService.log(uid, action, details);
    }
}
