package com.vaultx.model;

/**
 * Concrete class demonstrating Inheritance.
 */
public class BankUser extends User {

    public BankUser(int id, String fullName, String email, String phone, String passwordHash, boolean isActive) {
        super(id, fullName, email, phone, passwordHash, "USER", isActive);
    }

    @Override
    public String getDashboardType() {
        return "USER_DASHBOARD";
    }

    @Override
    public String getWelcomeMessage() {
        return "Welcome to VaultX, " + getFullName() + "!";
    }
}
