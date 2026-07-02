package com.pwp.lobby;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class ServerManager {

    private static final String TEMPLATE_PATH = "../server-template";
    private static final String SERVERS_PATH = "../match_";
    private static final int BASE_PORT = 25566;
    private static final int MAX_SERVERS = 10;

    private static final Map<Integer, Process> runningServers = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> serverPorts = new ConcurrentHashMap<>();
    private static int nextServerId = 1;

    public static int startMatchServer(String mapName, int maxPlayers) throws Exception {
        if (runningServers.size() >= MAX_SERVERS) {
            throw new Exception("Maximum server limit reached");
        }

        int serverId = nextServerId++;
        int port = BASE_PORT + serverId - 1;
        String serverDir = SERVERS_PATH + String.format("%02d", serverId);

        copyDirectory(Paths.get(TEMPLATE_PATH), Paths.get(serverDir));

        Path propertiesPath = Paths.get(serverDir, "server.properties");
        String properties = Files.readString(propertiesPath);
        properties = properties.replace("${PORT}", String.valueOf(port));
        properties = properties.replace("${LEVEL}", mapName);
        properties = properties.replace("${MAX_PLAYERS}", String.valueOf(maxPlayers));
        Files.writeString(propertiesPath, properties);

        ProcessBuilder pb = new ProcessBuilder(
                "java", "-Xmx2G", "-jar", "forge.jar", "nogui"
        );
        pb.directory(new File(serverDir));
        pb.inheritIO();
        Process process = pb.start();

        runningServers.put(serverId, process);
        serverPorts.put(serverId, port);

        return port;
    }

    public static void stopServer(int serverId) {
        Process process = runningServers.get(serverId);
        if (process != null && process.isAlive()) {
            process.destroy();
        }
        runningServers.remove(serverId);
        serverPorts.remove(serverId);
    }

    public static int getServerPort(int serverId) {
        return serverPorts.getOrDefault(serverId, -1);
    }

    public static int getAvailablePort() {
        return serverPorts.values().stream()
                .mapToInt(Integer::intValue)
                .min()
                .orElse(BASE_PORT);
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path dest = target.resolve(source.relativize(src));
                if (src.toFile().isDirectory()) {
                    Files.createDirectories(dest);
                } else {
                    Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }
}
