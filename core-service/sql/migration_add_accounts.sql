-- PWP: Add account system tables
-- Run this against your pwp_core database

USE pwp_core;

CREATE TABLE IF NOT EXISTS accounts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    login VARCHAR(32) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    telegram_id BIGINT UNIQUE,
    role VARCHAR(16) NOT NULL DEFAULT 'user',
    hwid VARCHAR(255) DEFAULT NULL,
    privacy_policy_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    privacy_policy_accepted_at DATETIME DEFAULT NULL,
    launcher_2fa_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    ban_reason TEXT DEFAULT NULL,
    last_ip VARCHAR(45) DEFAULT NULL,
    last_login DATETIME DEFAULT NULL,
    registered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_accounts_telegram (telegram_id),
    INDEX idx_accounts_login (login),
    INDEX idx_accounts_uuid (uuid)
);

CREATE TABLE IF NOT EXISTS trusted_ips (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    ip VARCHAR(45) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_account_ip (account_id, ip),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS sessions (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    ip VARCHAR(45) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    INDEX idx_sessions_token (token),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS twofa_codes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    code VARCHAR(6) NOT NULL,
    ip VARCHAR(45) DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_twofa_account (account_id),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS password_resets (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT NOT NULL,
    admin_id INT DEFAULT NULL,
    status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME DEFAULT NULL,
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (admin_id) REFERENCES accounts(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS account_logs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    account_id INT DEFAULT NULL,
    action VARCHAR(64) NOT NULL,
    ip VARCHAR(45) DEFAULT NULL,
    details TEXT DEFAULT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_logs_account (account_id),
    INDEX idx_logs_action (action),
    INDEX idx_logs_created (created_at),
    FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS broadcast_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    admin_id INT NOT NULL,
    message TEXT NOT NULL,
    recipient_count INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (admin_id) REFERENCES accounts(id) ON DELETE CASCADE
);

-- Add telegram_id column to players table if it doesn't exist
ALTER TABLE players ADD COLUMN telegram_id BIGINT DEFAULT NULL;
ALTER TABLE players ADD INDEX idx_players_telegram (telegram_id);
