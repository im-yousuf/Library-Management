package com.slms.models;

public class Transaction {
    private int transactionId;
    private int bookId;
    private String bookTitle; // joined
    private int memberId;
    private String issueDate;
    private String dueDate;
    private String returnDate;
    private String status;
    private int issuedBy;
    private double fineAmount; // computed or joined

    public Transaction() {}

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public int getMemberId() { return memberId; }
    public void setMemberId(int memberId) { this.memberId = memberId; }

    public String getIssueDate() { return issueDate; }
    public void setIssueDate(String issueDate) { this.issueDate = issueDate; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getReturnDate() { return returnDate; }
    public void setReturnDate(String returnDate) { this.returnDate = returnDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getIssuedBy() { return issuedBy; }
    public void setIssuedBy(int issuedBy) { this.issuedBy = issuedBy; }

    public double getFineAmount() { return fineAmount; }
    public void setFineAmount(double fineAmount) { this.fineAmount = fineAmount; }

    // Helper for table view
    public String getDaysStatus() {
        if ("RETURNED".equals(status)) return "Returned";
        if (dueDate == null) return "-";
        
        try {
            java.time.LocalDate due = java.time.LocalDate.parse(dueDate);
            long days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), due);
            if (days < 0) return Math.abs(days) + " days overdue";
            if (days == 0) return "Due today";
            return days + " days left";
        } catch (Exception e) {
            return dueDate;
        }
    }
}
