package com.pwp.core.db;

import com.pwp.core.CoreApplication;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

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
