package com.vaultx.service;

import com.vaultx.db.DatabaseManager;
import com.vaultx.exception.AccountFrozenException;
import com.vaultx.exception.AuthException;
import com.vaultx.exception.InsufficientFundsException;
import com.vaultx.exception.VaultXException;
import com.vaultx.model.Account;
import com.vaultx.model.Admin;
import com.vaultx.model.BankUser;
import com.vaultx.model.Transaction;
import com.vaultx.model.User;
import com.vaultx.util.SecurityUtil;
import com.vaultx.util.TransactionLogUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class BankService {
    private DatabaseManager dbManager;

    public BankService() {
        this.dbManager = DatabaseManager.getInstance();
    }

    public Account register(String fullName, String email, String phone, String password, String accountType) throws VaultXException {
        String hash = SecurityUtil.hashPassword(password);
        Connection conn = dbManager.getConnection();
        try {
            conn.setAutoCommit(false);
            
            // 1. Create User
            String userSql = "INSERT INTO users (full_name, email, phone, password_hash, role) VALUES (?, ?, ?, ?, 'USER')";
            try (PreparedStatement pstmt = conn.prepareStatement(userSql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, fullName);
                pstmt.setString(2, email);
                pstmt.setString(3, phone);
                pstmt.setString(4, hash);
                pstmt.executeUpdate();
                
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int userId = rs.getInt(1);
                        
                        // 2. Create Account
                        String accNum = "VX" + (10000000 + userId);
                        String accSql = "INSERT INTO accounts (account_number, user_id, account_type, balance) VALUES (?, ?, ?, 0.0)";
                        try (PreparedStatement pstmt2 = conn.prepareStatement(accSql, Statement.RETURN_GENERATED_KEYS)) {
                            pstmt2.setString(1, accNum);
                            pstmt2.setInt(2, userId);
                            pstmt2.setString(3, accountType.toUpperCase());
                            pstmt2.executeUpdate();
                            
                            try(ResultSet rs2 = pstmt2.getGeneratedKeys()) {
                                if(rs2.next()) {
                                    int accId = rs2.getInt(1);
                                    conn.commit();
                                    TransactionLogUtil.logSystem("User registered: " + email + " with account " + accNum);
                                    return new Account(accId, accNum, userId, accountType, 0.0, false);
                                }
                            }
                        }
                    }
                }
            }
            conn.rollback();
            throw new VaultXException("Failed to register user.");
        } catch (SQLException e) {
            try { conn.rollback(); } catch(SQLException ex){}
            if (e.getMessage().contains("Duplicate")) {
                throw new VaultXException("Email or Phone already registered.");
            }
            throw new VaultXException("Database error during registration: " + e.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch(SQLException ex){}
        }
    }

    public User login(String email, String password) throws VaultXException {
        String hash = SecurityUtil.hashPassword(password);
        String sql = "SELECT * FROM users WHERE email = ? AND password_hash = ? AND is_active = TRUE";

        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, hash);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id");
                    String fullName = rs.getString("full_name");
                    String phone = rs.getString("phone");
                    String role = rs.getString("role");
                    boolean isActive = rs.getBoolean("is_active");
                    
                    TransactionLogUtil.logSystem("User logged in: " + email);
                    
                    if ("ADMIN".equals(role)) {
                        return new Admin(id, fullName, email, phone, hash, isActive);
                    } else {
                        return new BankUser(id, fullName, email, phone, hash, isActive);
                    }
                }
            }
        } catch (SQLException e) {
            throw new VaultXException("Database error during login.");
        }
        throw new AuthException("Invalid email or password, or account is disabled.");
    }

    public Account getAccountByUserId(int userId) throws VaultXException {
        String sql = "SELECT * FROM accounts WHERE user_id = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                            rs.getInt("id"),
                            rs.getString("account_number"),
                            rs.getInt("user_id"),
                            rs.getString("account_type"),
                            rs.getDouble("balance"),
                            rs.getBoolean("is_frozen")
                    );
                }
            }
        } catch (SQLException e) {
            throw new VaultXException("Error fetching account details.");
        }
        return null;
    }
    
    public Account getAccountByNumber(String accNum) throws VaultXException {
        String sql = "SELECT * FROM accounts WHERE account_number = ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setString(1, accNum);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Account(
                            rs.getInt("id"),
                            rs.getString("account_number"),
                            rs.getInt("user_id"),
                            rs.getString("account_type"),
                            rs.getDouble("balance"),
                            rs.getBoolean("is_frozen")
                    );
                }
            }
        } catch (SQLException e) {
            throw new VaultXException("Error fetching account details.");
        }
        return null;
    }

    public Account deposit(String accountNumber, double amount, String description) throws VaultXException {
        if (amount <= 0) throw new VaultXException("Amount must be greater than zero.");
        Account acc = getAccountByNumber(accountNumber);
        if (acc == null) throw new VaultXException("Account not found.");
        if (acc.isFrozen()) throw new AccountFrozenException("Account is frozen. Cannot deposit.");

        Connection conn = dbManager.getConnection();
        try {
            conn.setAutoCommit(false);
            
            // 1. Update Balance
            double nextBal = acc.getBalance() + amount;
            String updateSql = "UPDATE accounts SET balance = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setDouble(1, nextBal);
                pstmt.setInt(2, acc.getId());
                pstmt.executeUpdate();
            }
            
            // 2. Insert Transaction
            String txnId = SecurityUtil.generateTransactionId();
            insertTransaction(conn, txnId, acc.getId(), "DEPOSIT", amount, nextBal, description, null, "SUCCESS");
            
            conn.commit();
            TransactionLogUtil.logTransaction(txnId + " - DEPOSIT - " + accountNumber + " - " + amount);
            acc.setBalance(nextBal);
            return acc;
        } catch (SQLException e) {
            try { conn.rollback(); } catch(SQLException ex){}
            throw new VaultXException("Deposit failed: " + e.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch(SQLException ex){}
        }
    }

    public Account withdraw(String accountNumber, double amount, String description) throws VaultXException {
        if (amount <= 0) throw new VaultXException("Amount must be greater than zero.");
        Account acc = getAccountByNumber(accountNumber);
        if (acc == null) throw new VaultXException("Account not found.");
        if (acc.isFrozen()) throw new AccountFrozenException("Account is frozen. Cannot withdraw.");
        if (acc.getBalance() < amount) throw new InsufficientFundsException("Insufficient funds.");

        Connection conn = dbManager.getConnection();
        try {
            conn.setAutoCommit(false);
            
            double nextBal = acc.getBalance() - amount;
            String updateSql = "UPDATE accounts SET balance = ? WHERE id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setDouble(1, nextBal);
                pstmt.setInt(2, acc.getId());
                pstmt.executeUpdate();
            }
            
            String txnId = SecurityUtil.generateTransactionId();
            insertTransaction(conn, txnId, acc.getId(), "WITHDRAWAL", amount, nextBal, description, null, "SUCCESS");
            
            conn.commit();
            TransactionLogUtil.logTransaction(txnId + " - WITHDRAWAL - " + accountNumber + " - " + amount);
            acc.setBalance(nextBal);
            return acc;
        } catch (SQLException e) {
            try { conn.rollback(); } catch(SQLException ex){}
            throw new VaultXException("Withdraw failed: " + e.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch(SQLException ex){}
        }
    }

    public Account transfer(String fromAccount, String toAccount, double amount, String description) throws VaultXException {
        if (amount <= 0) throw new VaultXException("Amount must be greater than zero.");
        if (fromAccount.equals(toAccount)) throw new VaultXException("Cannot transfer to the same account.");
        
        Account accFrom = getAccountByNumber(fromAccount);
        if (accFrom == null) throw new VaultXException("Source account not found.");
        if (accFrom.isFrozen()) throw new AccountFrozenException("Source account is frozen. Cannot transfer.");
        if (accFrom.getBalance() < amount) throw new InsufficientFundsException("Insufficient funds for transfer.");

        Account accTo = getAccountByNumber(toAccount);
        if (accTo == null) throw new VaultXException("Destination account not found.");
        if (accTo.isFrozen()) throw new AccountFrozenException("Destination account is frozen. Cannot receive transfer.");

        Connection conn = dbManager.getConnection();
        try {
            conn.setAutoCommit(false);
            
            // Deduct from Source
            double nextBalFrom = accFrom.getBalance() - amount;
            String updFrom = "UPDATE accounts SET balance = ? WHERE id = ?";
            try (PreparedStatement pFrom = conn.prepareStatement(updFrom)) {
                pFrom.setDouble(1, nextBalFrom);
                pFrom.setInt(2, accFrom.getId());
                pFrom.executeUpdate();
            }
            
            // Add to Dest
            double nextBalTo = accTo.getBalance() + amount;
            String updTo = "UPDATE accounts SET balance = ? WHERE id = ?";
            try (PreparedStatement pTo = conn.prepareStatement(updTo)) {
                pTo.setDouble(1, nextBalTo);
                pTo.setInt(2, accTo.getId());
                pTo.executeUpdate();
            }
            
            String txnId = SecurityUtil.generateTransactionId();
            // Source Txn Rec
            insertTransaction(conn, txnId, accFrom.getId(), "TRANSFER_OUT", amount, nextBalFrom, description, toAccount, "SUCCESS");
            
            // Dest Txn Rec
            insertTransaction(conn, txnId+"-IN", accTo.getId(), "TRANSFER_IN", amount, nextBalTo, "Received from " + fromAccount + (description.isEmpty() ? "" : ": " + description), fromAccount, "SUCCESS");
            
            conn.commit();
            TransactionLogUtil.logTransaction(txnId + " - TRANSFER - " + fromAccount + " -> " + toAccount + " - " + amount);
            accFrom.setBalance(nextBalFrom);
            return accFrom;
        } catch (SQLException e) {
            try { conn.rollback(); } catch(SQLException ex){}
            throw new VaultXException("Transfer failed: " + e.getMessage());
        } finally {
            try { conn.setAutoCommit(true); } catch(SQLException ex){}
        }
    }

    private void insertTransaction(Connection conn, String txnId, int accountId, String type, double amount, double balanceAfter, String desc, String targetAcc, String status) throws SQLException {
        String sql = "INSERT INTO transactions (transaction_id, account_id, type, amount, balance_after, description, target_account, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, txnId);
            pstmt.setInt(2, accountId);
            pstmt.setString(3, type);
            pstmt.setDouble(4, amount);
            pstmt.setDouble(5, balanceAfter);
            pstmt.setString(6, desc);
            pstmt.setString(7, targetAcc);
            pstmt.setString(8, status);
            pstmt.executeUpdate();
        }
    }

    public List<Transaction> getTransactions(String accountNumber, int limit) throws VaultXException {
        List<Transaction> list = new ArrayList<>();
        Account acc = getAccountByNumber(accountNumber);
        if (acc == null) return list;
        
        String sql = "SELECT * FROM transactions WHERE account_id = ? ORDER BY created_at DESC LIMIT ?";
        try (PreparedStatement pstmt = dbManager.getConnection().prepareStatement(sql)) {
            pstmt.setInt(1, acc.getId());
            pstmt.setInt(2, limit > 0 ? limit : 50);
            try(ResultSet rs = pstmt.executeQuery()) {
                while(rs.next()) {
                    Transaction t = new Transaction();
                    t.setId(rs.getInt("id"));
                    t.setTransactionId(rs.getString("transaction_id"));
                    t.setAccountId(rs.getInt("account_id"));
                    t.setType(rs.getString("type"));
                    t.setAmount(rs.getDouble("amount"));
                    t.setBalanceAfter(rs.getDouble("balance_after"));
                    t.setDescription(rs.getString("description"));
                    t.setTargetAccount(rs.getString("target_account"));
                    t.setStatus(rs.getString("status"));
                    t.setCreatedAt(rs.getString("created_at"));
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            throw new VaultXException("Error fetching transactions: " + e.getMessage());
        }
        return list;
    }
    
    // --- ADMIN METHODS ---
    
    public List<Account> getAllAccounts() throws VaultXException {
        List<Account> list = new ArrayList<>();
        // we'll also want FULL NAME and EMAIL so we do a join, but returning a specialized object or just injecting to Account would be fine. 
        // to simplify based on frontend JS, we'll return more info via the REST layer, but here we can just do basic Accounts and join in REST, or create a DTO.
        // Let's create a custom query in the HTTP server that maps both.
        return list; 
    }
    
    // Admin raw query - passing result directly to JSON in HTTP server for speed
}
