package com.vaultx.model;

/**
 * Concrete class demonstrating Inheritance and Polymorphism.
 */
public class Admin extends User {

    public Admin(int id, String fullName, String email, String phone, String passwordHash, boolean isActive) {
        super(id, fullName, email, phone, passwordHash, "ADMIN", isActive);
    }

    @Override
    public String getDashboardType() {
        return "ADMIN_DASHBOARD";
    }

    @Override
    public String getWelcomeMessage() {
        return "VaultX Control Panel - Admin Access Granted.";
    }
}
