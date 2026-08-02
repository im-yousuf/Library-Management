package com.slms.controllers;

import com.slms.models.Fine;
import com.slms.models.Member;
import com.slms.models.Transaction;
import com.slms.services.AuditService;
import com.slms.services.MemberService;
import com.slms.services.TransactionService;
import com.slms.utils.Session;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

import java.util.List;
import java.util.Optional;

public class ReturnBookController {

    @FXML private TextField memberSearchField;
    @FXML private ComboBox<Member> memberCombo;
    @FXML private TableView<Transaction> issuedBooksTable;
    @FXML private TableColumn<Transaction, String> bookTitleCol;
    @FXML private TableColumn<Transaction, String> issueDateCol;
    @FXML private TableColumn<Transaction, String> dueDateCol;
    @FXML private TableColumn<Transaction, String> statusCol;
    @FXML private TableColumn<Transaction, Void> actionsCol;

    private final MemberService memberService = new MemberService();
    private final TransactionService transactionService = new TransactionService();

    @FXML
    public void initialize() {
        setupMemberSearch();
        setupTable();
    }

    private void setupMemberSearch() {
        memberCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Member m) {
                if (m == null) return "";
                return m.getName() + " (" + m.getRollNumber() + ")";
            }

            @Override
            public Member fromString(String string) {
                return null;
            }
        });

        memberSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().length() > 0) {
                List<Member> results = memberService.searchMembers(newVal);
                memberCombo.setItems(FXCollections.observableArrayList(results));
                if (!results.isEmpty()) {
                    memberCombo.show();
                }
            } else {
                memberCombo.getItems().clear();
            }
        });

        memberCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            loadIssuedBooks(newVal);
        });
    }

    private void setupTable() {
        bookTitleCol.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        issueDateCol.setCellValueFactory(new PropertyValueFactory<>("issueDate"));
        dueDateCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDaysStatus()));

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button returnBtn = new Button("Return");

            {
                returnBtn.getStyleClass().add("primary-button");
                returnBtn.setOnAction(e -> {
                    Transaction tx = getTableView().getItems().get(getIndex());
                    handleReturn(tx);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : returnBtn);
            }
        });
    }

    private void loadIssuedBooks(Member member) {
        if (member == null) {
            issuedBooksTable.getItems().clear();
            return;
        }
        List<Transaction> currentBooks = memberService.getCurrentBooks(member.getMemberId());
        issuedBooksTable.setItems(FXCollections.observableArrayList(currentBooks));
    }

    private void handleReturn(Transaction tx) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Return book: " + tx.getBookTitle() + "?");
        confirm.setHeaderText("Confirm Return");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                Fine fine = transactionService.returnBook(tx.getTransactionId());
                int userId = Session.getCurrentUser() != null ? Session.getCurrentUser().getUserId() : 1;
                AuditService.log(userId, "RETURN_BOOK", "Returned transaction ID " + tx.getTransactionId());
                
                if (fine != null) {
                    showFineAlert(fine);
                } else {
                    Alert success = new Alert(Alert.AlertType.INFORMATION, "Book returned successfully with no fines.");
                    success.setHeaderText("Returned");
                    success.showAndWait();
                }

                loadIssuedBooks(memberCombo.getValue());
            } catch (Exception e) {
                Alert err = new Alert(Alert.AlertType.ERROR, e.getMessage());
                err.showAndWait();
            }
        }
    }

    private void showFineAlert(Fine fine) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Overdue Fine Generated");
        alert.setHeaderText("Book returned late. Fine Amount: $" + fine.getAmount());
        alert.setContentText("The member has been charged a fine for returning this book late.");

        ButtonType payNowBtn = new ButtonType("Mark as Paid Now");
        ButtonType payLaterBtn = new ButtonType("Pay Later", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(payNowBtn, payLaterBtn);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == payNowBtn) {
            try {
                transactionService.payFine(fine.getFineId());
                int userId = Session.getCurrentUser() != null ? Session.getCurrentUser().getUserId() : 1;
                AuditService.log(userId, "PAY_FINE", "Paid fine ID " + fine.getFineId());
                
                Alert success = new Alert(Alert.AlertType.INFORMATION, "Fine marked as paid.");
                success.showAndWait();
            } catch (Exception e) {
                Alert err = new Alert(Alert.AlertType.ERROR, "Failed to mark as paid: " + e.getMessage());
                err.showAndWait();
            }
        }
    }
}
