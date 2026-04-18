# 🏦 VaultX — Advanced Banking Management System

VaultX is a complete, full-stack Banking Management System built to demonstrate core Java Object-Oriented Programming (OOP) concepts, robust backend design patterns, and a dynamic, responsive frontend. This project operates seamlessly without relying on heavy frontend frameworks or external server dependencies besides MySQL.

---

## 🌟 Key Features

### 👤 User Portal
- **Secure Authentication**: End-to-end user registration and login with SHA-256 password hashing.
- **Real-Time Dashboard**: View current balance, account status (Active/Frozen), and recent transaction summaries.
- **Financial Operations**: Instantly deposit funds, withdraw money, and execute peer-to-peer transfers using account numbers.
- **Transaction History**: Comprehensive, searchable list of all transaction records, including exportable full bank statements.

### 🛡️ Admin Portal
- **System Overview**: High-level dashboard showing total registered accounts, overall system balance, and total transactions.
- **Account Management**: View every user's details, freeze/unfreeze accounts for compliance or security, and permanently delete accounts.
- **Global Transaction Monitoring**: Audit all system-wide deposits, withdrawals, and transfers in real time.

---

## 📂 Complete Project Structure & File Index

A detailed breakdown of every component in the VaultX architecture.

```text
VaultX/
│
├── backend/                              ← Java Backend Component
│   └── src/main/java/com/vaultx/
│       ├── VaultXServer.java             ← Main application entry point; sets up HttpServer on port 8080 and defines all REST API HTTP handlers.
│       │
│       ├── db/
│       │   └── DatabaseManager.java      ← Singleton database manager for establishing and providing MySQL JDBC Connections.
│       │
│       ├── model/                        ← Domain Models (Data Layer)
│       │   ├── User.java                 ← Abstract base class for all authenticated system users.
│       │   ├── BankUser.java             ← Concrete implementation representing a standard customer.
│       │   ├── Admin.java                ← Concrete implementation representing a system administrator.
│       │   ├── Account.java              ← Represents a bank account (balance, status, type).
│       │   └── Transaction.java          ← Represents a financial movement (deposit/withdraw/transfer).
│       │
│       ├── service/
│       │   └── BankService.java          ← Core business logic layer containing methods for login, registration, and all banking operations (transactions inside JDBC).
│       │
│       ├── exception/                    ← Custom Exceptions
│       │   ├── VaultXException.java      ← Base exception class for the application.
│       │   ├── AuthException.java        ← Thrown during login/authentication failures.
│       │   ├── AccountFrozenException.java ← Thrown when attempting operations on a frozen account.
│       │   └── InsufficientFundsException.java ← Thrown during over-withdrawal or over-transfer attempts.
│       │
│       └── util/
│           ├── SecurityUtil.java         ← Utility for SHA-256 password hashing and secure transaction ID generation.
│           └── TransactionLogUtil.java   ← Utility for asynchronous writing of system logs to local text files.
│
├── frontend/                             ← Static Frontend Component
│   ├── index.html                        ← Single Page Application (SPA) entry point.
│   ├── css/
│   │   └── style.css                     ← Custom styling with a premium, luxury dark mode theme using pure CSS variables.
│   └── js/
│       ├── api.js                        ← Encapsulates all `fetch()` calls connecting the UI to the backend REST API.
│       └── app.js                        ← Handles all DOM manipulation, state management (session), and UI rendering/routing logic.
│
├── database/
│   └── schema.sql                        ← MySQL structure file containing table definitions (`users`, `accounts`, `transactions`) and initial default data.
│
├── logs/                                 ← Auto-generated system logs
│   ├── system_YYYY-MM-DD.log             ← Application lifecycle and error logs.
│   └── transactions_YYYY-MM-DD.log       ← Immutable ledger backup of all financial movements.
│
└── run.bat                               ← Windows batch script to quickly compile and run the backend.
```

---

## ☕ Technical Architecture & OOP Concepts

VaultX is specifically architected to demonstrate the core tenets of Java programming:

| Concept | How It's Implemented in VaultX |
|---------|--------------------------------|
| **Abstraction** | `User.java` is an abstract class defining shared properties (`id`, `fullName`, `email`) and enforcing abstract method implementations like `getRole()`. |
| **Inheritance** | Both `BankUser.java` and `Admin.java` inherit from the base `User` class. |
| **Polymorphism** | Role-specific behaviors are achieved through overridden methods, allowing the system to handle standard users and admins dynamically. |
| **Encapsulation** | Strict use of `private` fields and `public` getter/setter accessors across all domain models to protect object state. |
| **Exception Handling**| A robust custom exception hierarchy extending `RuntimeException` (`VaultXException` -> Sub-exceptions) ensuring precise REST API error responses. |
| **Singleton Pattern** | `DatabaseManager.getInstance()` ensures only one database connection pool/manager exists throughout the application lifecycle. |
| **File Handling** | `TransactionLogUtil.java` uses `BufferedWriter` and `FileReader` to maintain physical transaction logs in the `logs/` directory. |
| **Concurrency** | The JDK `HttpServer` handles incoming API requests concurrently via defined endpoints. |

---

## 📡 RESTful API Reference

The Java backend exposes the following JSON APIs on `http://localhost:8080/api/...`

### User Endpoints
- `POST /register`: Accepts `{fullName, email, phone, password, accountType}`. Creates User & Account.
- `POST /login`: Accepts `{email, password}`. Returns Session IDs and Roles.
- `POST /deposit`: Accepts `{accountNumber, amount, description}`. Increments balance.
- `POST /withdraw`: Accepts `{accountNumber, amount, description}`. Decrements balance conditionally.
- `POST /transfer`: Accepts `{fromAccount, toAccount, amount, description}`. Atomic sequence.
- `POST /balance`: Retrieves current balance and active/frozen status.
- `POST /transactions`: Retrieves account-specific transaction history (supports `limit`).

### Admin Endpoints
- `GET /admin/accounts`: Retrieves global list of all user accounts and balances.
- `GET /admin/transactions`: Retrieves global chronological transaction feed.
- `POST /admin/freeze`: Accepts `{accountNumber, action: "freeze"/"unfreeze"}`. Updates account status.
- `POST /admin/delete`: Accepts `{accountNumber}`. Permanently erases user & account via Cascade Delete.

---

## 🚀 Installation & Setup Guide

### 1. Prerequisites
- **Java SE 17** or higher installed and added to PATH.
- **MySQL Server 8.0** or higher running locally.
- **MySQL Connector/J** (`mysql-connector-j-8.x.x.jar`) placed in a `lib/` directory in the project root.

### 2. Database Configuration
1. Login to your local MySQL prompt: `mysql -u root -p`
2. Source the schema file: `source C:/path/to/VaultX/database/schema.sql;`
3. Update the database password inside the backend:
   Navigate to `backend/src/main/java/com/vaultx/db/DatabaseManager.java` and modify:
   ```java
   private static final String DB_PASSWORD = "your_mysql_password"; // Update this
   ```

### 3. Compilation & Execution (Windows)
You can use the provided batch script for convenience or compile manually.

**Using Command Line:**
```bash
# 1. Create output directory
mkdir out

# 2. Compile Server & Dependencies
javac -cp "lib/mysql-connector-j*.*" -d out backend/src/main/java/com/vaultx/**/*.java

# 3. Start the Server
java -cp "out;lib/*" com.vaultx.VaultXServer
```

### 4. Access the Application
Once the server displays `VaultX Server started on port 8080`, open your browser and navigate to:
```
http://localhost:8080
```

### 5. Default Login Credentials
| Role | Email | Password |
|------|-------|----------|
| **Admin** | admin@vaultx.com | admin123 |
| **Demo User**| john@example.com | User@123 |
_*(You can also use the registration portal to create dynamic new users.)*_

---

## 🛡️ Security Implementation
- **Password Hashes**: Plaintext passwords are never stored. `SecurityUtil.java` hashes all inputs using **SHA-256** before database insertion and during authentication verification.
- **SQL Injection Prevention**: All database queries utilizing user inputs strictly use `PreparedStatement` to parameterize inputs and prevent injection attacks.
- **Atomic Transactions**: Transfers between accounts are wrapped in SQL transaction blocks (`conn.setAutoCommit(false)`), ensuring that either both the debit and credit succeed, or both fail and roll back safely.
