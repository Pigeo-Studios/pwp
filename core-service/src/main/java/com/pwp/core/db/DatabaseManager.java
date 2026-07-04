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
            + "PRIMARY KEY (faction, kit_name))"
        };
        try (Connection c = getConnection(); Statement s = c.createStatement()) {
            for (String sql : tables) {
                s.execute(sql);
            }
            s.execute("INSERT IGNORE INTO reward_config (action, xp_reward, coins_reward, score_reward) VALUES "
                    + "('KILL', 50, 10, 100), ('ASSIST', 25, 5, 25), ('VEHICLE_KILL', 150, 30, 150), "
                    + "('CAPTURE', 100, 25, 200), ('REVIVE', 75, 15, 75), ('WIN', 200, 50, 0), "
                    + "('LOSS', 100, 20, 0), ('TIME_MINUTE', 10, 0, 0), ('HEADSHOT', 25, 5, 50)");
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
