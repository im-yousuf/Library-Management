package com.slms.services;

import com.slms.models.Book;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public class ExcelService {

    public static void exportBooks(List<Book> books, File file) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Books");
            Row header = sheet.createRow(0);
            
            String[] columns = {"Title", "Author", "ISBN", "Category", "Location", "Copies", "Status", "Publisher", "Edition", "Language", "Publication Year", "Price"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columns[i]);
            }

            int rowNum = 1;
            for (Book book : books) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(book.getTitle() != null ? book.getTitle() : "");
                row.createCell(1).setCellValue(book.getAuthor() != null ? book.getAuthor() : "");
                row.createCell(2).setCellValue(book.getIsbn() != null ? book.getIsbn() : "");
                row.createCell(3).setCellValue(book.getCategoryName() != null ? book.getCategoryName() : "");
                row.createCell(4).setCellValue(book.getPosition() != null ? book.getPosition() : "");
                row.createCell(5).setCellValue(book.getQuantity());
                row.createCell(6).setCellValue(book.getStatus() != null ? book.getStatus() : "");
                row.createCell(7).setCellValue(book.getPublisher() != null ? book.getPublisher() : "");
                row.createCell(8).setCellValue(book.getEdition() != null ? book.getEdition() : "");
                row.createCell(9).setCellValue(book.getLanguage() != null ? book.getLanguage() : "");
                if (book.getPublicationYear() != null) {
                    row.createCell(10).setCellValue(book.getPublicationYear());
                }
                row.createCell(11).setCellValue(book.getPrice());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fos = new FileOutputStream(file)) {
                workbook.write(fos);
            }
        }
    }

    public static List<Book> importBooks(File file) throws Exception {
        List<Book> books = new ArrayList<>();
        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {
             
            Sheet sheet = workbook.getSheetAt(0);
            boolean firstRow = true;
            for (Row row : sheet) {
                if (firstRow) {
                    firstRow = false;
                    continue;
                }
                
                Book book = new Book();
                // Mandatory fields: Title, Shelf/Position
                Cell titleCell = row.getCell(0);
                if (titleCell == null || titleCell.getCellType() == CellType.BLANK) {
                    continue; // Title is mandatory, skip if blank
                }
                book.setTitle(getCellValue(titleCell));
                
                Cell authorCell = row.getCell(1);
                if (authorCell != null) book.setAuthor(getCellValue(authorCell));
                
                Cell isbnCell = row.getCell(2);
                if (isbnCell != null) book.setIsbn(getCellValue(isbnCell));
                
                Cell positionCell = row.getCell(4); // We just use Location for Position text temporarily or Shelf
                if (positionCell != null) book.setPosition(getCellValue(positionCell));
                
                Cell copiesCell = row.getCell(5);
                if (copiesCell != null && copiesCell.getCellType() == CellType.NUMERIC) {
                    int qty = (int) copiesCell.getNumericCellValue();
                    book.setQuantity(qty);
                    book.setAvailableCopies(qty);
                } else {
                    book.setQuantity(1);
                    book.setAvailableCopies(1);
                }
                
                Cell statusCell = row.getCell(6);
                if (statusCell != null) book.setStatus(getCellValue(statusCell));
                else book.setStatus("AVAILABLE");
                
                books.add(book);
            }
        }
        return books;
    }

    private static String getCellValue(Cell cell) {
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf(cell.getNumericCellValue());
        }
        return "";
    }
}
