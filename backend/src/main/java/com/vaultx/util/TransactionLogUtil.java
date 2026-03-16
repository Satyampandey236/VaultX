package com.vaultx.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TransactionLogUtil {
    private static final String LOG_DIR = "logs/";

    // Ensure directory exists
    static {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    public static void logTransaction(String message) {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String fileName = LOG_DIR + "transactions_" + date + ".log";
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String entry = "[" + time + "] " + message + "\n";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(entry);
        } catch (IOException e) {
            System.err.println("Failed to write to transaction log: " + e.getMessage());
        }
    }

    public static void logSystem(String message) {
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String fileName = LOG_DIR + "system_" + date + ".log";
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String entry = "[" + time + "] " + message + "\n";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            writer.write(entry);
        } catch (IOException e) {
            System.err.println("Failed to write to system log: " + e.getMessage());
        }
    }
}
