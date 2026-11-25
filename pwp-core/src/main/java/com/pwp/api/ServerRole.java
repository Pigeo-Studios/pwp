package com.pwp.api;

import com.pwp.api.config.ServerSettings;
import com.pwp.api.config.ServerSettingsFile;
import java.nio.file.Path;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Единый конфиг сервера {@code config/pwp-server.json} ({@link ServerSettings}) и роль сервера: лобби или
 * матч. Матч-серверы лобби поднимает в папках {@code match_NN} — при {@code "role": "auto"} это и есть
 * признак; {@code lobby}/{@code match} задают роль явно. Файл создаётся только на выделенном сервере.
 */
public final class ServerRole {
    private static final Logger LOG = LoggerFactory.getLogger(ServerRole.class);
    private static final ServerSettingsFile FILE = new ServerSettingsFile(Path.of("config"));
    private static volatile boolean loaded;

    private ServerRole() {}

    /** Настройки из файла (загружаются при первом обращении). */
    public static ServerSettings settings() {
        if (!loaded) {
            synchronized (ServerRole.class) {
                if (!loaded) {
                    FILE.load(FMLEnvironment.dist.isDedicatedServer(), LOG::warn);
                    loaded = true;
                }
            }
        }
        return FILE.get();
    }

    /** Файл — для записи «навсегда» (лобби: кнопка OWNER в админ-панели). */
    public static ServerSettingsFile file() {
        settings();
        return FILE;
    }

    /** Матч-сервер (папка {@code match_*} или {@code "role": "match"}). */
    public static boolean isMatchServer() {
        Path dir = Path.of("").toAbsolutePath().getFileName();
        return settings().isMatchServer(dir == null ? null : dir.toString());
    }
}
