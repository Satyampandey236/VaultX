package com.vaultx;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.vaultx.db.DatabaseManager;
import com.vaultx.exception.VaultXException;
import com.vaultx.model.Account;
import com.vaultx.model.Transaction;
import com.vaultx.model.User;
import com.vaultx.service.BankService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

public class VaultXServer {

    private static BankService bankService;

    public static void main(String[] args) throws Exception {
        bankService = new BankService();

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/api/register", new RegisterHandler());
        server.createContext("/api/login", new LoginHandler());
        server.createContext("/api/deposit", new DepositHandler());
        server.createContext("/api/withdraw", new WithdrawHandler());
        server.createContext("/api/transfer", new TransferHandler());
        server.createContext("/api/balance", new BalanceHandler());
        server.createContext("/api/transactions", new TransactionsHandler());
        server.createContext("/api/admin/accounts", new AdminAccountsHandler());
        server.createContext("/api/admin/transactions", new AdminTransactionsHandler());
        server.createContext("/api/admin/freeze", new AdminFreezeHandler());
        server.createContext("/api/admin/delete", new AdminDeleteHandler());

        server.setExecutor(null); 
        server.start();
        System.out.println("VaultX Server started on port 8080");
    }

    // --- Utility Methods ---
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        exchange.getResponseHeaders().add("Content-Type", "application/json");

        if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        byte[] bytes = response.getBytes("UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(bytes);
        os.close();
    }
    
    // Very simple JSON parser for flat string-valued or numeric-valued objects 
    private static String getJsonStr(String json, String key) {
        if (json == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^\"},]+)\"?").matcher(json);
        return m.find() ? m.group(1).trim() : null;
    }
    
    private static String escape(String in) { 
        if (in == null) return "";
        return in.replace("\"", "\\\""); 
    }

    // --- Handlers ---

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(t, 204, ""); return; }
            try (InputStream is = t.getRequestBody()) {
                String body = new String(is.readAllBytes());
                String fullName = getJsonStr(body, "fullName");
                String email = getJsonStr(body, "email");
                String phone = getJsonStr(body, "phone");
                String password = getJsonStr(body, "password");
                String accountType = getJsonStr(body, "accountType");

                Account acc = bankService.register(fullName, email, phone, password, accountType);
                sendResponse(t, 200, "{\"success\":true,\"message\":\"Account registered successfully.\",\"accountNumber\":\"" + acc.getAccountNumber() + "\"}");
            } catch (VaultXException e) {
                sendResponse(t, 400, "{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(t, 500, "{\"success\":false,\"message\":\"Internal Server Error\"}");
            }
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(t, 204, ""); return; }
            try (InputStream is = t.getRequestBody()) {
                String body = new String(is.readAllBytes());
                String email = getJsonStr(body, "email");
                String password = getJsonStr(body, "password");

                User user = bankService.login(email, password);
                Account acc = bankService.getAccountByUserId(user.getId());
                
                String accountData = "";
                if(acc != null) {
                    accountData = ",\"accountNumber\":\"" + acc.getAccountNumber() + "\",\"accountType\":\"" + acc.getAccountType() + "\",\"balance\":" + acc.getBalance() + ",\"accountStatus\":\"" + (acc.isFrozen() ? "FROZEN" : "ACTIVE") + "\"";
                }

                sendResponse(t, 200, "{\"success\":true,\"userId\":" + user.getId() + ",\"fullName\":\"" + user.getFullName() + "\",\"email\":\"" + user.getEmail() + "\",\"phone\":\"" + user.getPhone() + "\",\"role\":\"" + user.getRole() + "\"" + accountData + "}");
            } catch (com.vaultx.exception.AuthException e) {
                sendResponse(t, 401, "{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
            } catch (VaultXException e) {
                sendResponse(t, 400, "{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(t, 500, "{\"success\":false,\"message\":\"Internal Server Error\"}");
            }
        }
    }

    static class DepositHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(t, 204, ""); return; }
            try (InputStream is = t.getRequestBody()) {
                String body = new String(is.readAllBytes());
                String accNum = getJsonStr(body, "accountNumber");
                double amount = Double.parseDouble(getJsonStr(body, "amount"));
                String desc = getJsonStr(body, "description");

                Account acc = bankService.deposit(accNum, amount, desc);
                sendResponse(t, 200, "{\"success\":true,\"message\":\"Deposit successful.\",\"newBalance\":" + acc.getBalance() + ",\"txnId\":\"TXN-OK\"}");
            } catch (VaultXException e) {
                sendResponse(t, 400, "{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    static class WithdrawHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(t, 204, ""); return; }
            try (InputStream is = t.getRequestBody()) {
                String body = new String(is.readAllBytes());
                String accNum = getJsonStr(body, "accountNumber");
                double amount = Double.parseDouble(getJsonStr(body, "amount"));
                String desc = getJsonStr(body, "description");

                Account acc = bankService.withdraw(accNum, amount, desc);
                sendResponse(t, 200, "{\"success\":true,\"message\":\"Withdrawal successful.\",\"newBalance\":" + acc.getBalance() + ",\"txnId\":\"TXN-OK\"}");
            } catch (VaultXException e) {
                sendResponse(t, 400, "{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    static class TransferHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(t, 204, ""); return; }
            try (InputStream is = t.getRequestBody()) {
                String body = new String(is.readAllBytes());
                String from = getJsonStr(body, "fromAccount");
                String to = getJsonStr(body, "toAccount");
                double amount = Double.parseDouble(getJsonStr(body, "amount"));
                String desc = getJsonStr(body, "description");

                Account acc = bankService.transfer(from, to, amount, desc);
                sendResponse(t, 200, "{\"success\":true,\"message\":\"Transfer successful.\",\"newBalance\":" + acc.getBalance() + ",\"txnId\":\"TXN-OK\"}");
            } catch (VaultXException e) {
                sendResponse(t, 400, "{\"success\":false,\"message\":\"" + escape(e.getMessage()) + "\"}");
            }
        }
    }

    static class BalanceHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(t, 204, ""); return; }
            try (InputStream is = t.getRequestBody()) {
                String body = new String(is.readAllBytes());
                String accNum = getJsonStr(body, "accountNumber");

                Account acc = bankService.getAccountByNumber(accNum);
                if (acc != null) {
                    sendResponse(t, 200, "{\"success\":true,\"balance\":" + acc.getBalance() + ",\"status\":\"" + (acc.isFrozen() ? "FROZEN" : "ACTIVE") + "\"}");
                } else {
                    sendResponse(t, 404, "{\"success\":false,\"message\":\"Account not found\"}");
                }
            } catch (Exception e) {
                sendResponse(t, 400, "{\"success\":false}");
            }
        }
    }

    static class TransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(t, 204, ""); return; }
            try (InputStream is = t.getRequestBody()) {
                String body = new String(is.readAllBytes());
                String accNum = getJsonStr(body, "accountNumber");
                int limit = Integer.parseInt(getJsonStr(body, "limit") == null ? "50" : getJsonStr(body, "limit"));

                List<Transaction> txns = bankService.getTransactions(accNum, limit);
                
                StringBuilder sb = new StringBuilder("[");
                for(int i=0; i<txns.size(); i++) {
                    Transaction tr = txns.get(i);
                    boolean isCredit = tr.getType().equals("DEPOSIT") || tr.getType().equals("TRANSFER_IN");
                    sb.append("{ \"txnId\":\"").append(tr.getTransactionId())
                      .append("\",\"txnDate\":\"").append(tr.getCreatedAt())
                      .append("\",\"txnType\":\"").append(tr.getType())
                      .append("\",\"isCredit\":").append(isCredit)
                      .append(",\"amount\":").append(tr.getAmount())
                      .append(",\"balanceAfter\":").append(tr.getBalanceAfter())
                      .append(",\"description\":\"").append(escape(tr.getDescription()))
                      .append("\" }");
                    if (i < txns.size() - 1) sb.append(",");
                }
                sb.append("]");
                
                sendResponse(t, 200, "{\"success\":true,\"transactions\":" + sb.toString() + "}");
            } catch (Exception e) {
                sendResponse(t, 400, "{\"success\":false,\"message\":\"Error fetching transactions\"}");
            }
        }
    }

    // --- Admin Handlers ---
    
    static class AdminAccountsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(t, 204, ""); return; }
            
            String sql = "SELECT a.account_number, a.account_type, a.balance, a.is_frozen, u.full_name, u.email, a.created_at FROM accounts a JOIN users u ON a.user_id = u.id";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                 
                 StringBuilder sb = new StringBuilder("[");
                 boolean first = true;
                 while(rs.next()) {
                     if(!first) sb.append(",");
                     first = false;
                     sb.append("{ \"fullName\":\"").append(rs.getString("full_name"))
                       .append("\",\"email\":\"").append(rs.getString("email"))
                       .append("\",\"accountNumber\":\"").append(rs.getString("account_number"))
                       .append("\",\"accountType\":\"").append(rs.getString("account_type"))
                       .append("\",\"balance\":").append(rs.getDouble("balance"))
                       .append(",\"status\":\"").append(rs.getBoolean("is_frozen") ? "FROZEN" : "ACTIVE")
                       .append("\",\"createdAt\":\"").append(rs.getString("created_at"))
                       .append("\" }");
                 }
                 sb.append("]");
                 sendResponse(t, 200, "{\"success\":true,\"accounts\":" + sb.toString() + "}");
            } catch (Exception e) {
                sendResponse(t, 500, "{\"success\":false}");
            }
        }
    }

    static class AdminTransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(t, 204, ""); return; }
            
            String sql = "SELECT t.transaction_id, t.type, t.amount, t.description, t.created_at, a.account_number, u.full_name " +
                         "FROM transactions t JOIN accounts a ON t.account_id = a.id JOIN users u ON a.user_id = u.id " +
                         "ORDER BY t.created_at DESC LIMIT 100";
            try (Connection conn = DatabaseManager.getInstance().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                 
                 StringBuilder sb = new StringBuilder("[");
                 boolean first = true;
                 while(rs.next()) {
                     if(!first) sb.append(",");
                     first = false;
                     boolean isCredit = rs.getString("type").equals("DEPOSIT") || rs.getString("type").equals("TRANSFER_IN");
                     
                     sb.append("{ \"txnId\":\"").append(rs.getString("transaction_id"))
                       .append("\",\"fullName\":\"").append(rs.getString("full_name"))
                       .append("\",\"accountNumber\":\"").append(rs.getString("account_number"))
                       .append("\",\"txnDate\":\"").append(rs.getString("created_at"))
                       .append("\",\"isCredit\":").append(isCredit)
                       .append(",\"amount\":").append(rs.getDouble("amount"))
                       .append(",\"description\":\"").append(escape(rs.getString("description")))
                       .append("\" }");
                 }
                 sb.append("]");
                 sendResponse(t, 200, "{\"success\":true,\"transactions\":" + sb.toString() + "}");
            } catch (Exception e) {
                sendResponse(t, 500, "{\"success\":false}");
            }
        }
    }
    
    static class AdminFreezeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(t, 204, ""); return; }
            try (InputStream is = t.getRequestBody()) {
                String body = new String(is.readAllBytes());
                String accNum = getJsonStr(body, "accountNumber");
                boolean actionFreeze = getJsonStr(body, "action").equalsIgnoreCase("freeze");
                
                String sql = "UPDATE accounts SET is_frozen = ? WHERE account_number = ?";
                try (PreparedStatement pstmt = DatabaseManager.getInstance().getConnection().prepareStatement(sql)) {
                    pstmt.setBoolean(1, actionFreeze);
                    pstmt.setString(2, accNum);
                    pstmt.executeUpdate();
                    sendResponse(t, 200, "{\"success\":true,\"message\":\"Account updated\"}");
                }
            } catch (Exception e) {
                sendResponse(t, 500, "{\"success\":false}");
            }
        }
    }

    static class AdminDeleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) { sendResponse(t, 204, ""); return; }
            try (InputStream is = t.getRequestBody()) {
                String body = new String(is.readAllBytes());
                String accNum = getJsonStr(body, "accountNumber");
                
                String accCheck = "SELECT user_id FROM accounts WHERE account_number = ?";
                try (Connection conn = DatabaseManager.getInstance().getConnection();
                     PreparedStatement pstmtCheck = conn.prepareStatement(accCheck)) {
                     pstmtCheck.setString(1, accNum);
                     ResultSet rs = pstmtCheck.executeQuery();
                     if (rs.next()) {
                         int userId = rs.getInt(1);
                         
                         // Due to ON DELETE CASCADE, deleting User deletes account and transactions
                         String delSql = "DELETE FROM users WHERE id = ?";
                         try (PreparedStatement pstmtDel = conn.prepareStatement(delSql)) {
                             pstmtDel.setInt(1, userId);
                             pstmtDel.executeUpdate();
                             sendResponse(t, 200, "{\"success\":true,\"message\":\"Account deleted\"}");
                         }
                     } else {
                         sendResponse(t, 404, "{\"success\":false,\"message\":\"Account not found\"}");
                     }
                }
            } catch (Exception e) {
                sendResponse(t, 500, "{\"success\":false}");
            }
        }
    }
}
