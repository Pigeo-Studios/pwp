-- PWP Core Service Database Schema
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS pwp_core CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pwp_core;

-- ============================================================
-- ИГРОКИ
-- ============================================================
CREATE TABLE players (
    uuid VARCHAR(36) PRIMARY KEY,
    nickname VARCHAR(32) NOT NULL,
    first_join DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_join DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    donate_tier VARCHAR(16) NOT NULL DEFAULT 'NONE',
    role VARCHAR(16) NOT NULL DEFAULT 'PLAYER',
    is_banned BOOLEAN NOT NULL DEFAULT FALSE,
    ban_reason TEXT
);

CREATE TABLE player_stats (
    uuid VARCHAR(36) PRIMARY KEY,
    kills INT NOT NULL DEFAULT 0,
    deaths INT NOT NULL DEFAULT 0,
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    playtime_seconds BIGINT NOT NULL DEFAULT 0,
    shots_fired INT NOT NULL DEFAULT 0,
    shots_hit INT NOT NULL DEFAULT 0,
    revives INT NOT NULL DEFAULT 0,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

-- ============================================================
-- ВАЛЮТА
-- ============================================================
CREATE TABLE player_currency (
    uuid VARCHAR(36) PRIMARY KEY,
    coins BIGINT NOT NULL DEFAULT 0,
    total_earned BIGINT NOT NULL DEFAULT 0,
    total_spent BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

-- ============================================================
-- XP / УРОВНИ
-- ============================================================
CREATE TABLE player_xp (
    uuid VARCHAR(36) PRIMARY KEY,
    xp BIGINT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 1,
    prestige INT NOT NULL DEFAULT 0,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

-- ============================================================
-- ИНВЕНТАРЬ СКИНОВ (каждый предмет = свой UUID)
-- ============================================================
CREATE TABLE player_cosmetics (
    item_uuid VARCHAR(36) PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    skin_id VARCHAR(64) NOT NULL,
    slot_type VARCHAR(32) NOT NULL,
    rarity VARCHAR(16) NOT NULL DEFAULT 'COMMON',
    obtained_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(32) NOT NULL,
    tradeable BOOLEAN NOT NULL DEFAULT TRUE,
    deletable BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

-- ============================================================
-- ЭКИПИРОВКА ПО РОЛЯМ
-- ============================================================
CREATE TABLE player_equipment (
    uuid VARCHAR(36) NOT NULL,
    slot_type VARCHAR(32) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'ALL',
    item_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY (uuid, slot_type, role),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE,
    FOREIGN KEY (item_uuid) REFERENCES player_cosmetics(item_uuid) ON DELETE CASCADE
);

-- ============================================================
-- МАГАЗИН
-- ============================================================
CREATE TABLE shop_items (
    skin_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    slot_type VARCHAR(32) NOT NULL,
    rarity VARCHAR(16) NOT NULL DEFAULT 'COMMON',
    price_coins INT NOT NULL DEFAULT 0,
    price_real DECIMAL(10,2),
    model_path VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- ИСТОРИЯ МАТЧЕЙ
-- ============================================================
CREATE TABLE match_history (
    match_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    map_name VARCHAR(64) NOT NULL,
    mode VARCHAR(32) NOT NULL DEFAULT 'AAS',
    team_blue_score INT NOT NULL DEFAULT 0,
    team_red_score INT NOT NULL DEFAULT 0,
    winner VARCHAR(16),
    duration_seconds INT NOT NULL DEFAULT 0,
    started_at DATETIME NOT NULL,
    ended_at DATETIME NOT NULL
);

CREATE TABLE match_players (
    match_id BIGINT NOT NULL,
    uuid VARCHAR(36) NOT NULL,
    team VARCHAR(16) NOT NULL,
    kills INT NOT NULL DEFAULT 0,
    deaths INT NOT NULL DEFAULT 0,
    score INT NOT NULL DEFAULT 0,
    role VARCHAR(32),
    PRIMARY KEY (match_id, uuid),
    FOREIGN KEY (match_id) REFERENCES match_history(match_id) ON DELETE CASCADE,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

-- ============================================================
-- ЛОГИРОВАНИЕ ОПЕРАЦИЙ
-- ============================================================
CREATE TABLE operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,
    amount BIGINT DEFAULT 0,
    details JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

-- ============================================================
-- ДОНАТ ТРАНЗАКЦИИ
-- ============================================================
CREATE TABLE donation_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'RUB',
    payment_id VARCHAR(128),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

-- ============================================================
-- АДМИН-КЛЮЧИ (для аутентификации API)
-- ============================================================
CREATE TABLE api_keys (
    id INT AUTO_INCREMENT PRIMARY KEY,
    key_value VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    permission_level VARCHAR(16) NOT NULL DEFAULT 'SERVER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- ИНДЕКСЫ
-- ============================================================
CREATE INDEX idx_player_cosmetics_player ON player_cosmetics(player_uuid);
CREATE INDEX idx_player_equipment_player ON player_equipment(uuid);
CREATE INDEX idx_match_players_match ON match_players(match_id);
CREATE INDEX idx_match_players_uuid ON match_players(uuid);
CREATE INDEX idx_operation_logs_uuid ON operation_logs(uuid);
CREATE INDEX idx_operation_logs_type ON operation_logs(operation_type);
CREATE INDEX idx_donation_uuid ON donation_transactions(uuid);
CREATE INDEX idx_donation_status ON donation_transactions(status);
