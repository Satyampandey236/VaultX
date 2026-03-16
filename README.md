# 🏦 VaultX — Banking Management System

## Complete Project Structure

```
VaultX/
│
├── backend/
│   └── src/main/java/com/vaultx/
│       ├── VaultXServer.java              ← Main HTTP server (port 8080)
│       ├── db/
│       │   └── DatabaseManager.java       ← JDBC Singleton (MySQL)
│       ├── model/
│       │   ├── User.java                  ← Abstract base (Abstraction)
│       │   ├── BankUser.java              ← Extends User (Inheritance)
│       │   ├── Admin.java                 ← Extends User (Polymorphism)
│       │   ├── Account.java               ← Account with Status enums
│       │   └── Transaction.java           ← Transaction with Type enums
│       ├── service/
│       │   └── BankService.java           ← All JDBC banking operations
│       ├── exception/
│       │   ├── VaultXException.java       ← Base exception
│       │   ├── InsufficientFundsException.java
│       │   ├── AccountFrozenException.java
│       │   └── AuthException.java
│       └── util/
│           ├── SecurityUtil.java          ← SHA-256, ID generation
│           └── TransactionLogUtil.java    ← File Handling (logs/)
│
├── frontend/
│   ├── index.html                         ← Single page app
│   ├── css/
│   │   └── style.css                      ← Luxury dark theme
│   └── js/
│       ├── api.js                         ← All fetch() calls
│       └── app.js                         ← UI logic & rendering
│
├── database/
│   └── schema.sql                         ← MySQL schema
│
├── logs/                                  ← Auto-generated .log files
│
└── README.md
```

---

## ▶️ How to Run

### Prerequisites
- Java 17+ : https://jdk.java.net
- MySQL 8+  : https://dev.mysql.com/downloads/
- MySQL JDBC Driver (mysql-connector-j-8.x.jar)

---

### Step 1 — Setup MySQL Database

```sql
mysql -u root -p
source VaultX/database/schema.sql;
```

Or paste the contents of `database/schema.sql` in MySQL Workbench.

---

### Step 2 — Update DB Password

Edit `backend/src/main/java/com/vaultx/db/DatabaseManager.java`:
```java
private static final String DB_PASSWORD = "your_mysql_password"; // ← change this
```

---

### Step 3 — Download MySQL JDBC Driver

Download from: https://dev.mysql.com/downloads/connector/j/
Place `mysql-connector-j-8.x.x.jar` in a `lib/` folder in VaultX root.

---

### Step 4 — Compile Java

```bash
cd VaultX

# Create output directory
mkdir -p out

# Compile all Java files
javac -cp "lib/mysql-connector-j-8.x.x.jar" \
  -d out \
  backend/src/main/java/com/vaultx/db/DatabaseManager.java \
  backend/src/main/java/com/vaultx/exception/*.java \
  backend/src/main/java/com/vaultx/model/*.java \
  backend/src/main/java/com/vaultx/util/*.java \
  backend/src/main/java/com/vaultx/service/BankService.java \
  backend/src/main/java/com/vaultx/VaultXServer.java
```

---

### Step 5 — Run the Server

```bash
java -cp "out:lib/mysql-connector-j-8.x.x.jar" com.vaultx.VaultXServer
```

On Windows:
```bash
java -cp "out;lib/mysql-connector-j-8.x.x.jar" com.vaultx.VaultXServer
```

---

### Step 6 — Open in Browser

```
http://localhost:8080
```

---

## 🔐 Login Credentials

| Role  | Email                | Password  |
|-------|----------------------|-----------|
| Admin | admin@vaultx.com     | admin123  |
| Demo  | (Register new user)  | —         |

---

## 📡 API Endpoints

| Method | Endpoint                    | Description            |
|--------|-----------------------------|------------------------|
| POST   | /api/register               | Create new account     |
| POST   | /api/login                  | Login user/admin       |
| POST   | /api/deposit                | Deposit money          |
| POST   | /api/withdraw               | Withdraw money         |
| POST   | /api/transfer               | Transfer between accounts |
| POST   | /api/balance                | Check balance          |
| POST   | /api/transactions           | Get transaction history|
| GET    | /api/admin/accounts         | All accounts (admin)   |
| GET    | /api/admin/transactions     | All transactions (admin)|
| POST   | /api/admin/freeze           | Freeze/unfreeze account|
| POST   | /api/admin/delete           | Delete account         |

---

## ☕ Java OOP Concepts Demonstrated

| Concept          | Implementation                                      |
|------------------|-----------------------------------------------------|
| Abstraction      | `User.java` abstract class with abstract methods    |
| Inheritance      | `BankUser`, `Admin` extend `User`                   |
| Polymorphism     | `getDashboardType()`, `getWelcomeMessage()` overridden |
| Encapsulation    | Private fields + public getters/setters in all models |
| Exception Handling | Custom exception hierarchy: `VaultXException` → sub-exceptions |
| File Handling    | `TransactionLogUtil` — BufferedWriter/Reader for .log files |
| JDBC             | `DatabaseManager` + `BankService` — PreparedStatements, transactions |
| Collections      | ArrayList, List for result sets                    |
| Enums            | `AccountType`, `AccountStatus`, `TxnType`, `Priority` |
| Singleton        | `DatabaseManager.getInstance()`                    |
| Java Libraries   | `java.security.MessageDigest` (SHA-256), `java.nio`, `java.time` |

---

## 🗄️ MySQL Tables

- `users` — user accounts and credentials
- `accounts` — bank accounts with balance and status
- `transactions` — every money movement

---

## 📝 Log Files

Transaction logs are saved automatically to:
```
logs/transactions_YYYY-MM-DD.log
logs/system_YYYY-MM-DD.log
```
