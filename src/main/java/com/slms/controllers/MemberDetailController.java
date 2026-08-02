package com.slms.controllers;

import com.slms.models.Member;
import com.slms.models.Transaction;
import com.slms.services.MemberService;
import com.slms.utils.SceneManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class MemberDetailController {

    @FXML private Label fineLabel;
    @FXML private ImageView photoView;
    @FXML private Label nameLabel;
    @FXML private Label rollLabel;
    @FXML private Label deptLabel;
    @FXML private Label statusLabel;
    @FXML private Label phoneLabel;
    @FXML private Label emailLabel;
    @FXML private Label joinedLabel;

    @FXML private TableView<Transaction> currentBooksTable;
    @FXML private TableColumn<Transaction, String> curBookCol;
    @FXML private TableColumn<Transaction, String> curIssueCol;
    @FXML private TableColumn<Transaction, String> curDueCol;
    @FXML private TableColumn<Transaction, String> curStatusCol;

    @FXML private TableView<Transaction> historyTable;
    @FXML private TableColumn<Transaction, String> histBookCol;
    @FXML private TableColumn<Transaction, String> histIssueCol;
    @FXML private TableColumn<Transaction, String> histReturnCol;
    @FXML private TableColumn<Transaction, String> histStatusCol;
    @FXML private TableColumn<Transaction, String> histFineCol;

    private final MemberService memberService = new MemberService();
    private Member currentMember;

    @FXML
    public void initialize() {
        // Setup Current Books Table
        curBookCol.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        curIssueCol.setCellValueFactory(new PropertyValueFactory<>("issueDate"));
        curDueCol.setCellValueFactory(new PropertyValueFactory<>("dueDate"));
        curStatusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDaysStatus()));

        // Setup History Table
        histBookCol.setCellValueFactory(new PropertyValueFactory<>("bookTitle"));
        histIssueCol.setCellValueFactory(new PropertyValueFactory<>("issueDate"));
        histReturnCol.setCellValueFactory(cellData -> {
            String ret = cellData.getValue().getReturnDate();
            return new SimpleStringProperty(ret == null ? "-" : ret);
        });
        histStatusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        histFineCol.setCellValueFactory(cellData -> {
            double fine = cellData.getValue().getFineAmount();
            return new SimpleStringProperty(fine > 0 ? "$" + fine : "-");
        });
    }

    public void setMember(Member member) {
        this.currentMember = member;
        refreshData();
    }
    
    private void refreshData() {
        if (currentMember == null) return;
        
        nameLabel.setText(currentMember.getName());
        rollLabel.setText(currentMember.getRollNumber());
        deptLabel.setText(currentMember.getDeptCourse());
        statusLabel.setText(currentMember.getStatus());
        phoneLabel.setText(currentMember.getPhone());
        emailLabel.setText(currentMember.getEmail());
        joinedLabel.setText(currentMember.getJoiningDate());

        if (currentMember.getPhotoPath() != null && !currentMember.getPhotoPath().isEmpty()) {
            File photo = new File(currentMember.getPhotoPath());
            if (photo.exists()) {
                photoView.setImage(new Image(photo.toURI().toString()));
            } else {
                photoView.setImage(null);
            }
        } else {
            photoView.setImage(null);
        }

        // Load Tables
        List<Transaction> currentBooks = memberService.getCurrentBooks(currentMember.getMemberId());
        currentBooksTable.setItems(FXCollections.observableArrayList(currentBooks));

        List<Transaction> history = memberService.getMemberBorrowHistory(currentMember.getMemberId());
        historyTable.setItems(FXCollections.observableArrayList(history));

        // Load Fine
        double outstandingFine = memberService.getOutstandingFine(currentMember.getMemberId());
        if (outstandingFine > 0) {
            fineLabel.setText("Outstanding Fine: $" + outstandingFine);
            fineLabel.setVisible(true);
        } else {
            fineLabel.setVisible(false);
        }
    }

    @FXML
    private void handleEdit() {
        try {
            FXMLLoader loader = SceneManager.showModal("/views/member-form.fxml", "Edit Member");
            Parent root = loader.getRoot();
            
            MemberFormController controller = loader.getController();
            controller.setMember(currentMember);
            
            Stage stage = (Stage) root.getScene().getWindow();
            stage.showAndWait();
            
            // Reload member from DB to reflect updates
            List<Member> search = memberService.searchMembers(currentMember.getRollNumber());
            if (!search.isEmpty()) {
                // Approximate reload by matching ID
                for (Member m : search) {
                    if (m.getMemberId() == currentMember.getMemberId()) {
                        this.currentMember = m;
                        refreshData();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) nameLabel.getScene().getWindow();
        stage.close();
    }
}
