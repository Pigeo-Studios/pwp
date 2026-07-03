-- PWP: Add new tables for skin/economy update
-- Run this against your pwp_core database

USE pwp_core;

CREATE TABLE IF NOT EXISTS skin_definitions (
    skin_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    slot_type VARCHAR(32) NOT NULL,
    weapon_tag VARCHAR(32) NOT NULL DEFAULT 'any',
    rarity VARCHAR(16) NOT NULL DEFAULT 'COMMON',
    model_path VARCHAR(255),
    image_url VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS case_definitions (
    case_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    price_coins INT NOT NULL DEFAULT 0,
    icon_path VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS case_loot (
    case_id VARCHAR(64),
    skin_id VARCHAR(64),
    weight INT NOT NULL DEFAULT 100,
    is_guaranteed BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (case_id, skin_id),
    FOREIGN KEY (case_id) REFERENCES case_definitions(case_id) ON DELETE CASCADE,
    FOREIGN KEY (skin_id) REFERENCES skin_definitions(skin_id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS reward_config (
    action VARCHAR(32) PRIMARY KEY,
    xp_reward BIGINT NOT NULL DEFAULT 0,
    coins_reward BIGINT NOT NULL DEFAULT 0,
    score_reward INT NOT NULL DEFAULT 0
);

-- Default reward values (adjust as needed)
INSERT IGNORE INTO reward_config (action, xp_reward, coins_reward, score_reward) VALUES
('KILL', 50, 10, 100),
('ASSIST', 25, 5, 25),
('VEHICLE_KILL', 150, 30, 150),
('CAPTURE', 100, 25, 200),
('REVIVE', 75, 15, 75),
('WIN', 200, 50, 0),
('LOSS', 100, 20, 0),
('TIME_MINUTE', 10, 0, 0),
('HEADSHOT', 25, 5, 50);
