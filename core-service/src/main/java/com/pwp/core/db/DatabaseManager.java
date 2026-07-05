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
            + "first_join DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "last_join DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,"
            + "donate_tier VARCHAR(32) DEFAULT NULL,"
            + "role VARCHAR(32) DEFAULT 'PLAYER',"
            + "is_banned BOOLEAN NOT NULL DEFAULT FALSE,"
            + "ban_reason TEXT DEFAULT NULL)",

            "CREATE TABLE IF NOT EXISTS player_stats ("
            + "uuid VARCHAR(36) PRIMARY KEY,"
            + "kills INT NOT NULL DEFAULT 0, deaths INT NOT NULL DEFAULT 0,"
            + "assists INT NOT NULL DEFAULT 0,"
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
            + "hub_destructions INT NOT NULL DEFAULT 0,"
            + "base_defends INT NOT NULL DEFAULT 0,"
            + "vehicles_destroyed INT NOT NULL DEFAULT 0,"
            + "air_vehicles_destroyed INT NOT NULL DEFAULT 0,"
            + "team_kills INT NOT NULL DEFAULT 0,"
            + "match_mvp_count INT NOT NULL DEFAULT 0,"
            + "current_win_streak INT NOT NULL DEFAULT 0,"
            + "best_win_streak INT NOT NULL DEFAULT 0,"
            + "survival_time BIGINT NOT NULL DEFAULT 0,"
            + "distance_traveled DOUBLE NOT NULL DEFAULT 0,"
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
            + "FOREIGN KEY (uuid) REFERENCES players(uuid) ON DELETE CASCADE)"
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
        } catch (Exception e) {
            log.warn("Could not create tables (may already exist): {}", e.getMessage());
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
