package com.vaultx.model;

/**
 * Abstract Base Class demonstrating Abstraction.
 */
public abstract class User {
    private int id;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private String role;
    private boolean isActive;

    public User(int id, String fullName, String email, String phone, String passwordHash, String role, boolean isActive) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.role = role;
        this.isActive = isActive;
    }

    public abstract String getDashboardType();
    public abstract String getWelcomeMessage();

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
