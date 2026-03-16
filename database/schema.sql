-- ============================================================
--  VaultX Banking System — MySQL Database Schema
-- ============================================================

CREATE DATABASE IF NOT EXISTS vaultx_db;
USE vaultx_db;

-- ── Users Table ─────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    full_name     VARCHAR(100)        NOT NULL,
    email         VARCHAR(100) UNIQUE NOT NULL,
    phone         VARCHAR(15)         NOT NULL,
    password_hash VARCHAR(255)        NOT NULL,
    role          ENUM('USER','ADMIN') DEFAULT 'USER',
    is_active     BOOLEAN              DEFAULT TRUE,
    created_at    TIMESTAMP            DEFAULT CURRENT_TIMESTAMP
);

-- ── Accounts Table ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS accounts (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    account_number VARCHAR(20) UNIQUE  NOT NULL,
    user_id        INT                 NOT NULL,
    account_type   ENUM('SAVINGS','CURRENT','FIXED') DEFAULT 'SAVINGS',
    balance        DECIMAL(15,2)       DEFAULT 0.00,
    is_frozen      BOOLEAN             DEFAULT FALSE,
    created_at     TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ── Transactions Table ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS transactions (
    id               INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id   VARCHAR(30) UNIQUE  NOT NULL,
    account_id       INT                 NOT NULL,
    type             ENUM('DEPOSIT','WITHDRAWAL','TRANSFER_IN','TRANSFER_OUT') NOT NULL,
    amount           DECIMAL(15,2)       NOT NULL,
    balance_after    DECIMAL(15,2)       NOT NULL,
    description      VARCHAR(255),
    target_account   VARCHAR(20),
    status           ENUM('SUCCESS','FAILED','PENDING') DEFAULT 'SUCCESS',
    created_at       TIMESTAMP           DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

-- ── Admin seed account ───────────────────────────────────────
-- Password: admin123 (SHA-256 hashed)
INSERT IGNORE INTO users (full_name, email, phone, password_hash, role)
VALUES ('VaultX Admin', 'admin@vaultx.com', '9999999999',
        '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'ADMIN');

-- ── Sample User (for testing) ────────────────────────────────
-- Password: User@123
INSERT IGNORE INTO users (full_name, email, phone, password_hash, role)
VALUES ('John Doe', 'john@example.com', '9876543210',
        '3e7c19576488862816f13b512cacf3e4ba97dd97243ea0bd6a2ad1642d86ba72', 'USER');

INSERT IGNORE INTO accounts (account_number, user_id, account_type, balance)
SELECT 'VX10000001', id, 'SAVINGS', 25000.00
FROM users WHERE email = 'john@example.com';
