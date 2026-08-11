package com.pwp.core.db;

import com.pwp.core.CoreApplication;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static HikariDataSource dataSource;

    public static void init(CoreApplication.DatabaseConfig cfg) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl("jdbc:mysql://" + cfg.host + ":" + cfg.port + "/" + cfg.name
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        hikari.setUsername(cfg.user);
        hikari.setPassword(cfg.password);
        hikari.setMaximumPoolSize(cfg.poolSize);
        hikari.setMaxLifetime(cfg.maxLifetimeMs);
        hikari.setMinimumIdle(2);
        hikari.setConnectionTimeout(5000);
        hikari.setConnectionTestQuery("SELECT 1");
        hikari.setPoolName("PWP-Core-Pool");

        dataSource = new HikariDataSource(hikari);
        log.info("Database pool initialized: {}:{}/{}", cfg.host, cfg.port, cfg.name);

        createTables();
    }

    private static void createTables() {
        String[] tables = {
            "CREATE TABLE IF NOT EXISTS players ("
            + "uuid VARCHAR(36) PRIMARY KEY, nickname VARCHAR(64) NOT NULL,"
            + "login VARCHAR(32) UNIQUE, email VARCHAR(255) UNIQUE,"
            + "password_hash VARCHAR(255),"
            + "telegram_id BIGINT UNIQUE,"
            + "hwid VARCHAR(255) DEFAULT NULL,"
            + "privacy_policy_accepted BOOLEAN NOT NULL DEFAULT FALSE,"
            + "launcher_2fa_enabled BOOLEAN NOT NULL DEFAULT FALSE,"
            + "first_join DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "last_join DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
            + "last_ip VARCHAR(45) DEFAULT NULL,"
            + "donate_tier VARCHAR(16) NOT NULL DEFAULT 'NONE',"
            + "role VARCHAR(32) DEFAULT 'PLAYER',"
            + "is_banned BOOLEAN NOT NULL DEFAULT FALSE,"
            + "ban_reason TEXT DEFAULT NULL)",

            "CREATE TABLE IF NOT EXISTS player_stats ("
            + "uuid VARCHAR(36) PRIMARY KEY,"
            + "kills INT NOT NULL DEFAULT 0, deaths INT NOT NULL DEFAULT 0,"
            + "wins INT NOT NULL DEFAULT 0, losses INT NOT NULL DEFAULT 0,"
            + "playtime_seconds BIGINT NOT NULL DEFAULT 0,"
            + "shots_fired INT NOT NULL DEFAULT 0, shots_hit INT NOT NULL DEFAULT 0,"
            + "revives INT NOT NULL DEFAULT 0,"
            + "vehicle_kills INT NOT NULL DEFAULT 0,"
            + "captures INT NOT NULL DEFAULT 0,"
            + "damage_dealt DOUBLE NOT NULL DEFAULT 0,"
            + "healing_done DOUBLE NOT NULL DEFAULT 0,"
            + "supplies_delivered INT NOT NULL DEFAULT 0,"
            + "longest_kill DOUBLE NOT NULL DEFAULT 0,"
            + "best_kill_streak INT NOT NULL DEFAULT 0,"
            + "matches_played INT NOT NULL DEFAULT 0,"
            + "vehicles_destroyed INT NOT NULL DEFAULT 0,"
            + "air_vehicles_destroyed INT NOT NULL DEFAULT 0,"
            + "team_kills INT NOT NULL DEFAULT 0,"
            + "current_win_streak INT NOT NULL DEFAULT 0,"
            + "best_win_streak INT NOT NULL DEFAULT 0,"
            + "survival_time BIGINT NOT NULL DEFAULT 0,"
            + "headshots INT NOT NULL DEFAULT 0,"
            + "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS player_currency ("
            + "uuid VARCHAR(36) PRIMARY KEY,"
            + "coins BIGINT NOT NULL DEFAULT 0,"
            + "total_earned BIGINT NOT NULL DEFAULT 0,"
            + "total_spent BIGINT NOT NULL DEFAULT 0,"
            + "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS player_xp ("
            + "uuid VARCHAR(36) PRIMARY KEY,"
            + "xp BIGINT NOT NULL DEFAULT 0,"
            + "level INT NOT NULL DEFAULT 1,"
            + "prestige INT NOT NULL DEFAULT 0,"
            + "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS match_history ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "map_name VARCHAR(64) NOT NULL,"
            + "mode VARCHAR(32) NOT NULL,"
            + "team_blue_score INT NOT NULL DEFAULT 0,"
            + "team_red_score INT NOT NULL DEFAULT 0,"
            + "winner VARCHAR(16) NOT NULL,"
            + "duration_seconds INT NOT NULL DEFAULT 0,"
            + "started_at VARCHAR(32),"
            + "ended_at VARCHAR(32))",

            "CREATE TABLE IF NOT EXISTS match_players ("
            + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
            + "match_id BIGINT NOT NULL,"
            + "uuid VARCHAR(36) NOT NULL,"
            + "team VARCHAR(16) NOT NULL,"
            + "kills INT NOT NULL DEFAULT 0,"
            + "deaths INT NOT NULL DEFAULT 0,"
            + "assists INT NOT NULL DEFAULT 0,"
            + "score INT NOT NULL DEFAULT 0,"
            + "vehicle_kills INT NOT NULL DEFAULT 0,"
            + "captures INT NOT NULL DEFAULT 0,"
            + "revives INT NOT NULL DEFAULT 0,"
            + "shots_fired INT NOT NULL DEFAULT 0,"
            + "shots_hit INT NOT NULL DEFAULT 0,"
            + "damage_dealt DOUBLE NOT NULL DEFAULT 0,"
            + "healing_done DOUBLE NOT NULL DEFAULT 0,"
            + "supplies_delivered INT NOT NULL DEFAULT 0,"
            + "longest_kill DOUBLE NOT NULL DEFAULT 0,"
            + "role VARCHAR(32) DEFAULT '',"
            + "squad_id INT NOT NULL DEFAULT 0,"
            + "was_squad_leader BOOLEAN NOT NULL DEFAULT FALSE,"
            + "vehicles_destroyed INT NOT NULL DEFAULT 0,"
            + "air_vehicles_destroyed INT NOT NULL DEFAULT 0,"
            + "headshots INT NOT NULL DEFAULT 0,"
            + "FOREIGN KEY (match_id) REFERENCES match_history(id) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS skin_definitions ("
            + "skin_id VARCHAR(64) PRIMARY KEY, name VARCHAR(64) NOT NULL, description TEXT, "
            + "slot_type VARCHAR(32) NOT NULL, weapon_tag VARCHAR(32) NOT NULL DEFAULT 'any', "
            + "rarity VARCHAR(16) NOT NULL DEFAULT 'COMMON', model_path VARCHAR(255), image_url VARCHAR(255), "
            + "price INT NOT NULL DEFAULT 0, "
            + "enabled BOOLEAN NOT NULL DEFAULT TRUE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)",

            "CREATE TABLE IF NOT EXISTS case_definitions ("
            + "case_id VARCHAR(64) PRIMARY KEY, name VARCHAR(64) NOT NULL, description TEXT, "
            + "price_coins INT NOT NULL DEFAULT 0, icon_path VARCHAR(255), "
            + "enabled BOOLEAN NOT NULL DEFAULT TRUE, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)",

            "CREATE TABLE IF NOT EXISTS case_loot ("
            + "case_id VARCHAR(64), skin_id VARCHAR(64), weight INT NOT NULL DEFAULT 100, "
            + "is_guaranteed BOOLEAN NOT NULL DEFAULT FALSE, "
            + "PRIMARY KEY (case_id, skin_id), "
            + "FOREIGN KEY (case_id) REFERENCES case_definitions(case_id) ON DELETE CASCADE, "
            + "FOREIGN KEY (skin_id) REFERENCES skin_definitions(skin_id) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS reward_config ("
            + "action VARCHAR(32) PRIMARY KEY, xp_reward BIGINT NOT NULL DEFAULT 0, "
            + "coins_reward BIGINT NOT NULL DEFAULT 0, score_reward INT NOT NULL DEFAULT 0)",

            "CREATE TABLE IF NOT EXISTS kit_definitions ("
            + "faction VARCHAR(32) NOT NULL, kit_name VARCHAR(32) NOT NULL, "
            + "category VARCHAR(32) NOT NULL DEFAULT 'INFANTRY', "
            + "description TEXT, "
            + "leader_only BOOLEAN NOT NULL DEFAULT FALSE, "
            + "max_per_team INT NOT NULL DEFAULT -1, "
            + "max_per_squad INT NOT NULL DEFAULT -1, "
            + "min_squad_players INT NOT NULL DEFAULT 0, "
            + "items JSON NOT NULL, slot_skins JSON, "
            + "PRIMARY KEY (faction, kit_name))",

            "CREATE TABLE IF NOT EXISTS voice_mutes ("
            + "uuid VARCHAR(36) PRIMARY KEY, "
            + "muted_by_uuid VARCHAR(36) NOT NULL, "
            + "muted_by_nickname VARCHAR(64) NOT NULL, "
            + "reason VARCHAR(256) DEFAULT '', "
            + "muted_at BIGINT NOT NULL, "
            + "expires_at BIGINT NOT NULL DEFAULT 0, "
            + "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS chat_mutes ("
            + "uuid VARCHAR(36) PRIMARY KEY, "
            + "muted_by_uuid VARCHAR(36) NOT NULL, "
            + "muted_by_nickname VARCHAR(64) NOT NULL, "
            + "reason VARCHAR(256) DEFAULT '', "
            + "muted_at BIGINT NOT NULL, "
            + "expires_at BIGINT NOT NULL DEFAULT 0, "
            + "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS ip_confirmations ("
            + "id INT AUTO_INCREMENT PRIMARY KEY, "
            + "player_uuid VARCHAR(36) NOT NULL, "
            + "new_ip VARCHAR(45) NOT NULL, "
            + "status ENUM('pending','allow','deny') NOT NULL DEFAULT 'pending', "
            + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
            + "responded_at DATETIME NULL, "
            + "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS ip_blocks ("
            + "id INT AUTO_INCREMENT PRIMARY KEY, "
            + "ip VARCHAR(45) NOT NULL, "
            + "blocked_until DATETIME NOT NULL, "
            + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)",

            "CREATE TABLE IF NOT EXISTS hwid_bans ("
            + "hwid VARCHAR(255) PRIMARY KEY, "
            + "reason VARCHAR(256), "
            + "banned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)",

            "CREATE TABLE IF NOT EXISTS player_owned_skins ("
            + "uuid VARCHAR(36) NOT NULL, skin_id VARCHAR(64) NOT NULL, "
            + "PRIMARY KEY (uuid, skin_id), "
            + "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE, "
            + "FOREIGN KEY (skin_id) REFERENCES skin_definitions(skin_id) ON DELETE CASCADE)",

            "CREATE TABLE IF NOT EXISTS player_equipped_skins ("
            + "uuid VARCHAR(36) NOT NULL, weapon_tag VARCHAR(32) NOT NULL, "
            + "skin_id VARCHAR(64) NOT NULL, "
            + "PRIMARY KEY (uuid, weapon_tag), "
            + "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE, "
            + "FOREIGN KEY (skin_id) REFERENCES skin_definitions(skin_id) ON DELETE CASCADE)"
        };
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            for (String sql : tables) {
                s.execute(sql);
            }
            s.execute("INSERT IGNORE INTO reward_config (action, xp_reward, coins_reward, score_reward) VALUES "
                    + "('KILL', 50, 10, 100), ('ASSIST', 25, 5, 25), ('VEHICLE_KILL', 150, 30, 150), "
                    + "('CAPTURE', 100, 25, 200), ('REVIVE', 75, 15, 75), ('WIN', 200, 50, 0), "
                    + "('LOSS', 100, 20, 0), ('TIME_MINUTE', 10, 0, 0), ('HEADSHOT', 25, 5, 50), "
                    + "('TEAMKILL', -50, -10, -50)");
            log.info("Database tables verified");

            // Migration: rebuild account FK tables (old INT FK -> new UUID FK)
            try (Statement d = c.createStatement()) {
                d.execute("DROP TABLE IF EXISTS broadcast_log");
                d.execute("DROP TABLE IF EXISTS player_logs");
                d.execute("DROP TABLE IF EXISTS password_resets");
                d.execute("DROP TABLE IF EXISTS twofa_codes");
                // d.execute("DROP TABLE IF EXISTS sessions"); // НЕ ДРОПАТЬ — теряются сессии
                d.execute("DROP TABLE IF EXISTS trusted_ips");
                d.execute("DROP TABLE IF EXISTS accounts");
            } catch (Exception ignored) {}

            // Migration: sessions v2 columns
            String[] sessionMigrations = {
                "ALTER TABLE sessions ADD COLUMN access_token VARCHAR(128) DEFAULT NULL",
                "ALTER TABLE sessions ADD COLUMN refresh_token VARCHAR(128) DEFAULT NULL",
                "ALTER TABLE sessions ADD COLUMN session_key VARCHAR(64) DEFAULT NULL",
                "ALTER TABLE sessions ADD COLUMN hwid VARCHAR(64) DEFAULT NULL",
                "ALTER TABLE sessions ADD COLUMN hmac_secret VARCHAR(64) DEFAULT NULL",
                "ALTER TABLE sessions ADD COLUMN last_heartbeat DATETIME DEFAULT NULL",
                "ALTER TABLE sessions ADD INDEX idx_access_token (access_token)",
                "ALTER TABLE sessions ADD INDEX idx_refresh_token (refresh_token)"
            };
            for (String sql : sessionMigrations) {
                try { s.execute(sql); } catch (Exception ignored) {}
            }

            // Migration: drop server_tokens table (replaced by auth_tokens)
            try { s.execute("DROP TABLE IF EXISTS server_tokens"); } catch (Exception ignored) {}
            // Migration: add accountId as permanent player identity
            try { s.execute("ALTER TABLE players ADD COLUMN id BIGINT AUTO_INCREMENT UNIQUE FIRST"); } catch (Exception ignored) {}

            // Migration: add missing columns to player_stats (safe, ignores duplicates)
            String[] migrations = {
                "ALTER TABLE player_stats ADD COLUMN vehicle_kills INT NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN captures INT NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN damage_dealt DOUBLE NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN healing_done DOUBLE NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN supplies_delivered INT NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN longest_kill DOUBLE NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN best_kill_streak INT NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN matches_played INT NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN vehicles_destroyed INT NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN air_vehicles_destroyed INT NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN team_kills INT NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN current_win_streak INT NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN best_win_streak INT NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN survival_time BIGINT NOT NULL DEFAULT 0",
                "ALTER TABLE player_stats ADD COLUMN headshots INT NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN assists INT NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN vehicle_kills INT NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN captures INT NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN revives INT NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN shots_fired INT NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN shots_hit INT NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN damage_dealt DOUBLE NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN healing_done DOUBLE NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN supplies_delivered INT NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN longest_kill DOUBLE NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN squad_id INT NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN was_squad_leader BOOLEAN NOT NULL DEFAULT FALSE",
                "ALTER TABLE match_players ADD COLUMN vehicles_destroyed INT NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN air_vehicles_destroyed INT NOT NULL DEFAULT 0",
                "ALTER TABLE match_players ADD COLUMN headshots INT NOT NULL DEFAULT 0",
                "ALTER TABLE players ADD COLUMN telegram_id BIGINT DEFAULT NULL",
                "ALTER TABLE players ADD COLUMN login VARCHAR(32) DEFAULT NULL",
                "ALTER TABLE players ADD COLUMN email VARCHAR(255) DEFAULT NULL",
                "ALTER TABLE players ADD COLUMN password_hash VARCHAR(255) DEFAULT NULL",
                "ALTER TABLE players ADD COLUMN hwid VARCHAR(255) DEFAULT NULL",
                "ALTER TABLE players ADD COLUMN privacy_policy_accepted BOOLEAN NOT NULL DEFAULT FALSE",
                "ALTER TABLE players ADD COLUMN launcher_2fa_enabled BOOLEAN NOT NULL DEFAULT FALSE",
                "ALTER TABLE players ADD COLUMN last_ip VARCHAR(45) DEFAULT NULL",
                    "ALTER TABLE players ADD INDEX idx_players_telegram (telegram_id)",
                "ALTER TABLE players ADD INDEX idx_players_login (login)",
                "ALTER TABLE password_resets ADD COLUMN player_uuid VARCHAR(36) NOT NULL",
                "ALTER TABLE players ADD COLUMN banned_until DATETIME DEFAULT NULL",
                "ALTER TABLE hwid_bans ADD COLUMN banned_until DATETIME DEFAULT NULL",
                "ALTER TABLE hwid_bans ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP",
                "ALTER TABLE ip_blocks ADD COLUMN reason VARCHAR(256) DEFAULT NULL",
                "ALTER TABLE skin_definitions ADD COLUMN price INT NOT NULL DEFAULT 0"
            };
            for (String sql : migrations) {
                try { s.execute(sql); } catch (Exception ignored) {}
            }

            // Recreate account FK tables after drop
            String[] accountTables = {
                "CREATE TABLE IF NOT EXISTS trusted_ips ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, "
                + "ip VARCHAR(45) NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "UNIQUE KEY uk_player_ip (player_uuid, ip), "
                + "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE)",

                "CREATE TABLE IF NOT EXISTS sessions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, "
                + "token VARCHAR(128) NOT NULL UNIQUE, "
                + "access_token VARCHAR(128) DEFAULT NULL UNIQUE, "
                + "refresh_token VARCHAR(128) DEFAULT NULL UNIQUE, "
                + "session_key VARCHAR(64) DEFAULT NULL, "
                + "hwid VARCHAR(64) DEFAULT NULL, "
                + "hmac_secret VARCHAR(64) DEFAULT NULL, "
                + "ip VARCHAR(45) DEFAULT NULL, "
                + "last_heartbeat DATETIME DEFAULT NULL, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, expires_at DATETIME NOT NULL, "
                + "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE)",

                "CREATE TABLE IF NOT EXISTS twofa_codes ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, "
                + "code VARCHAR(6) NOT NULL, ip VARCHAR(45) DEFAULT NULL, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "expires_at DATETIME NOT NULL, used BOOLEAN NOT NULL DEFAULT FALSE, "
                + "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE)",

                "CREATE TABLE IF NOT EXISTS password_resets ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36) NOT NULL, "
                + "admin_uuid VARCHAR(36) DEFAULT NULL, "
                + "status ENUM('pending', 'approved', 'rejected') NOT NULL DEFAULT 'pending', "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, resolved_at DATETIME DEFAULT NULL, "
                + "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE, "
                + "FOREIGN KEY (admin_uuid) REFERENCES players(uuid) ON DELETE SET NULL)",

                "CREATE TABLE IF NOT EXISTS player_logs ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, player_uuid VARCHAR(36) DEFAULT NULL, "
                + "action VARCHAR(64) NOT NULL, ip VARCHAR(45) DEFAULT NULL, "
                + "details TEXT DEFAULT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE SET NULL)",

                "CREATE TABLE IF NOT EXISTS broadcast_log ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, admin_uuid VARCHAR(36) NOT NULL, "
                + "message TEXT NOT NULL, recipient_count INT NOT NULL DEFAULT 0, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (admin_uuid) REFERENCES players(uuid) ON DELETE CASCADE)"
            };
            for (String sql : accountTables) {
                try { s.execute(sql); } catch (Exception ignored) {}
            }

            // Launcher tables
            String[] launcherTables = {
                "CREATE TABLE IF NOT EXISTS launcher_versions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, version VARCHAR(16) NOT NULL, "
                + "url VARCHAR(512) NOT NULL, sha256 VARCHAR(64) NOT NULL, "
                + "changelog TEXT, mandatory BOOLEAN NOT NULL DEFAULT TRUE, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP)",

                "CREATE TABLE IF NOT EXISTS file_manifests ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, file_path VARCHAR(512) NOT NULL, "
                + "file_size BIGINT NOT NULL, sha256 VARCHAR(64) NOT NULL, "
                + "version VARCHAR(32) NOT NULL DEFAULT 'latest', "
                + "category VARCHAR(32) NOT NULL DEFAULT 'game', "
                + "mod_name VARCHAR(128) DEFAULT NULL, "
                + "mod_description TEXT DEFAULT NULL, "
                + "mod_optional BOOLEAN NOT NULL DEFAULT FALSE, "
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "UNIQUE KEY uk_file_version (file_path(255), version(32), category(32)))",

                "CREATE TABLE IF NOT EXISTS hwid_bans ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, hwid VARCHAR(64) NOT NULL, "
                + "reason TEXT, banned_by VARCHAR(36), "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "INDEX idx_hwid (hwid))",

                "CREATE TABLE IF NOT EXISTS hwid_history ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, account_uuid VARCHAR(36) NOT NULL, "
                + "hwid VARCHAR(64) NOT NULL, hwid_components TEXT, "
                + "pc_name VARCHAR(128), ip VARCHAR(45), "
                + "flags INT NOT NULL DEFAULT 0, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (account_uuid) REFERENCES players(uuid) ON DELETE CASCADE)",

                "CREATE TABLE IF NOT EXISTS launcher_logs ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, account_uuid VARCHAR(36) DEFAULT NULL, "
                + "level VARCHAR(8) NOT NULL DEFAULT 'INFO', message TEXT NOT NULL, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (account_uuid) REFERENCES players(uuid) ON DELETE SET NULL)",

                "CREATE TABLE IF NOT EXISTS auth_tokens ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "account_id BIGINT NOT NULL, "
                + "token VARCHAR(64) NOT NULL UNIQUE, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "expires_at DATETIME NOT NULL, "
                + "revoked BOOLEAN NOT NULL DEFAULT FALSE, "
                + "last_used_at DATETIME DEFAULT NULL, "
                + "FOREIGN KEY (account_id) REFERENCES players(id) ON DELETE CASCADE)",

                "CREATE TABLE IF NOT EXISTS punishment_history ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "type VARCHAR(32) NOT NULL, "
                + "reason TEXT, "
                + "admin_uuid VARCHAR(36) DEFAULT NULL, "
                + "duration_minutes INT DEFAULT NULL, "
                + "expires_at DATETIME DEFAULT NULL, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "INDEX idx_ph_player (player_uuid), "
                + "INDEX idx_ph_type (type), "
                + "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE)",

                "CREATE TABLE IF NOT EXISTS anticheat_blacklist ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "kind VARCHAR(64) NOT NULL DEFAULT '', "
                + "pattern VARCHAR(512) NOT NULL, "
                + "match_type VARCHAR(16) NOT NULL DEFAULT 'substring', "
                + "hash VARCHAR(64) NOT NULL DEFAULT '', "
                + "severity INT NOT NULL DEFAULT 0, "
                + "enabled BOOLEAN NOT NULL DEFAULT TRUE, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, "
                + "UNIQUE KEY uk_ac_pattern (pattern(255)))",

                "CREATE TABLE IF NOT EXISTS anticheat_sessions ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "player_uuid VARCHAR(36) NOT NULL, "
                + "launch_token VARCHAR(64) NOT NULL UNIQUE, "
                + "hwid VARCHAR(64) DEFAULT NULL, "
                + "last_heartbeat DATETIME DEFAULT NULL, "
                + "revoked TINYINT NOT NULL DEFAULT 0, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "expires_at DATETIME NOT NULL, "
                + "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE CASCADE, "
                + "INDEX idx_ac_sess (player_uuid, expires_at))",

                "CREATE TABLE IF NOT EXISTS anticheat_detections ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "player_uuid VARCHAR(36) DEFAULT NULL, "
                + "launch_token VARCHAR(64) DEFAULT NULL, "
                + "source VARCHAR(16) NOT NULL DEFAULT 'launcher', "
                + "type VARCHAR(32) NOT NULL, "
                + "signature TEXT, "
                + "details TEXT, "
                + "severity INT NOT NULL DEFAULT 0, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE SET NULL, "
                + "INDEX idx_ac_det_created (created_at), "
                + "INDEX idx_ac_det_token (launch_token))",

                "CREATE TABLE IF NOT EXISTS anticheat_screenshots ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                + "player_uuid VARCHAR(36) DEFAULT NULL, "
                + "file_path VARCHAR(512) NOT NULL, "
                + "width INT NOT NULL DEFAULT 0, "
                + "height INT NOT NULL DEFAULT 0, "
                + "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                + "FOREIGN KEY (player_uuid) REFERENCES players(uuid) ON DELETE SET NULL, "
                + "INDEX idx_ac_shot (created_at))"
            };
            for (String sql : launcherTables) {
                try { s.execute(sql); } catch (Exception ignored) {}
            }

            // ── Миграции античита для существующих БД ──
            migrateAddColumn(s, "anticheat_blacklist", "hash",
                "ALTER TABLE anticheat_blacklist ADD COLUMN hash VARCHAR(64) NOT NULL DEFAULT '' AFTER match_type");
            migrateAddColumn(s, "anticheat_blacklist", "updated_at",
                "ALTER TABLE anticheat_blacklist ADD COLUMN updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER created_at");
            migrateAddColumn(s, "anticheat_sessions", "revoked",
                "ALTER TABLE anticheat_sessions ADD COLUMN revoked TINYINT NOT NULL DEFAULT 0 AFTER last_heartbeat");
            migrateAddColumn(s, "anticheat_detections", "details",
                "ALTER TABLE anticheat_detections ADD COLUMN details TEXT AFTER signature");
        } catch (Exception e) {
            log.warn("Could not create tables (may already exist): {}", e.getMessage());
        }
    }

    /** Добавляет колонку, если её ещё нет (information_schema). Ошибки не роняют старт. */
    private static void migrateAddColumn(java.sql.Statement s, String table, String column, String alterSql) {
        try {
            String db = s.getConnection().getCatalog();
            try (var ps = s.getConnection().prepareStatement(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? AND COLUMN_NAME = ?")) {
                ps.setString(1, db);
                ps.setString(2, table);
                ps.setString(3, column);
                try (var rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) == 0) {
                        s.execute(alterSql);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("migrate column {}.{} failed: {}", table, column, e.getMessage());
        }
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Database pool shut down");
        }
    }
}
