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
    private static final long START_TIMEOUT_MS = 240_000;

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

    public static StartResult startMatchServer(String mapName, int maxPlayers, String mapWorldPath) {
        StartResult result = new StartResult();

        Path templatePath = Paths.get(TEMPLATE_PATH).toAbsolutePath().normalize();
        if (!Files.exists(templatePath) || !Files.isDirectory(templatePath)) {
            result.error = "Template not found: " + templatePath.toAbsolutePath();
            return result;
        }

        Path runBat = templatePath.resolve("run.bat");
        if (!Files.exists(runBat)) {
            result.error = "run.bat not found in template";
            return result;
        }

        int serverId = nextServerId++;
        int port = BASE_PORT + serverId - 1;
        String serverDirName = "match_" + String.format("%02d", serverId);
        Path serverDir = Paths.get(serverDirName).toAbsolutePath();
        String worldFolderName = mapName;

        // Return immediately with pending status, work done on background thread
        result.serverId = serverId;
        result.port = port;
        result.ready = false;
        matchExecutor.submit(() -> {
            try {
                if (Files.exists(serverDir)) deleteDirectory(serverDir);

                log.info("Copying template to {}...", serverDirName);
                copyTemplateFast(templatePath, serverDir);
                log.info("Template copied to {}", serverDirName);

                // Copy map world
                if (mapWorldPath != null && !mapWorldPath.isEmpty()) {
                    Path mapWorldDir = Paths.get(mapWorldPath);
                    if (Files.exists(mapWorldDir) && Files.isDirectory(mapWorldDir)) {
                        log.info("Copying map world '{}'...", mapName);
                        Path matchWorldDir = serverDir.resolve(worldFolderName);
                        Files.createDirectories(matchWorldDir);
                        Files.walk(mapWorldDir).forEach(src -> {
                            try {
                                Path rel = mapWorldDir.relativize(src);
                                if (rel.toString().equals("map_config.json")) return;
                                Path dest = matchWorldDir.resolve(rel);
                                if (Files.isDirectory(src)) Files.createDirectories(dest);
                                else Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        });
                        log.info("Map world '{}' copied", mapName);
                    }
                }

                // Update server.properties
                Path propertiesPath = serverDir.resolve("server.properties");
                if (Files.exists(propertiesPath)) {
                    String props = Files.readString(propertiesPath);
                    props = props.replace("${PORT}", String.valueOf(port));
                    props = props.replace("${LEVEL}", worldFolderName);
                    props = props.replace("${MAX_PLAYERS}", String.valueOf(maxPlayers));
                    Files.writeString(propertiesPath, props);
                } else {
                    Files.writeString(propertiesPath,
                            "server-port=" + port + "\nlevel-name=" + worldFolderName +
                            "\nmax-players=" + maxPlayers + "\nonline-mode=true\n");
                }

                // Generate win_args.txt and fix run.bat for the match server
                generateWinArgsAndFixRunBat(serverDir);

                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "run.bat", "nogui");
                pb.directory(serverDir.toFile());
                pb.environment().put("JAVA_HOME", System.getProperty("java.home"));
                pb.redirectErrorStream(true);

                log.info("Starting match server {} on port {} (map: {})", serverId, port, mapName);
                Process process = pb.start();
                startOutputReader(serverId, process);

                boolean ready = waitForServerReady("127.0.0.1", port, START_TIMEOUT_MS);

                runningServers.put(serverId, process);
                serverPorts.put(serverId, port);
                serverDirs.put(serverId, serverDirName);

                if (ready) {
                    log.info("Match server {} ready on port {}", serverId, port);
                } else {
                    log.warn("Match server {} not ready after {}ms", serverId, START_TIMEOUT_MS);
                }
            } catch (Exception e) {
                log.error("Failed to start match server {}: {}", serverId, e.getMessage());
            }
        });

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
        if (timeoutMs <= 0) {
            // Single non-blocking check
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(host, port), 500);
                return true;
            } catch (IOException e) {
                return false;
            }
        }
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

    private static final Set<String> SKIP_DIRS = Set.of("world", "logs", "crash-reports",
            "tacz", "maps", "cache", "match_01", "match_02", "match_03", "match_04", "match_05",
            "usercache.json", "banned-ips.json", "banned-players.json", "ops.json", "whitelist.json");
    private static final ExecutorService matchExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "match-server-starter");
        t.setDaemon(true);
        return t;
    });

    /** Directories to junction instead of copy — instant and saves disk space */
    private static final Set<String> JUNCTION_DIRS = Set.of("libraries");

    /** Fast template copy using robocopy (multi-threaded on Windows) + junctions for large dirs */
    private static void copyTemplateFast(Path source, Path target) throws IOException {
        // Create target directory
        Files.createDirectories(target);

        // Create junctions for large static directories (instant, no data copied)
        for (String dirName : JUNCTION_DIRS) {
            Path srcDir = source.resolve(dirName).normalize();
            if (Files.exists(srcDir) && Files.isDirectory(srcDir)) {
                Path junctionPath = target.resolve(dirName).normalize();
                if (!Files.exists(junctionPath)) {
                    String srcAbs = srcDir.toAbsolutePath().toString();
                    String dstAbs = junctionPath.toAbsolutePath().toString();
                    ProcessBuilder pb = new ProcessBuilder(
                            "cmd.exe", "/c", "mklink", "/J", dstAbs, srcAbs
                    );
                    pb.redirectErrorStream(true);
                    try {
                        Process p = pb.start();
                        p.waitFor(10, TimeUnit.SECONDS);
                        if (p.exitValue() != 0) {
                            log.warn("mklink returned non-zero for {}", dirName);
                        } else {
                            log.info("Created junction for {} -> {}", dirName, srcAbs);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to create junction for {}, falling back to copy: {}", dirName, e.getMessage());
                        copyDirectory(srcDir, junctionPath);
                    }
                }
            }
        }

        // Use robocopy for the rest (multi-threaded, much faster than Files.walk)
        String src = source.toAbsolutePath().toString() + "\\";
        String dst = target.toAbsolutePath().toString() + "\\";

        // Build robocopy args: robocopy src dst /E /MT:8 /XD dir1 dir2 ... /XF file1 ...
        List<String> cmd = new ArrayList<>();
        cmd.add("cmd.exe");
        cmd.add("/c");
        cmd.add("robocopy");
        cmd.add(src);
        cmd.add(dst);
        cmd.add("/E");
        cmd.add("/MT:8");
        cmd.add("/NFL");
        cmd.add("/NDL");
        cmd.add("/NJH");
        cmd.add("/NJS");
        cmd.add("/R:0");
        cmd.add("/W:0");
        cmd.add("/XD");
        cmd.addAll(SKIP_DIRS);
        cmd.addAll(JUNCTION_DIRS);

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();

        // Read output silently
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            while (br.readLine() != null) {}
        }

        try {
            boolean finished = p.waitFor(5, TimeUnit.MINUTES);
            if (!finished) {
                p.destroyForcibly();
                throw new IOException("robocopy timed out after 5 minutes");
            }
            int exitCode = p.exitValue();
            // robocopy exit codes 0-7 are success
            if (exitCode > 7) {
                throw new IOException("robocopy failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            p.destroyForcibly();
            throw new IOException("robocopy interrupted", e);
        }
    }

    /** Fallback Java copy for small dirs (map worlds) */
    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walk(source).forEach(src -> {
            try {
                Path rel = source.relativize(src);
                Path dest = target.resolve(rel);
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

    /** Generate win_args.txt and fix run.bat for the match server */
    private static void generateWinArgsAndFixRunBat(Path serverDir) throws IOException {
        Path librariesDir = serverDir.resolve("libraries");
        if (!Files.exists(librariesDir)) {
            log.warn("libraries dir not found, skipping win_args generation");
            return;
        }

        // Build classpath from all JARs in libraries/
        StringBuilder classpath = new StringBuilder();
        List<Path> jars = new ArrayList<>();
        Files.walk(librariesDir).forEach(p -> {
            if (p.toString().endsWith(".jar")) jars.add(p);
        });

        for (int i = 0; i < jars.size(); i++) {
            if (i > 0) classpath.append(";");
            classpath.append(jars.get(i).toAbsolutePath().toString());
        }

        String winArgs = "-p " + classpath.toString() + "\n" +
                "--add-modules ALL-MODULE-PATH\n" +
                "--add-opens java.base/java.util.jar=cpw.mods.securejarhandler\n" +
                "--add-opens java.base/java.lang.invoke=cpw.mods.securejarhandler\n" +
                "--add-exports java.base/sun.security.util=cpw.mods.securejarhandler\n" +
                "--add-exports jdk.naming.dns/com.sun.jndi.dns=java.naming\n" +
                "-Djava.net.preferIPv6Addresses=system\n" +
                "-DignoreList=bootstraplauncher-1.1.2.jar,securejarhandler-2.1.10.jar," +
                "asm-commons-9.9.1.jar,asm-util-9.9.1.jar,asm-analysis-9.9.1.jar," +
                "asm-tree-9.9.1.jar,asm-9.9.1.jar,JarJarFileSystems-0.3.19.jar\n" +
                "-DlibraryDirectory=libraries\n" +
                "-DlegacyClassPath=" + classpath.toString() + "\n" +
                "cpw.mods.bootstraplauncher.BootstrapLauncher\n" +
                "--launchTarget forgeserver\n" +
                "--fml.forgeVersion 47.4.20\n" +
                "--fml.mcVersion 1.20.1\n" +
                "--fml.forgeGroup net.minecraftforge\n" +
                "--fml.mcpVersion 20230612.114412\n" +
                "nogui\n";

        String[] dirs = {"libraries", "net", "minecraftforge", "forge", "1.20.1-47.4.20"};
        Path winArgsDir = serverDir;
        for (String d : dirs) {
            winArgsDir = winArgsDir.resolve(d);
        }
        Files.createDirectories(winArgsDir);
        Path winArgsPath = winArgsDir.resolve("win_args.txt");
        Files.writeString(winArgsPath, winArgs);

        // Fix run.bat to use absolute path to win_args.txt
        Path runBatPath = serverDir.resolve("run.bat");
        if (Files.exists(runBatPath)) {
            String runBatContent = Files.readString(runBatPath);
            runBatContent = runBatContent.replace(
                "@libraries/net/minecraftforge/forge/1.20.1-47.4.20/win_args.txt",
                "@" + winArgsPath.toAbsolutePath().toString().replace("\\", "/")
            );
            Files.writeString(runBatPath, runBatContent);
        }

        log.info("Generated win_args.txt for match server ({} jars in classpath)", jars.size());
    }
}
