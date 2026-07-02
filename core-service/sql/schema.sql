-- PWP Core Service Database Schema
-- MySQL 8.0+

CREATE DATABASE IF NOT EXISTS pwp_core CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pwp_core;

-- ============================================================
-- 1. ИГРОКИ
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

    -- Combat
    kills INT NOT NULL DEFAULT 0,
    deaths INT NOT NULL DEFAULT 0,
    assists INT NOT NULL DEFAULT 0,
    kill_streak_best INT NOT NULL DEFAULT 0,
    headshots INT NOT NULL DEFAULT 0,
    teamkills INT NOT NULL DEFAULT 0,

    -- Score / Progression
    total_score BIGINT NOT NULL DEFAULT 0,
    score_per_minute DOUBLE NOT NULL DEFAULT 0,

    -- Vehicles
    vehicle_kills INT NOT NULL DEFAULT 0,
    vehicles_destroyed INT NOT NULL DEFAULT 0,
    vehicles_driven INT NOT NULL DEFAULT 0,

    -- Teamplay
    revives INT NOT NULL DEFAULT 0,
    healing_done BIGINT NOT NULL DEFAULT 0,
    supplies_delivered INT NOT NULL DEFAULT 0,
    hub_builds INT NOT NULL DEFAULT 0,
    rally_points_placed INT NOT NULL DEFAULT 0,

    -- Objectives
    captures INT NOT NULL DEFAULT 0,
    defends INT NOT NULL DEFAULT 0,

    -- Match history
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0,
    games_played INT NOT NULL DEFAULT 0,
    playtime_seconds BIGINT NOT NULL DEFAULT 0,

    -- Accuracy
    shots_fired INT NOT NULL DEFAULT 0,
    shots_hit INT NOT NULL DEFAULT 0,
    longest_kill_distance DOUBLE NOT NULL DEFAULT 0,
    distance_traveled BIGINT NOT NULL DEFAULT 0,

    -- Leadership
    time_as_squad_leader BIGINT NOT NULL DEFAULT 0,
    time_as_commander BIGINT NOT NULL DEFAULT 0,

    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

-- ============================================================
-- 2. ВАЛЮТА
-- ============================================================
CREATE TABLE player_currency (
    uuid VARCHAR(36) PRIMARY KEY,
    coins BIGINT NOT NULL DEFAULT 0,
    total_earned BIGINT NOT NULL DEFAULT 0,
    total_spent BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

-- ============================================================
-- 3. XP / РАНГИ
-- ============================================================
CREATE TABLE player_xp (
    uuid VARCHAR(36) PRIMARY KEY,
    xp BIGINT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 1,
    prestige INT NOT NULL DEFAULT 0,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE TABLE rank_definitions (
    rank_id INT PRIMARY KEY AUTO_INCREMENT,
    rank_name VARCHAR(32) NOT NULL,
    xp_required BIGINT NOT NULL,
    level_required INT NOT NULL DEFAULT 1,
    kits_unlocked JSON,
    description TEXT
);

-- Игроки и их прогресс по рангам
CREATE TABLE player_ranks (
    uuid VARCHAR(36),
    rank_id INT NOT NULL,
    unlocked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uuid, rank_id),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE,
    FOREIGN KEY (rank_id) REFERENCES rank_definitions(rank_id) ON DELETE CASCADE
);

-- ============================================================
-- 4. ТЕХНИКА / АНЛОКИ
-- ============================================================
CREATE TABLE vehicle_unlocks (
    vehicle_id VARCHAR(64) PRIMARY KEY,
    display_name VARCHAR(64) NOT NULL,
    vehicle_type VARCHAR(32) NOT NULL,
    rank_required INT NOT NULL DEFAULT 1,
    xp_required BIGINT NOT NULL DEFAULT 0,
    coins_required BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (rank_required) REFERENCES rank_definitions(rank_id)
);

CREATE TABLE player_vehicle_unlocks (
    uuid VARCHAR(36),
    vehicle_id VARCHAR(64),
    unlocked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (uuid, vehicle_id),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE,
    FOREIGN KEY (vehicle_id) REFERENCES vehicle_unlocks(vehicle_id) ON DELETE CASCADE
);

-- ============================================================
-- 5. КИТЫ / РОЛИ (анлоки по рангам)
-- ============================================================
CREATE TABLE kit_unlocks (
    kit_name VARCHAR(32) PRIMARY KEY,
    display_name VARCHAR(64) NOT NULL,
    rank_required INT NOT NULL DEFAULT 1,
    xp_required BIGINT NOT NULL DEFAULT 0,
    description TEXT,
    icon_path VARCHAR(255),
    FOREIGN KEY (rank_required) REFERENCES rank_definitions(rank_id)
);

-- ============================================================
-- 6. ДОСТИЖЕНИЯ
-- ============================================================
CREATE TABLE achievements (
    achievement_id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    description TEXT,
    icon_path VARCHAR(255),
    category VARCHAR(32) NOT NULL,  -- COMBAT, TEAMPLAY, VEHICLE, PROGRESSION, SPECIAL
    rarity VARCHAR(16) NOT NULL DEFAULT 'COMMON',
    xp_reward BIGINT NOT NULL DEFAULT 0,
    coins_reward BIGINT NOT NULL DEFAULT 0,
    criteria_type VARCHAR(32) NOT NULL,
    criteria_value BIGINT NOT NULL,
    hidden BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE player_achievements (
    uuid VARCHAR(36),
    achievement_id VARCHAR(64),
    progress BIGINT NOT NULL DEFAULT 0,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_at DATETIME,
    PRIMARY KEY (uuid, achievement_id),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE,
    FOREIGN KEY (achievement_id) REFERENCES achievements(achievement_id) ON DELETE CASCADE
);

-- ============================================================
-- 7. СЕЗОНЫ / БОЕВОЙ ПРОПУСК
-- ============================================================
CREATE TABLE seasons (
    season_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL,
    start_date DATETIME NOT NULL,
    end_date DATETIME NOT NULL,
    free_rewards JSON,
    premium_rewards JSON,
    is_active BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE player_season_progress (
    uuid VARCHAR(36),
    season_id INT NOT NULL,
    xp_earned BIGINT NOT NULL DEFAULT 0,
    level INT NOT NULL DEFAULT 1,
    has_premium BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (uuid, season_id),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE,
    FOREIGN KEY (season_id) REFERENCES seasons(season_id) ON DELETE CASCADE
);

-- ============================================================
-- 8. ИСТОРИЯ МАТЧЕЙ
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

    -- Базовое
    kills INT NOT NULL DEFAULT 0,
    deaths INT NOT NULL DEFAULT 0,
    assists INT NOT NULL DEFAULT 0,
    score INT NOT NULL DEFAULT 0,

    -- Детальное
    vehicle_kills INT NOT NULL DEFAULT 0,
    captures INT NOT NULL DEFAULT 0,
    revives INT NOT NULL DEFAULT 0,
    shots_fired INT NOT NULL DEFAULT 0,
    shots_hit INT NOT NULL DEFAULT 0,
    damage_dealt DOUBLE NOT NULL DEFAULT 0,
    healing_done DOUBLE NOT NULL DEFAULT 0,
    supplies_delivered INT NOT NULL DEFAULT 0,
    longest_kill DOUBLE NOT NULL DEFAULT 0,

    -- Мета
    role VARCHAR(32),
    squad_id INT DEFAULT NULL,
    was_squad_leader BOOLEAN DEFAULT FALSE,

    PRIMARY KEY (match_id, uuid),
    FOREIGN KEY (match_id) REFERENCES match_history(match_id) ON DELETE CASCADE,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

-- ============================================================
-- 9. БОЕВОЙ ЛОГ (каждое убийство)
-- ============================================================
CREATE TABLE combat_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    match_id BIGINT NOT NULL,
    killer_uuid VARCHAR(36),
    victim_uuid VARCHAR(36),
    weapon VARCHAR(64),
    damage_type VARCHAR(32) NOT NULL DEFAULT 'BULLET',
    distance DOUBLE NOT NULL DEFAULT 0,
    is_headshot BOOLEAN NOT NULL DEFAULT FALSE,
    is_teamkill BOOLEAN NOT NULL DEFAULT FALSE,
    timestamp DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (match_id) REFERENCES match_history(match_id) ON DELETE CASCADE
);

-- ============================================================
-- 10. СКИНЫ / КОСМЕТИКА
-- ============================================================
CREATE TABLE player_cosmetics (
    item_uuid VARCHAR(36) PRIMARY KEY,
    player_uuid VARCHAR(36) NOT NULL,
    skin_id VARCHAR(64) NOT NULL,
    slot_type VARCHAR(32) NOT NULL,  -- KNIFE, PRIMARY, SECONDARY, UNIFORM, EFFECT, VOICE, ANIMATION, PATCH
    rarity VARCHAR(16) NOT NULL DEFAULT 'COMMON',
    obtained_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    source VARCHAR(32) NOT NULL,  -- SHOP, DROP, DONATE, ACHIEVEMENT, SEASON, BATTLEPASS
    tradeable BOOLEAN NOT NULL DEFAULT TRUE,
    deletable BOOLEAN NOT NULL DEFAULT TRUE,
    FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE TABLE player_equipment (
    uuid VARCHAR(36) NOT NULL,
    slot_type VARCHAR(32) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'ALL',
    item_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY (uuid, slot_type, role),
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE,
    FOREIGN KEY (item_uuid) REFERENCES player_cosmetics(item_uuid) ON DELETE CASCADE
);

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
-- 11. ЛОГИРОВАНИЕ / ДОНАТ
-- ============================================================
CREATE TABLE operation_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    operation_type VARCHAR(32) NOT NULL,  -- XP_ADD, COINS_ADD, COINS_SPEND, ITEM_GRANT, DONATE, ACHIEVEMENT, RANK_UP
    amount BIGINT DEFAULT 0,
    details JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

CREATE TABLE donation_transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL,
    item_id VARCHAR(64) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    currency VARCHAR(8) NOT NULL DEFAULT 'RUB',
    payment_id VARCHAR(128),
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',  -- PENDING, COMPLETED, FAILED, REFUNDED
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at DATETIME,
    FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE
);

-- ============================================================
-- 12. API КЛЮЧИ
-- ============================================================
CREATE TABLE api_keys (
    id INT AUTO_INCREMENT PRIMARY KEY,
    key_value VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    permission_level VARCHAR(16) NOT NULL DEFAULT 'SERVER',  -- SERVER, WEBSITE, ADMIN
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- ИНДЕКСЫ
-- ============================================================
CREATE INDEX idx_player_stats_uuid ON player_stats(uuid);
CREATE INDEX idx_player_cosmetics_player ON player_cosmetics(player_uuid);
CREATE INDEX idx_player_equipment_player ON player_equipment(uuid);
CREATE INDEX idx_match_players_match ON match_players(match_id);
CREATE INDEX idx_match_players_uuid ON match_players(uuid);
CREATE INDEX idx_operation_logs_uuid ON operation_logs(uuid);
CREATE INDEX idx_operation_logs_type ON operation_logs(operation_type);
CREATE INDEX idx_operation_logs_created ON operation_logs(created_at);
CREATE INDEX idx_donation_uuid ON donation_transactions(uuid);
CREATE INDEX idx_donation_status ON donation_transactions(status);
CREATE INDEX idx_combat_log_match ON combat_log(match_id);
CREATE INDEX idx_combat_log_killer ON combat_log(killer_uuid);
CREATE INDEX idx_combat_log_victim ON combat_log(victim_uuid);
CREATE INDEX idx_player_achievements_completed ON player_achievements(uuid, completed);
CREATE INDEX idx_player_ranks_player ON player_ranks(uuid);
CREATE INDEX idx_player_season ON player_season_progress(uuid, season_id);
