package com.pwp.lobby;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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

                log.info("Copying template to {}...", dirName);
                robocopy(templatePath, serverDir);

                if (mapWorldPath != null && !mapWorldPath.isEmpty()) {
                    Path mapWorldDir = Paths.get(mapWorldPath);
                    if (Files.isDirectory(mapWorldDir)) {
                        log.info("Copying map world '{}'...", mapName);
                        Path matchWorldDir = serverDir.resolve(mapName);
                        Files.createDirectories(matchWorldDir);
                        robocopy(mapWorldDir, matchWorldDir);
                    }
                }

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

                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "run.bat", "nogui");
                pb.directory(serverDir.toFile());
                pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
                pb.redirectErrorStream(true);

                instance.process = pb.start();
                startOutputReader(serverId, instance.process);
                log.info("Match server {} started on port {} (map: {})", serverId, port, mapName);
            } catch (Exception e) {
                log.error("Failed to start match server {}: {}", serverId, e.getMessage());
                instance.failed = true;
                instance.error = e.getMessage();
                try { if (Files.exists(serverDir)) deleteDirectory(serverDir); } catch (IOException ignored) {}
            }
        });

        return result;
    }

    /** Called from main tick — updates booted status and cleans dead servers */
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
        return si != null && !si.failed;
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

    private static void startOutputReader(int serverId, Process process) {
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("Done") && line.contains("For help")) {
                        // Brief delay to let Forge finish internal init after "Done"
                        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                        ServerInstance si = servers.get(serverId);
                        if (si != null) si.booted = true;
                        log.info("Server {} is fully booted", serverId);
                    }
                }
            } catch (IOException ignored) {}
        }, "output-" + serverId);
        reader.setDaemon(true);
        reader.start();
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }
}
