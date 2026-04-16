-- Revolutionary Bank Database Schema v3.0
-- Author: Gabriel J. Zayas

CREATE DATABASE IF NOT EXISTS revolutionary_bank;
USE revolutionary_bank;

-- User Table: Stores credentials and core account data
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL, -- Hashed
    balance DECIMAL(15, 2) DEFAULT 0.00,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    security_question VARCHAR(255),
    security_answer VARCHAR(255)
);

-- Transactions Table: Linked to users via user_id
CREATE TABLE IF NOT EXISTS transactions (
    fk_user_transaction INT AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(50) NOT NULL UNIQUE,
    user_id INT NOT NULL,
    description VARCHAR(255),
    type VARCHAR(20) NOT NULL;,
    amount DECIMAL(15, 2) NOT NULL,
    note TEXT,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
