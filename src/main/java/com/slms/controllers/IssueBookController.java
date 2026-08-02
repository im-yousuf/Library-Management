package com.slms.controllers;

import com.slms.database.DatabaseManager;
import com.slms.models.Book;
import com.slms.models.Member;
import com.slms.models.Transaction;
import com.slms.services.AuditService;
import com.slms.services.BookService;
import com.slms.services.MemberService;
import com.slms.services.TransactionService;
import com.slms.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.Callback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class IssueBookController {

    @FXML private TextField memberSearchField;
    @FXML private ListView<Member> memberListView;
    @FXML private Label selectedMemberLabel;

    @FXML private TextField bookSearchField;
    @FXML private ListView<Book> bookListView;
    @FXML private Label selectedBookLabel;

    @FXML private Label confirmMemberLabel;
    @FXML private Label confirmBookLabel;
    @FXML private Label confirmIssueDateLabel;
    @FXML private Label confirmDueDateLabel;

    @FXML private Label errorLabel;
    @FXML private Button issueButton;

    private final MemberService memberService = new MemberService();
    private final BookService bookService = new BookService();
    private final TransactionService transactionService = new TransactionService();

    private Member selectedMember;
    private Book selectedBook;

    @FXML
    public void initialize() {
        setupMemberSearch();
        setupBookSearch();
        updateConfirmation();
    }

    private void setupMemberSearch() {
        memberListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Member> call(ListView<Member> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Member item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getName() + " (" + item.getRollNumber() + ") - " + item.getStatus());
                        }
                    }
                };
            }
        });

        memberSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().length() > 0) {
                List<Member> results = memberService.searchMembers(newVal);
                memberListView.setItems(FXCollections.observableArrayList(results));
            } else {
                memberListView.getItems().clear();
            }
        });

        memberListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedMember = newVal;
            if (newVal != null) {
                selectedMemberLabel.setText("Selected: " + newVal.getName());
            } else {
                selectedMemberLabel.setText("");
            }
            updateConfirmation();
        });
    }

    private void setupBookSearch() {
        bookListView.setCellFactory(new Callback<>() {
            @Override
            public ListCell<Book> call(ListView<Book> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(Book item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getTitle() + " by " + item.getAuthor() + " | Avail: " + item.getAvailableCopies());
                        }
                    }
                };
            }
        });

        bookSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().length() > 0) {
                List<Book> results = bookService.searchBooks(newVal);
                bookListView.setItems(FXCollections.observableArrayList(results));
            } else {
                bookListView.getItems().clear();
            }
        });

        bookListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedBook = newVal;
            if (newVal != null) {
                selectedBookLabel.setText("Selected: " + newVal.getTitle());
            } else {
                selectedBookLabel.setText("");
            }
            updateConfirmation();
        });
    }

    private void updateConfirmation() {
        errorLabel.setText("");
        if (selectedMember != null && selectedBook != null) {
            confirmMemberLabel.setText(selectedMember.getName() + " (" + selectedMember.getRollNumber() + ")");
            confirmBookLabel.setText(selectedBook.getTitle());

            confirmIssueDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
            
            // Read loan period
            int loanPeriodDays = 14;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT setting_value FROM settings WHERE setting_key = 'loan_period_days'")) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    loanPeriodDays = Integer.parseInt(rs.getString("setting_value"));
                }
            } catch (Exception ignored) {}
            
            confirmDueDateLabel.setText(LocalDate.now().plusDays(loanPeriodDays).format(DateTimeFormatter.ISO_LOCAL_DATE));
            issueButton.setDisable(false);
        } else {
            confirmMemberLabel.setText("-");
            confirmBookLabel.setText("-");
            confirmIssueDateLabel.setText("-");
            confirmDueDateLabel.setText("-");
            issueButton.setDisable(true);
        }
    }

    @FXML
    private void handleIssueBook() {
        if (selectedMember == null || selectedBook == null) return;
        
        try {
            int userId = Session.getCurrentUser() != null ? Session.getCurrentUser().getUserId() : 1;
            Transaction tx = transactionService.issueBook(selectedBook.getBookId(), selectedMember.getMemberId(), userId);
            
            AuditService.log(userId, "ISSUE_BOOK", "Issued book ID " + selectedBook.getBookId() + " to member ID " + selectedMember.getMemberId());
            
            Alert success = new Alert(Alert.AlertType.INFORMATION, "Book issued successfully!\nDue date is: " + tx.getDueDate(), ButtonType.OK);
            success.setHeaderText("Success");
            success.showAndWait();
            
            clearForm();
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
            Alert error = new Alert(Alert.AlertType.ERROR, e.getMessage(), ButtonType.OK);
            error.setHeaderText("Issue Failed");
            error.showAndWait();
        }
    }

    private void clearForm() {
        memberSearchField.clear();
        bookSearchField.clear();
        selectedMember = null;
        selectedBook = null;
        updateConfirmation();
    }
}
