package com.slms.controllers;

import com.slms.services.DashboardService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class DashboardController {


    @FXML private Label totalBooksValue;
    @FXML private Label booksIssuedValue;
    @FXML private Label availableBooksValue;
    @FXML private Label totalMembersValue;
    @FXML private Label overdueBooksValue;
    @FXML private Label lostBooksValue;

    private final DashboardService dashboardService = new DashboardService();

    @FXML
    public void initialize() {
        loadStats();
    }

    private void loadStats() {
        DashboardService.DashboardStats stats = dashboardService.getStats();
        totalBooksValue.setText(String.valueOf(stats.totalBooks()));
        booksIssuedValue.setText(String.valueOf(stats.booksIssued()));
        availableBooksValue.setText(String.valueOf(stats.availableBooks()));
        totalMembersValue.setText(String.valueOf(stats.totalMembers()));
        overdueBooksValue.setText(String.valueOf(stats.overdueBooks()));
        lostBooksValue.setText(String.valueOf(stats.lostBooks()));
    }

}
