package com.slms.models;

public class Fine {
    private int fineId;
    private int transactionId;
    private double amount;
    private String status; // PENDING, PAID
    private String paidDate;

    public Fine() {}

    private String memberName;
    private String bookTitle;
    private String dueDate;
    private int daysOverdue;

    public Fine(int fineId, int transactionId, double amount, String status, String paidDate) {
        this.fineId = fineId;
        this.transactionId = transactionId;
        this.amount = amount;
        this.status = status;
        this.paidDate = paidDate;
    }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public int getDaysOverdue() { return daysOverdue; }
    public void setDaysOverdue(int daysOverdue) { this.daysOverdue = daysOverdue; }

    public int getFineId() { return fineId; }
    public void setFineId(int fineId) { this.fineId = fineId; }

    public int getTransactionId() { return transactionId; }
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaidDate() { return paidDate; }
    public void setPaidDate(String paidDate) { this.paidDate = paidDate; }
}
