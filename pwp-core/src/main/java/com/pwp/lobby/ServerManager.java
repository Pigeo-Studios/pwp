package com.pwp.lobby;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pwp.api.config.MatchConfig;
import com.pwp.api.lobby.MatchHandoff;
import com.pwp.api.time.GameClock;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServerManager {

    private static final Logger log = LoggerFactory.getLogger(ServerManager.class);

    // Шаблон и порты матчей — из config/pwp-server.json (matchServers.template/basePort; раньше зашиты здесь)
    private static final long SERVER_START_TIMEOUT_MS = 300_000;

    private static final Map<Integer, ServerInstance> servers = new ConcurrentHashMap<>();
    private static int nextServerId = 1;

    private static final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "match-server-worker");
        t.setDaemon(true);
        return t;
    });

    private static class ServerInstance {
        final int serverId;
        final int port;
        final Path directory;
        final long createdAt;
        volatile Process process;
        volatile boolean booted;
        volatile boolean failed;
        volatile String error;
        volatile String phase; // "template","map","config","booting", or null when done
        volatile long phaseStartedAt;
        volatile long logDoneAt; // when "Done" was found in log file (0 = not yet)

        ServerInstance(int serverId, int port, Path directory) {
            this.serverId = serverId;
            this.port = port;
            this.directory = directory;
            this.createdAt = GameClock.realMillis();
        }
    }

    public static class StartResult {
        public int serverId;
        public int port;
        public String error;
    }

    public static StartResult startMatchServer(String mapName, int maxPlayers, String mapWorldPath) {
        return startMatchServer(mapName, maxPlayers, mapWorldPath, null, false);
    }

    /**
     * Копия шаблона + мира и запуск процесса (в фоне). {@code awaitSettings} — предпрогрев (этап 5): пишется
     * {@link MatchHandoff#AWAIT_FLAG}, матч-сервер не начинает фазу ожидания до файла настроек лобби.
     */
    public static StartResult startMatchServer(
            String mapName,
            int maxPlayers,
            String mapWorldPath,
            Consumer<Path> configCustomizer,
            boolean awaitSettings) {
        StartResult result = new StartResult();

        Path templatePath = Paths.get(com.pwp.api.ServerRole.settings().matchTemplate())
                .toAbsolutePath()
                .normalize();
        if (!Files.exists(templatePath)) {
            result.error = "Template not found: " + templatePath;
            return result;
        }

        int serverId = nextServerId++;
        int port = com.pwp.api.ServerRole.settings().basePort() + serverId - 1;
        String dirName = "match_" + String.format("%02d", serverId);
        Path serverDir = Paths.get(dirName).toAbsolutePath();

        result.serverId = serverId;
        result.port = port;

        // Stop any existing server on this port (zombie from previous run)
        for (ServerInstance si : servers.values()) {
            if (si.port == port && si.serverId != serverId) {
                stopServer(si.serverId);
            }
        }

        // Register placeholder immediately so tick() doesn't clean it up
        ServerInstance instance = new ServerInstance(serverId, port, serverDir);
        servers.put(serverId, instance);

        backgroundExecutor.submit(() -> {
            try {
                // Зомби прошлого запуска мог остаться висеть на порту (окно "PWP Match N") —
                // освобождаем порт до старта, иначе сервер упадёт с "port in use"
                killPort(port);

                if (Files.exists(serverDir)) deleteDirectory(serverDir);

                instance.phase = "template";
                instance.phaseStartedAt = GameClock.realMillis();
                log.info("Copying template to {}...", dirName);
                robocopy(templatePath, serverDir);

                if (mapWorldPath != null && !mapWorldPath.isEmpty()) {
                    Path mapWorldDir = Paths.get(mapWorldPath);
                    if (Files.isDirectory(mapWorldDir)) {
                        instance.phase = "map";
                        instance.phaseStartedAt = GameClock.realMillis();
                        log.info("Copying map world '{}'...", mapName);
                        Path matchWorldDir = serverDir.resolve(mapName);
                        Files.createDirectories(matchWorldDir);
                        robocopy(mapWorldDir, matchWorldDir);
                    }
                }

                instance.phase = "config";
                instance.phaseStartedAt = GameClock.realMillis();

                // файлы канала лобби↔матч из шаблона/прошлого запуска не должны «дожить» до этого матча
                Files.deleteIfExists(serverDir.resolve(MatchHandoff.SETTINGS_FILE));
                Files.deleteIfExists(serverDir.resolve(MatchHandoff.STATUS_FILE));
                Files.deleteIfExists(serverDir.resolve(MatchHandoff.AWAIT_FLAG));
                if (awaitSettings) Files.writeString(serverDir.resolve(MatchHandoff.AWAIT_FLAG), "prewarm");

                // Apply custom config overrides (factions, mode, etc.)
                if (configCustomizer != null) {
                    try {
                        configCustomizer.accept(serverDir);
                    } catch (Exception e) {
                        log.error("Config customization failed for match {}: {}", serverId, e.getMessage());
                    }
                }

                // Read match_config.json for server and SBW settings
                Path matchConfigPath = serverDir.resolve("match_config.json");
                MatchConfig matchConfig = null;
                if (Files.exists(matchConfigPath)) {
                    try {
                        // Единый POJO match_config.json (pwp-api). Отсутствующая секция = null, как раньше:
                        // тогда view-distance и superbwarfare-server.toml не пишутся (дефолты сервера/мода)
                        var raw = new Gson().fromJson(Files.readString(matchConfigPath), JsonObject.class);
                        matchConfig = new Gson().fromJson(raw, MatchConfig.class);
                        if (!raw.has("server")) matchConfig.server = null;
                        if (!raw.has("superbwarfare")) matchConfig.superbwarfare = null;
                    } catch (Exception e) {
                        log.warn("Failed to parse match_config.json: {}", e.getMessage());
                    }
                }

                String voidGen =
                        "{\"layers\":[{\"block\":\"minecraft:air\",\"height\":1}],\"biomes\":\"minecraft:the_void\",\"structures\":{\"structures\":{}}}";
                StringBuilder props = new StringBuilder();
                props.append("server-port=").append(port).append("\n");
                props.append("level-name=").append(mapName).append("\n");
                // резервные слоты (этап 10b): сверх публичного лимита — для модераторов и выше;
                // сколько из них публичных, решают очередь лобби и ReservedSlots матч-сервера
                props.append("max-players=")
                        .append(maxPlayers
                                + com.pwp.api.ServerRole.settings().community().reservedSlots())
                        .append("\n");
                props.append("online-mode=true\n");
                props.append("level-type=minecraft:superflat\n");
                props.append("generator-settings=").append(voidGen).append("\n");
                props.append("generate-structures=false\n");
                props.append("spawn-animals=false\n");
                props.append("spawn-monsters=false\n");
                props.append("spawn-npcs=false\n");
                props.append("difficulty=peaceful\n");
                props.append("gamemode=adventure\n");
                props.append("allow-flight=true\n");
                if (matchConfig != null && matchConfig.server != null) {
                    props.append("view-distance=")
                            .append(matchConfig.server.viewDistance)
                            .append("\n");
                    props.append("simulation-distance=")
                            .append(matchConfig.server.simulationDistance)
                            .append("\n");
                    props.append("entity-broadcast-range-percentage=")
                            .append(matchConfig.server.entityBroadcastRange)
                            .append("\n");
                }
                Files.writeString(serverDir.resolve("server.properties"), props.toString());

                // Generate superbwarfare-server.toml from match_config.json
                if (matchConfig != null && matchConfig.superbwarfare != null) {
                    writeSBWServerConfig(serverDir, mapName, matchConfig.superbwarfare);
                }

                // Unique voice chat port for each match server (avoid collision with main server)
                Path vcConfig = serverDir.resolve("config/voicechat/voicechat-server.properties");
                if (Files.exists(vcConfig)) {
                    String vcContent = Files.readString(vcConfig);
                    vcContent = vcContent.replaceAll("port=\\d+", "port=" + (24454 + serverId));
                    Files.writeString(vcConfig, vcContent);
                }

                instance.phase = "booting";
                instance.phaseStartedAt = GameClock.realMillis();
                String title = "PWP Match " + serverId;
                String dir = serverDir.toAbsolutePath().toString();
                File sDir = serverDir.toFile();

                ProcessBuilder pb = new ProcessBuilder(
                        "cmd.exe", "/c", "start", title, "/D", dir, "/WAIT", "cmd", "/c", "run.bat", "nogui");
                pb.directory(sDir);
                pb.environment().put("JAVA_HOME", System.getProperty("java.home"));

                instance.process = pb.start();
                log.info("Match server {} started on port {} (map: {})", serverId, port, mapName);
            } catch (Exception e) {
                log.error("Failed to start match server {}: {}", serverId, e.getMessage());
                instance.failed = true;
                instance.error = e.getMessage();
                instance.phase = null;
                try {
                    if (Files.exists(serverDir)) deleteDirectory(serverDir);
                } catch (IOException ignored) {
                    log.warn("ServerManager.startMatchServer: исключение", ignored);
                }
            }
        });

        return result;
    }

    /** Called from main tick — updates booted status, cleans dead servers, broadcasts progress */
    public static void tick() {
        long now = GameClock.realMillis();
        for (ServerInstance si : servers.values()) {
            if (si.failed || (!si.booted && now - si.createdAt > SERVER_START_TIMEOUT_MS)) {
                if (!si.failed) {
                    log.warn("Server {} timed out after {}ms", si.serverId, SERVER_START_TIMEOUT_MS);
                    si.failed = true;
                    si.error = "start timeout";
                }
                if (si.process != null) si.process.destroyForcibly();
                continue;
            }
            if (si.process == null) continue; // still starting (copying template/map)
            if (!si.process.isAlive() && !si.failed) {
                int exitCode = -1;
                try {
                    exitCode = si.process.exitValue();
                } catch (Exception ignored) {
                    log.warn("ServerManager.tick: исключение", ignored);
                }
                log.warn("Server {} process died prematurely (exitCode={})", si.serverId, exitCode);
                // Read last lines of match server log for diagnostics
                Path logFile = si.directory.resolve("logs/latest.log");
                if (Files.exists(logFile)) {
                    try (BufferedReader br = new BufferedReader(new FileReader(logFile.toFile()))) {
                        java.util.List<String> lines = br.lines().toList();
                        int start = Math.max(0, lines.size() - 30);
                        for (int i = start; i < lines.size(); i++) {
                            log.warn("MATCH_LOG[{}]: {}", si.serverId, lines.get(i));
                        }
                    } catch (Exception ignored) {
                        log.warn("ServerManager.tick: исключение", ignored);
                    }
                } else {
                    log.warn("MATCH_LOG[{}]: logs/latest.log not found", si.serverId);
                }
                si.failed = true;
                si.error = "process died (exit=" + exitCode + ")";
            }
        }

        // Загрузка: строка "Done" в logs/latest.log (раз в секунду — раньше файл читался целиком КАЖДЫЙ тик,
        // а порт проверялся блокирующим connect на главном потоке со стектрейсом в лог на каждый тик)
        if (now - lastBootCheckMs < 1000) return;
        lastBootCheckMs = now;
        for (ServerInstance si : servers.values()) {
            if (!"booting".equals(si.phase) || si.booted || si.failed) continue;
            Path logFile = si.directory.resolve("logs/latest.log");
            if (si.logDoneAt == 0 && Files.exists(logFile)) {
                try (BufferedReader br =
                        Files.newBufferedReader(logFile, java.nio.charset.StandardCharsets.ISO_8859_1)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.contains("Done") && line.contains("For help")) {
                            si.logDoneAt = now;
                            log.info("Server {} detected 'Done' in log", si.serverId);
                            break;
                        }
                    }
                } catch (IOException e) {
                    com.pwp.api.util.LogOnce.warn(log, "match-log-" + si.serverId, "Cannot read " + logFile, e);
                }
            }
            if (si.logDoneAt != 0 && now - si.logDoneAt >= 2000) {
                si.booted = true;
                si.phase = null;
                log.info("Server {} fully booted", si.serverId);
            }
        }
    }

    private static long lastBootCheckMs;

    /** Стадия подготовки процесса: template/map/config/booting; null — загружен или нет такого. */
    public static String getStage(int serverId) {
        ServerInstance si = servers.get(serverId);
        return si != null ? si.phase : null;
    }

    /** Текст ошибки процесса (таймаут/падение) или null. */
    public static String getError(int serverId) {
        ServerInstance si = servers.get(serverId);
        return si != null ? si.error : null;
    }

    /** Отчёт матч-сервера ({@link MatchHandoff#STATUS_FILE}); null — ещё нет/читается в момент записи. */
    public static MatchHandoff.Status readStatus(int serverId) {
        Path dir = getServerDirectory(serverId);
        if (dir == null) return null;
        Path f = dir.resolve(MatchHandoff.STATUS_FILE);
        if (!Files.isRegularFile(f)) return null;
        try {
            return MatchHandoff.Status.fromJson(
                    com.google.gson.JsonParser.parseString(Files.readString(f)).getAsJsonObject());
        } catch (IOException | RuntimeException e) {
            return null; // файл переписывается атомарно, но на Windows move может совпасть с чтением
        }
    }

    /** Записать настройки лобби в папку матча (атомарно). false — не удалось. */
    public static boolean writeSettings(int serverId, MatchHandoff.Settings settings) {
        Path dir = getServerDirectory(serverId);
        if (dir == null) return false;
        try {
            Path tmp = dir.resolve(MatchHandoff.SETTINGS_FILE + ".tmp");
            Files.writeString(tmp, settings.toJson().toString());
            Files.move(tmp, dir.resolve(MatchHandoff.SETTINGS_FILE), StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            log.error("Cannot write lobby settings for match {}", serverId, e);
            return false;
        }
    }

    public static void stopServer(int serverId) {
        ServerInstance si = servers.remove(serverId);
        if (si == null) return;
        if (si.process != null && si.process.isAlive()) {
            si.process.destroy();
            try {
                si.process.waitFor(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (si.process.isAlive()) si.process.destroyForcibly();
        }
        try {
            deleteDirectory(si.directory);
            log.info("Cleaned up {}", si.directory.getFileName());
        } catch (IOException ignored) {
            log.warn("ServerManager.stopServer: исключение", ignored);
        }
    }

    public static ServerInstance getServer(int serverId) {
        return servers.get(serverId);
    }

    public static boolean isAlive(int serverId) {
        ServerInstance si = servers.get(serverId);
        if (si == null || si.failed) return false;
        if (si.process != null && !si.process.isAlive()) return false;
        return true;
    }

    public static boolean isBooted(int serverId) {
        ServerInstance si = servers.get(serverId);
        return si != null && si.booted;
    }

    public static int getPort(int serverId) {
        ServerInstance si = servers.get(serverId);
        return si != null ? si.port : -1;
    }

    public static Path getServerDirectory(int serverId) {
        ServerInstance si = servers.get(serverId);
        return si != null ? si.directory : null;
    }

    public static int getActiveCount() {
        return (int) servers.values().stream().filter(si -> !si.failed).count();
    }

    // --- private helpers ---

    private static void writeSBWServerConfig(Path serverDir, String levelName, MatchConfig.Superbwarfare cfg)
            throws IOException {
        Path scDir = serverDir.resolve(levelName).resolve("serverconfig");
        Files.createDirectories(scDir);
        String toml = "[vehicle]\n\tvehicle_info_display_distance = " + cfg.vehicleInfoDisplayDistance
                + "\n\tvehicle_chunk_loading = " + cfg.vehicleChunkLoading
                + "\n\n[projectile]\n\tprojectile_chunk_loading = " + cfg.projectileChunkLoading + "\n";
        Files.writeString(scDir.resolve("superbwarfare-server.toml"), toml);
        log.info("Generated superbwarfare-server.toml for {}", levelName);
    }

    /** Убить процесс, слушающий порт (зомби матч-сервер от прошлого запуска). */
    private static void killPort(int port) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe",
                    "/c",
                    "for /f \"tokens=5\" %a in ('netstat -ano ^| findstr \":" + port
                            + " \" ^| findstr \"LISTENING\"') do @taskkill /F /PID %a");
            Process p = pb.start();
            p.waitFor(30, TimeUnit.SECONDS);
            log.info("Port {} freed (killed zombie if any)", port);
        } catch (Exception e) {
            log.warn("Failed to free port {}: {}", port, e.getMessage());
        }
    }

    private static void robocopy(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        String src = source.toAbsolutePath() + "\\";
        String dst = target.toAbsolutePath() + "\\";
        ProcessBuilder pb = new ProcessBuilder(
                "cmd.exe", "/c", "robocopy", src, dst, "/E", "/MT:2", "/NFL", "/NDL", "/NJH", "/NJS", "/R:0", "/W:0");
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while (br.readLine() != null) {}
        }
        try {
            if (!p.waitFor(5, TimeUnit.MINUTES)) {
                p.destroyForcibly();
                throw new IOException("robocopy timed out");
            }
            if (p.exitValue() > 7) throw new IOException("robocopy failed: " + p.exitValue());
        } catch (InterruptedException e) {
            p.destroyForcibly();
            throw new IOException("robocopy interrupted", e);
        }
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
