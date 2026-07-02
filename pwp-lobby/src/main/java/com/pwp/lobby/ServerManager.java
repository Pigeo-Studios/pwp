package com.pwp.lobby;

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

    private static final String TEMPLATE_PATH = "../PWP-Server";
    private static final int BASE_PORT = 25566;
    private static final int MAX_SERVERS = 10;
    private static final long START_TIMEOUT_MS = 120_000;

    private static final Map<Integer, Process> runningServers = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> serverPorts = new ConcurrentHashMap<>();
    private static final Map<Integer, String> serverDirs = new ConcurrentHashMap<>();
    private static int nextServerId = 1;

    public static class StartResult {
        public int serverId;
        public int port;
        public boolean ready;
        public String error;
    }

    public static StartResult startMatchServer(String mapName, int maxPlayers) {
        StartResult result = new StartResult();

        // Check if template exists
        Path templatePath = Paths.get(TEMPLATE_PATH);
        if (!Files.exists(templatePath) || !Files.isDirectory(templatePath)) {
            result.error = "Template directory not found: " + templatePath.toAbsolutePath();
            log.error(result.error);
            return result;
        }

        // Find forge launcher in template
        Path runBat = templatePath.resolve("run.bat");
        if (!Files.exists(runBat)) {
            result.error = "run.bat not found in template";
            return result;
        }

        int serverId = nextServerId++;
        int port = BASE_PORT + serverId - 1;
        String serverDirName = "match_" + String.format("%02d", serverId);
        Path serverDir = Paths.get(serverDirName);

        try {
            // Clean up old match dir if exists
            if (Files.exists(serverDir)) {
                deleteDirectory(serverDir);
            }

            // Copy template
            copyDirectory(templatePath, serverDir);

            // Update server.properties
            Path propertiesPath = serverDir.resolve("server.properties");
            if (Files.exists(propertiesPath)) {
                String props = Files.readString(propertiesPath);
                props = props.replace("${PORT}", String.valueOf(port));
                props = props.replace("${LEVEL}", mapName);
                props = props.replace("${MAX_PLAYERS}", String.valueOf(maxPlayers));
                Files.writeString(propertiesPath, props);
            } else {
                // Create default
                Files.writeString(propertiesPath,
                        "server-port=" + port + "\nlevel-name=" + mapName +
                        "\nmax-players=" + maxPlayers + "\nonline-mode=true\n");
            }

            // Read JVM args from template
            Path jvmArgsPath = serverDir.resolve("user_jvm_args.txt");
            String jvmArgs = "-Xmx4G -Xms2G";
            if (Files.exists(jvmArgsPath)) {
                jvmArgs = Files.readString(jvmArgsPath).trim();
            }

            // Start the server process
            ProcessBuilder pb = new ProcessBuilder(
                    "cmd.exe", "/c", "run.bat"
            );
            pb.directory(serverDir.toFile());
            pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
            pb.redirectErrorStream(true);

            log.info("Starting match server {} on port {} (map: {})", serverId, port, mapName);
            Process process = pb.start();

            // Read initial output in a separate thread
            startOutputReader(serverId, process);

            runningServers.put(serverId, process);
            serverPorts.put(serverId, port);
            serverDirs.put(serverId, serverDirName);

            result.serverId = serverId;
            result.port = port;

            // Wait for server to be ready
            log.info("Waiting for server {} to be ready...", serverId);
            result.ready = waitForServerReady("127.0.0.1", port, START_TIMEOUT_MS);

            if (result.ready) {
                log.info("Match server {} ready on port {}", serverId, port);
            } else {
                log.warn("Match server {} not ready after {}ms", serverId, START_TIMEOUT_MS);
            }

        } catch (Exception e) {
            log.error("Failed to start match server: {}", e.getMessage());
            result.error = e.getMessage();
        }

        return result;
    }

    private static void startOutputReader(int serverId, Process process) {
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.contains("Done") && line.contains("For help")) {
                        log.info("Server {} ready message detected", serverId);
                    }
                }
            } catch (IOException e) {
                // Process ended
            }
        }, "server-" + serverId + "-output");
        reader.setDaemon(true);
        reader.start();
    }

    public static boolean waitForServerReady(String host, int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        int attempt = 0;

        while (System.currentTimeMillis() < deadline) {
            attempt++;
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 500);
                log.info("Server ready after {} attempts ({}ms)", attempt,
                        System.currentTimeMillis() - (deadline - timeoutMs));
                return true;
            } catch (IOException e) {
                if (attempt % 10 == 0) {
                    log.debug("Waiting for server... ({})", attempt);
                }
                try { Thread.sleep(1000); } catch (InterruptedException ie) { break; }
            }
        }
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

        String dir = serverDirs.remove(serverId);
        if (dir != null) {
            try {
                deleteDirectory(Paths.get(dir));
                log.info("Cleaned up {}", dir);
            } catch (IOException e) {
                log.warn("Failed to clean up {}: {}", dir, e.getMessage());
            }
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
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
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
