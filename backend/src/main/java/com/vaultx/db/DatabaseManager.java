package com.vaultx.db;

import com.vaultx.util.TransactionLogUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Singleton class for managing database connection.
 */
public class DatabaseManager {
    private static final String URL = "jdbc:mysql://localhost:3306/vaultx_db";
    private static final String USER = "root";
    private static final String PASSWORD = "satyam@12345"; // Assuming common local setup or change it as per your env

    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            // Ensure driver is loaded
            Class.forName("com.mysql.cj.jdbc.Driver");
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            TransactionLogUtil.logSystem("Database connection established.");
        } catch (ClassNotFoundException e) {
            System.err.println("JDBC Driver not found: " + e.getMessage());
            TransactionLogUtil.logSystem("ERROR: JDBC Driver not found.");
        } catch (SQLException e) {
            System.err.println("Failed to connect to MySQL database: " + e.getMessage());
            TransactionLogUtil.logSystem("ERROR: DB Connection failed - " + e.getMessage());
        }
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        } else {
            try {
                if (instance.getConnection().isClosed()) {
                    instance = new DatabaseManager();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                TransactionLogUtil.logSystem("Database connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            TransactionLogUtil.logSystem("ERROR closing DB connection: " + e.getMessage());
        }
    }
}
