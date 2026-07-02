package com.pwp.core;

import com.pwp.core.api.*;
import com.pwp.core.auth.AuthMiddleware;
import com.pwp.core.db.DatabaseManager;
import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;

public class CoreApplication {

    private static final Logger log = LoggerFactory.getLogger(CoreApplication.class);

    public static Config config;

    public static void main(String[] args) {
        log.info("Starting PWP Core Service...");

        loadConfig(args.length > 0 ? args[0] : "config.json");

        DatabaseManager.init(config.database);

        int port = config.server.port;
        Javalin app = Javalin.create(cfg -> {
            cfg.showJavalinBanner = false;
            com.pwp.core.api.GsonMapper.apply(cfg);
        });

        app.before("/api/*", ctx -> AuthMiddleware.handle(ctx, config.api));

        app.get("/api/v1/health", ctx -> ctx.json("{\"status\":\"ok\"}"));

        new PlayerController(app, config);
        new CurrencyController(app);
        new XpController(app);
        new CosmeticsController(app);
        new MatchController(app);
        new ShopController(app);
        new DonationController(app);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down Core Service...");
            app.stop();
            DatabaseManager.shutdown();
        }));

        app.start(port);
        log.info("PWP Core Service running on port {}", port);
    }

    private static void loadConfig(String path) {
        try (FileReader reader = new FileReader(path)) {
            config = new com.google.gson.Gson().fromJson(reader, Config.class);
        } catch (Exception e) {
            log.warn("Could not load config file '{}', using defaults", path);
            config = new Config();
        }
    }

    public static class Config {
        public ServerConfig server = new ServerConfig();
        public DatabaseConfig database = new DatabaseConfig();
        public ApiConfig api = new ApiConfig();
        public LoggingConfig logging = new LoggingConfig();
    }

    public static class ServerConfig {
        public int port = 8080;
        public String host = "0.0.0.0";
    }

    public static class DatabaseConfig {
        public String host = "127.0.0.1";
        public int port = 3306;
        public String name = "pwp_core";
        public String user = "root";
        public String password = "";
        public int poolSize = 10;
        public long maxLifetimeMs = 1800000;
    }

    public static class ApiConfig {
        public String[] keys = {"pwp_server_key_change_me"};
        public int rateLimitPerMinute = 100;
        public int rateLimitPerMinuteAdmin = 10;
    }

    public static class LoggingConfig {
        public String level = "INFO";
        public String file = "logs/core-service.log";
    }
}
