package com.slms.controllers;

import com.slms.models.Member;
import com.slms.services.AuditService;
import com.slms.services.MemberService;
import com.slms.utils.Session;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class MemberFormController {

    @FXML private Label formTitle;
    @FXML private ImageView photoView;
    @FXML private TextField nameField;
    @FXML private TextField rollField;
    @FXML private TextField phoneField;
    @FXML private TextField emailField;
    @FXML private TextField deptField;
    @FXML private TextField courseField;
    @FXML private TextField semesterField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private DatePicker joiningDatePicker;
    @FXML private TextArea addressArea;

    private final MemberService memberService = new MemberService();
    private Member currentMember;
    private File selectedPhotoFile;

    @FXML
    public void initialize() {
        statusCombo.setItems(FXCollections.observableArrayList("ACTIVE", "INACTIVE"));
        statusCombo.getSelectionModel().select("ACTIVE");
        joiningDatePicker.setValue(LocalDate.now());
    }

    public void setMember(Member member) {
        this.currentMember = member;
        if (member != null) {
            formTitle.setText("Edit Member");
            nameField.setText(member.getName());
            rollField.setText(member.getRollNumber());
            phoneField.setText(member.getPhone());
            emailField.setText(member.getEmail());
            deptField.setText(member.getDepartment());
            courseField.setText(member.getCourse());
            semesterField.setText(member.getSemester());
            statusCombo.getSelectionModel().select(member.getStatus());
            addressArea.setText(member.getAddress());
            
            if (member.getJoiningDate() != null && !member.getJoiningDate().isEmpty()) {
                try {
                    joiningDatePicker.setValue(LocalDate.parse(member.getJoiningDate()));
                } catch (Exception ignored) {}
            }

            if (member.getPhotoPath() != null && !member.getPhotoPath().isEmpty()) {
                File photo = new File(member.getPhotoPath());
                if (photo.exists()) {
                    photoView.setImage(new Image(photo.toURI().toString()));
                    selectedPhotoFile = photo;
                }
            }
        }
    }

    @FXML
    private void handleChoosePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose Member Photo");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        Stage stage = (Stage) nameField.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            selectedPhotoFile = file;
            photoView.setImage(new Image(file.toURI().toString()));
        }
    }

    @FXML
    private void handleSave() {
        if (!validateInput()) {
            return;
        }

        boolean isNew = (currentMember == null);
        if (isNew) {
            currentMember = new Member();
        }

        currentMember.setName(nameField.getText().trim());
        currentMember.setRollNumber(rollField.getText().trim());
        currentMember.setPhone(phoneField.getText().trim());
        currentMember.setEmail(emailField.getText().trim());
        currentMember.setDepartment(deptField.getText().trim());
        currentMember.setCourse(courseField.getText().trim());
        currentMember.setSemester(semesterField.getText().trim());
        currentMember.setStatus(statusCombo.getValue());
        currentMember.setAddress(addressArea.getText().trim());
        
        LocalDate date = joiningDatePicker.getValue();
        currentMember.setJoiningDate(date != null ? date.format(DateTimeFormatter.ISO_LOCAL_DATE) : null);

        // Handle Photo Copy
        if (selectedPhotoFile != null) {
            try {
                File dir = new File("data/images/members");
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                
                // If it's a new file selected (not just displaying the existing path)
                if (currentMember.getPhotoPath() == null || !selectedPhotoFile.getAbsolutePath().equals(new File(currentMember.getPhotoPath()).getAbsolutePath())) {
                    String ext = "";
                    String name = selectedPhotoFile.getName();
                    int lastIdx = name.lastIndexOf('.');
                    if (lastIdx > 0) ext = name.substring(lastIdx);
                    
                    String newName = "member_" + System.currentTimeMillis() + ext;
                    Path dest = Paths.get(dir.getAbsolutePath(), newName);
                    Files.copy(selectedPhotoFile.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                    currentMember.setPhotoPath(dest.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                showError("Failed to save photo.");
                return;
            }
        }

        boolean success = isNew ? memberService.addMember(currentMember) : memberService.updateMember(currentMember);

        if (success) {
            String action = isNew ? "ADD_MEMBER" : "EDIT_MEMBER";
            String details = (isNew ? "Added" : "Edited") + " member: " + currentMember.getName();
            AuditService.log(Session.getCurrentUser().getUserId(), action, details);
            closeStage();
        } else {
            showError("Failed to save member.");
        }
    }

    private boolean validateInput() {
        if (nameField.getText().trim().isEmpty()) {
            showError("Name is required.");
            return false;
        }

        if (rollField.getText().trim().isEmpty()) {
            showError("Roll Number is required.");
            return false;
        }

        String phone = phoneField.getText().trim();
        if (!phone.isEmpty() && !phone.matches("^[0-9\\-\\+ ]+$")) {
            showError("Phone format is invalid.");
            return false;
        }

        String email = emailField.getText().trim();
        if (!email.isEmpty() && !email.matches("^[^@]+@[^@]+\\.[^@]+$")) {
            showError("Email format is invalid.");
            return false;
        }

        return true;
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private void closeStage() {
        Stage stage = (Stage) nameField.getScene().getWindow();
        stage.close();
    }

    private void showError(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        alert.setHeaderText("Validation Error");
        alert.showAndWait();
    }
}
