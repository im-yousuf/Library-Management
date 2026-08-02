package com.slms.controllers;

import com.slms.database.DatabaseManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.FileChooser;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class ReportsController {

    @FXML
    public void generateInventoryReport() {
        String sql = "SELECT title, author, isbn, category_name, quantity, available_copies, status FROM books " +
                     "LEFT JOIN categories ON books.category_id = categories.category_id";
        exportToExcel("Inventory_Report", sql, new String[]{"Title", "Author", "ISBN", "Category", "Quantity", "Available", "Status"});
    }

    @FXML
    public void generateFinesReport() {
        String sql = "SELECT members.name, books.title, fines.amount, fines.status, fines.paid_date FROM fines " +
                     "JOIN transactions ON fines.transaction_id = transactions.transaction_id " +
                     "JOIN members ON transactions.member_id = members.member_id " +
                     "JOIN books ON transactions.book_id = books.book_id";
        exportToExcel("Fines_Report", sql, new String[]{"Member", "Book", "Amount", "Status", "Paid Date"});
    }

    private void exportToExcel(String reportName, String query, String[] headers) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel File", "*.xlsx"));
        fileChooser.setInitialFileName(reportName + ".xlsx");

        File file = fileChooser.showSaveDialog(null);
        if (file == null) return;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query);
             Workbook workbook = new XSSFWorkbook()) {

            Sheet sheet = workbook.createSheet(reportName);
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }

            int rowNum = 1;
            int colCount = headers.length;
            while (rs.next()) {
                Row row = sheet.createRow(rowNum++);
                for (int i = 0; i < colCount; i++) {
                    String val = rs.getString(i + 1);
                    row.createCell(i).setCellValue(val != null ? val : "");
                }
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
            new Alert(Alert.AlertType.INFORMATION, "Report saved successfully!", ButtonType.OK).showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, "Failed to generate report: " + e.getMessage(), ButtonType.OK).showAndWait();
        }
    }
}
