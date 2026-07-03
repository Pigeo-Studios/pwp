package com.pwp.lobby;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerManager {

    private static final Logger log = LoggerFactory.getLogger(ServerManager.class);

    private static final String TEMPLATE_PATH = "../PWP-Server/match_template";
    private static final int BASE_PORT = 25566;
    private static final int MAX_SERVERS = 10;
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
        volatile long logDoneAt;  // when "Done" was found in log file (0 = not yet)
        volatile long portOpenAt; // when port was detected open (0 = not yet)
        long lastBroadcastMs;

        ServerInstance(int serverId, int port, Path directory) {
            this.serverId = serverId;
            this.port = port;
            this.directory = directory;
            this.createdAt = System.currentTimeMillis();
        }
    }

    public static class StartResult {
        public int serverId;
        public int port;
        public String error;
    }

    public static StartResult startMatchServer(String mapName, int maxPlayers, String mapWorldPath) {
        StartResult result = new StartResult();

        Path templatePath = Paths.get(TEMPLATE_PATH).toAbsolutePath().normalize();
        if (!Files.exists(templatePath)) {
            result.error = "Template not found: " + templatePath;
            return result;
        }

        int serverId = nextServerId++;
        int port = BASE_PORT + serverId - 1;
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
                if (Files.exists(serverDir)) deleteDirectory(serverDir);

                instance.phase = "template";
                instance.phaseStartedAt = System.currentTimeMillis();
                log.info("Copying template to {}...", dirName);
                robocopy(templatePath, serverDir);

                if (mapWorldPath != null && !mapWorldPath.isEmpty()) {
                    Path mapWorldDir = Paths.get(mapWorldPath);
                    if (Files.isDirectory(mapWorldDir)) {
                        instance.phase = "map";
                        instance.phaseStartedAt = System.currentTimeMillis();
                        log.info("Copying map world '{}'...", mapName);
                        Path matchWorldDir = serverDir.resolve(mapName);
                        Files.createDirectories(matchWorldDir);
                        robocopy(mapWorldDir, matchWorldDir);
                    }
                }

                instance.phase = "config";
                instance.phaseStartedAt = System.currentTimeMillis();
                Files.writeString(serverDir.resolve("server.properties"),
                        "server-port=" + port + "\nlevel-name=" + mapName +
                        "\nmax-players=" + maxPlayers + "\nonline-mode=false\n");

                // Unique voice chat port for each match server (avoid collision with main server)
                Path vcConfig = serverDir.resolve("config/voicechat/voicechat-server.properties");
                if (Files.exists(vcConfig)) {
                    String vcContent = Files.readString(vcConfig);
                    vcContent = vcContent.replaceAll("port=\\d+", "port=" + (24454 + serverId));
                    Files.writeString(vcConfig, vcContent);
                }

                instance.phase = "booting";
                instance.phaseStartedAt = System.currentTimeMillis();
                String title = "PWP Match " + serverId;
                String dir = serverDir.toAbsolutePath().toString();
                ProcessBuilder pb = new ProcessBuilder(
                        "cmd.exe", "/c",
                        "start", title, "/D", dir, "/WAIT",
                        "cmd", "/c", "run.bat", "nogui");
                pb.directory(serverDir.toFile());
                pb.environment().put("JAVA_HOME", System.getProperty("java.home"));

                instance.process = pb.start();
                log.info("Match server {} started on port {} (map: {})", serverId, port, mapName);
            } catch (Exception e) {
                log.error("Failed to start match server {}: {}", serverId, e.getMessage());
                instance.failed = true;
                instance.error = e.getMessage();
                instance.phase = null;
                try { if (Files.exists(serverDir)) deleteDirectory(serverDir); } catch (IOException ignored) {}
            }
        });

        return result;
    }

    /** Called from main tick — updates booted status, cleans dead servers, broadcasts progress */
    public static void tick() {
        long now = System.currentTimeMillis();
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
                log.warn("Server {} process died prematurely", si.serverId);
                si.failed = true;
                si.error = "process died";
            }
        }

        // Detect boot via logs/latest.log "Done", fallback to port-based timeout
        for (ServerInstance si : servers.values()) {
            if (si.phase == "booting" && !si.booted && !si.failed) {
                Path logFile = si.directory.resolve("logs/latest.log");
                // Method 1: read log file line-by-line looking for "Done"
                if (si.logDoneAt == 0 && Files.exists(logFile)) {
                    try (BufferedReader br = new BufferedReader(new FileReader(logFile.toFile()))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (line.contains("Done") && line.contains("For help")) {
                                si.logDoneAt = System.currentTimeMillis();
                                log.info("Server {} detected 'Done' in log", si.serverId);
                                break;
                            }
                        }
                    } catch (IOException ignored) {}
                }
                // Method 2: fallback — port open + 55s (covers ~44s from port to "Done")
                if (si.logDoneAt == 0 && si.portOpenAt == 0) {
                    try (Socket s = new Socket()) {
                        s.connect(new InetSocketAddress("127.0.0.1", si.port), 200);
                        si.portOpenAt = System.currentTimeMillis();
                        log.info("Server {} port open, fallback boot in 55s", si.serverId);
                    } catch (IOException ignored) {}
                }
                boolean logReady  = si.logDoneAt  != 0 && now - si.logDoneAt  >= 2000;
                boolean portReady = si.portOpenAt != 0 && now - si.portOpenAt >= 55000;
                if (logReady || portReady) {
                    si.booted = true;
                    log.info("Server {} fully booted (log={})", si.serverId, si.logDoneAt != 0);
                }
            }
        }

        // Broadcast progress to lobby players (throttled to every 2s per server)
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerInstance si : servers.values()) {
            if (si.phase != null && !si.failed && now - si.lastBroadcastMs > 2000) {
                si.lastBroadcastMs = now;
                long elapsed = (now - si.phaseStartedAt) / 1000;
                String msg = switch (si.phase) {
                    case "template" -> "§7[PWP] Copying server template... §e" + elapsed + "s";
                    case "map" -> "§7[PWP] Copying map world... §e" + elapsed + "s";
                    case "config" -> "§7[PWP] Preparing config... §e" + elapsed + "s";
                    case "booting" -> "§7[PWP] Starting server... §e" + elapsed + "s";
                    default -> null;
                };
                if (msg != null) {
                    String finalMsg = msg;
                    server.getPlayerList().getPlayers().forEach(p ->
                            p.sendSystemMessage(Component.literal(finalMsg), false));
                }
            }
            if (si.booted && si.phase != null) {
                si.phase = null;
                server.getPlayerList().getPlayers().forEach(p ->
                        p.sendSystemMessage(Component.literal("§a[PWP] Match server ready! §7(port " + si.port + ")"), false));
            }
        }
    }

    public static void stopServer(int serverId) {
        ServerInstance si = servers.remove(serverId);
        if (si == null) return;
        if (si.process.isAlive()) {
            si.process.destroy();
            try { si.process.waitFor(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            if (si.process.isAlive()) si.process.destroyForcibly();
        }
        try { deleteDirectory(si.directory); log.info("Cleaned up {}", si.directory.getFileName()); } catch (IOException ignored) {}
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

    public static int getActiveCount() {
        return (int) servers.values().stream().filter(si -> !si.failed).count();
    }

    // --- private helpers ---

    private static void robocopy(Path source, Path target) throws IOException {
        Files.createDirectories(target);
        String src = source.toAbsolutePath() + "\\";
        String dst = target.toAbsolutePath() + "\\";
        ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "robocopy", src, dst,
                "/E", "/MT:8", "/NFL", "/NDL", "/NJH", "/NJS", "/R:0", "/W:0");
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while (br.readLine() != null) {}
        }
        try {
            if (!p.waitFor(5, TimeUnit.MINUTES)) { p.destroyForcibly(); throw new IOException("robocopy timed out"); }
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
