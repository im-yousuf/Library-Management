package com.slms.controllers;

import com.slms.models.Fine;
import com.slms.services.FineService;
import com.slms.utils.Session;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.util.List;
import java.util.Optional;

public class FinesController {

    @FXML private TableView<Fine> finesTable;
    @FXML private TableColumn<Fine, String> memberCol;
    @FXML private TableColumn<Fine, String> bookCol;
    @FXML private TableColumn<Fine, Number> amountCol;
    @FXML private TableColumn<Fine, String> statusCol;
    @FXML private TableColumn<Fine, String> dueDateCol;
    @FXML private TableColumn<Fine, Number> daysOverdueCol;
    @FXML private TableColumn<Fine, Void> actionsCol;
    @FXML private ToggleGroup filterGroup;
    @FXML private Label totalPendingLabel;
    @FXML private TextField finePerDayField;
    @FXML private TextField graceDaysField;

    private final FineService fineService = new FineService();
    private final ObservableList<Fine> finesList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        memberCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getMemberName() != null ? cd.getValue().getMemberName() : "N/A"));
        bookCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getBookTitle() != null ? cd.getValue().getBookTitle() : "N/A"));
        amountCol.setCellValueFactory(cd -> new SimpleDoubleProperty(cd.getValue().getAmount()));
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
        dueDateCol.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getDueDate() != null ? cd.getValue().getDueDate() : "-"));
        daysOverdueCol.setCellValueFactory(cd -> new SimpleIntegerProperty(cd.getValue().getDaysOverdue()));

        setupActionsColumn();
        loadFineSettings();
        loadFines("ALL");

        // Role gating: only ADMIN can change fine settings
        boolean isAdmin = Session.getCurrentUser() != null && "ADMIN".equals(Session.getCurrentUser().getRole());
        finePerDayField.setDisable(!isAdmin);
        graceDaysField.setDisable(!isAdmin);
    }

    @FXML
    private void applyFilter() {
        RadioButton selected = (RadioButton) filterGroup.getSelectedToggle();
        String filter = selected != null ? selected.getText().toUpperCase() : "ALL";
        loadFines(filter);
    }

    private void loadFines(String filter) {
        List<Fine> fines = fineService.getAllFines(filter);
        finesList.setAll(fines);
        finesTable.setItems(finesList);

        double totalPending = fines.stream()
                .filter(f -> "PENDING".equals(f.getStatus()))
                .mapToDouble(Fine::getAmount)
                .sum();
        totalPendingLabel.setText(String.format("₹ %.2f", totalPending));
    }

    private void loadFineSettings() {
        try (var conn = com.slms.database.DatabaseManager.getConnection();
             var stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT setting_value FROM settings WHERE setting_key = 'fine_per_day'");
            if (rs.next()) finePerDayField.setText(rs.getString("setting_value"));

            rs = stmt.executeQuery("SELECT setting_value FROM settings WHERE setting_key = 'grace_days'");
            if (rs.next()) graceDaysField.setText(rs.getString("setting_value"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void saveFineSettings() {
        try {
            double finePerDay = Double.parseDouble(finePerDayField.getText().trim());
            int graceDays = Integer.parseInt(graceDaysField.getText().trim());
            if (fineService.updateFineSettings(finePerDay, graceDays)) {
                new Alert(Alert.AlertType.INFORMATION, "Fine settings updated.", ButtonType.OK).showAndWait();
            } else {
                new Alert(Alert.AlertType.ERROR, "Failed to update settings.", ButtonType.OK).showAndWait();
            }
        } catch (NumberFormatException e) {
            new Alert(Alert.AlertType.ERROR, "Please enter valid numbers.", ButtonType.OK).showAndWait();
        }
    }

    private void setupActionsColumn() {
        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button payBtn = new Button("Pay");
            private final Button waiveBtn = new Button("Waive");

            {
                payBtn.setOnAction(e -> {
                    Fine fine = getTableView().getItems().get(getIndex());
                    if (fineService.markFinePaid(fine.getFineId())) {
                        applyFilter();
                    }
                });
                waiveBtn.setOnAction(e -> {
                    Fine fine = getTableView().getItems().get(getIndex());
                    TextInputDialog dialog = new TextInputDialog();
                    dialog.setTitle("Waive Fine");
                    dialog.setHeaderText("Provide a reason for waiving this fine:");
                    Optional<String> result = dialog.showAndWait();
                    result.ifPresent(reason -> {
                        if (fineService.waiveFine(fine.getFineId(), reason)) {
                            applyFilter();
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
                    Fine fine = getTableView().getItems().get(getIndex());
                    if ("PENDING".equals(fine.getStatus())) {
                        javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(5, payBtn, waiveBtn);
                        setGraphic(box);
                    } else {
                        setGraphic(new Label("Settled"));
                    }
                }
            }
        });
    }
}
