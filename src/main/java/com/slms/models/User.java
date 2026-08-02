package com.slms.models;

public class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String role; // ADMIN or LIBRARIAN
    private boolean active;

    public User(int userId, String username, String passwordHash, String fullName, String role, boolean active) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.active = active;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getRole() { return role; }
    public boolean isActive() { return active; }

    public boolean isAdmin() { return "ADMIN".equalsIgnoreCase(role); }
}
