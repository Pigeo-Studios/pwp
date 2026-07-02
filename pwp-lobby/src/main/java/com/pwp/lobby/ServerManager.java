package com.pwp.lobby;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.*;
import java.util.Map;
import java.util.concurrent.*;

public class ServerManager {

    private static final Logger log = LoggerFactory.getLogger(ServerManager.class);

    private static final String TEMPLATE_PATH = "../server-template";
    private static final String SERVERS_PATH = "../match_";
    private static final int BASE_PORT = 25566;
    private static final int MAX_SERVERS = 10;

    private static final Map<Integer, Process> runningServers = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> serverPorts = new ConcurrentHashMap<>();
    private static int nextServerId = 1;

    public static class StartResult {
        public int serverId;
        public int port;
        public boolean ready;
        public String error;
    }

    public static StartResult startMatchServer(String mapName, int maxPlayers) {
        StartResult result = new StartResult();
        try {
            if (runningServers.size() >= MAX_SERVERS) {
                result.error = "Maximum server limit reached";
                return result;
            }

            int serverId = nextServerId++;
            int port = BASE_PORT + serverId - 1;
            String serverDir = SERVERS_PATH + String.format("%02d", serverId);

            Files.createDirectories(Paths.get(serverDir));
            copyDirectory(Paths.get(TEMPLATE_PATH), Paths.get(serverDir));

            Path propertiesPath = Paths.get(serverDir, "server.properties");
            String properties = Files.readString(propertiesPath);
            properties = properties.replace("${PORT}", String.valueOf(port));
            properties = properties.replace("${LEVEL}", mapName);
            properties = properties.replace("${MAX_PLAYERS}", String.valueOf(maxPlayers));
            Files.writeString(propertiesPath, properties);

            ProcessBuilder pb = new ProcessBuilder(
                    "java", "-Xmx2G", "-Xms1G", "-jar", "forge.jar", "nogui"
            );
            pb.directory(new File(serverDir));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            runningServers.put(serverId, process);
            serverPorts.put(serverId, port);

            result.serverId = serverId;
            result.port = port;

            log.info("Match server {} starting on port {}...", serverId, port);

            boolean ready = waitForServerReady("127.0.0.1", port, 60_000);
            result.ready = ready;
            log.info("Match server {} ready: {}", serverId, ready);

        } catch (Exception e) {
            log.error("Failed to start match server: {}", e.getMessage());
            result.error = e.getMessage();
        }
        return result;
    }

    public static boolean waitForServerReady(String host, int port, int timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int attempt = 0;

        while (System.currentTimeMillis() < deadline) {
            attempt++;
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 1000);
                log.info("Server ready after {} attempts", attempt);
                return true;
            } catch (IOException e) {
                if (attempt % 10 == 0) {
                    log.debug("Waiting for server... (attempt {})", attempt);
                }
                try { Thread.sleep(1000); } catch (InterruptedException ie) { break; }
            }
        }
        log.warn("Server not ready after {}ms", timeoutMs);
        return false;
    }

    public static void stopServer(int serverId) {
        Process process = runningServers.get(serverId);
        if (process != null && process.isAlive()) {
            log.info("Stopping server {}", serverId);
            process.destroy();
            try {
                process.waitFor(10, TimeUnit.SECONDS);
                if (process.isAlive()) {
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
                process.destroyForcibly();
            }
        }
        runningServers.remove(serverId);
        serverPorts.remove(serverId);

        String serverDir = SERVERS_PATH + String.format("%02d", serverId);
        try {
            deleteDirectory(Paths.get(serverDir));
            log.info("Cleaned up server directory {}", serverDir);
        } catch (IOException e) {
            log.warn("Failed to clean up server directory: {}", e.getMessage());
        }
    }

    public static boolean isServerAlive(int serverId) {
        Process process = runningServers.get(serverId);
        return process != null && process.isAlive();
    }

    public static int getServerPort(int serverId) {
        return serverPorts.getOrDefault(serverId, -1);
    }

    public static int getActiveServerCount() {
        return runningServers.size();
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (Files.isDirectory(src)) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        }
    }
}
