package com.slms.controllers;

import com.slms.models.Member;
import com.slms.services.AuditService;
import com.slms.services.MemberService;
import com.slms.utils.SceneManager;
import com.slms.utils.Session;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;
import java.util.Optional;

public class MembersController {

    @FXML private TextField searchField;
    @FXML private TableView<Member> membersTable;
    @FXML private TableColumn<Member, Member> photoCol;
    @FXML private TableColumn<Member, String> nameCol;
    @FXML private TableColumn<Member, String> rollCol;
    @FXML private TableColumn<Member, String> deptCol;
    @FXML private TableColumn<Member, String> phoneCol;
    @FXML private TableColumn<Member, String> statusCol;
    @FXML private TableColumn<Member, Void> actionsCol;

    private final MemberService memberService = new MemberService();
    private ObservableList<Member> membersList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupColumns();
        loadMembers("");

        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            loadMembers(newVal);
        });
    }

    private void setupColumns() {
        photoCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        photoCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            {
                imageView.setFitWidth(40);
                imageView.setFitHeight(40);
                imageView.setPreserveRatio(true);
            }
            @Override
            protected void updateItem(Member member, boolean empty) {
                super.updateItem(member, empty);
                if (empty || member == null) {
                    setGraphic(null);
                } else {
                    if (member.getPhotoPath() != null && !member.getPhotoPath().isEmpty()) {
                        File f = new File(member.getPhotoPath());
                        if (f.exists()) {
                            imageView.setImage(new Image(f.toURI().toString()));
                        } else {
                            imageView.setImage(null);
                        }
                    } else {
                        imageView.setImage(null);
                    }
                    setGraphic(imageView);
                }
            }
        });

        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        rollCol.setCellValueFactory(new PropertyValueFactory<>("rollNumber"));
        deptCol.setCellValueFactory(new PropertyValueFactory<>("deptCourse"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        actionsCol.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("View");
            private final Button editBtn = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox pane = new HBox(5, viewBtn, editBtn, deleteBtn);

            {
                viewBtn.getStyleClass().add("nav-button");
                editBtn.getStyleClass().add("nav-button");
                deleteBtn.getStyleClass().add("logout-button");

                viewBtn.setOnAction(e -> {
                    Member member = getTableView().getItems().get(getIndex());
                    openMemberDetail(member);
                });

                editBtn.setOnAction(e -> {
                    Member member = getTableView().getItems().get(getIndex());
                    openMemberForm(member);
                });

                deleteBtn.setOnAction(e -> {
                    Member member = getTableView().getItems().get(getIndex());
                    handleDelete(member);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadMembers(String query) {
        List<Member> list = memberService.searchMembers(query);
        membersList.setAll(list);
        membersTable.setItems(membersList);
    }

    @FXML
    private void handleAddMember() {
        openMemberForm(null);
    }

    private void openMemberForm(Member member) {
        try {
            FXMLLoader loader = SceneManager.showModal("/views/member-form.fxml", member == null ? "Add Member" : "Edit Member");
            Parent root = loader.getRoot();
            
            MemberFormController controller = loader.getController();
            controller.setMember(member);
            
            Stage stage = (Stage) root.getScene().getWindow();
            stage.showAndWait();
            
            loadMembers(searchField.getText());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open member form.");
        }
    }
    
    private void openMemberDetail(Member member) {
        try {
            FXMLLoader loader = SceneManager.showModal("/views/member-detail.fxml", "Member Detail - " + member.getName());
            Parent root = loader.getRoot();
            
            MemberDetailController controller = loader.getController();
            controller.setMember(member);
            
            Stage stage = (Stage) root.getScene().getWindow();
            stage.showAndWait();
            
            // Reload in case they click 'Edit' from the detail view
            loadMembers(searchField.getText());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to open member detail.");
        }
    }

    private void handleDelete(Member member) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText("Delete Member: " + member.getName());
        confirm.setContentText("Are you sure you want to delete this member? This action cannot be undone.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                if (memberService.deleteMember(member.getMemberId())) {
                    AuditService.log(Session.getCurrentUser().getUserId(), "DELETE_MEMBER", "Deleted member ID " + member.getMemberId());
                    loadMembers(searchField.getText());
                } else {
                    showError("Failed to delete member.");
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
