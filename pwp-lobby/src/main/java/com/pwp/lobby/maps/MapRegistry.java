package com.pwp.lobby.maps;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MapRegistry {

    private static final Logger log = LoggerFactory.getLogger(MapRegistry.class);
    private static final Gson GSON = new Gson();

    private static final Map<String, MapConfig> maps = new LinkedHashMap<>();
    private static String mapsDirectory = "../maps";

    public static void configure(String dir) {
        mapsDirectory = dir;
    }

    public static void loadAll() {
        maps.clear();
        File dir = new File(mapsDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Maps directory not found: {} (resolved: {})", mapsDirectory, dir.getAbsolutePath());
            return;
        }

        File[] subdirs = dir.listFiles(File::isDirectory);
        if (subdirs == null || subdirs.length == 0) {
            log.warn("No map subdirectories found in {}", mapsDirectory);
            return;
        }

        for (File subdir : subdirs) {
            File configFile = new File(subdir, "map_config.json");
            if (!configFile.exists()) {
                log.debug("Skipping {}: no map_config.json", subdir.getName());
                continue;
            }
            try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
                MapConfig cfg = GSON.fromJson(reader, MapConfig.class);
                cfg.worldPath = subdir.getAbsolutePath();
                if (cfg.name != null && !cfg.name.isEmpty()) {
                    maps.put(cfg.name, cfg);
                    log.info("Loaded map: {} ({}) from {}", cfg.displayName, cfg.name, subdir.getAbsolutePath());
                }
            } catch (Exception e) {
                log.warn("Failed to load map config from {}: {}", configFile.getPath(), e.getMessage());
            }
        }

        if (maps.isEmpty()) {
            log.warn("No valid maps found in {}", mapsDirectory);
        }
    }

    public static MapConfig get(String name) {
        return maps.get(name);
    }

    public static List<MapConfig> getAll() {
        return List.copyOf(maps.values());
    }

    public static List<MapConfig> getVotable() {
        return maps.values().stream()
                .filter(m -> m.maxPlayers > 0 && m.minPlayers > 0)
                .collect(Collectors.toList());
    }

    public static MapConfig getBestFit(int playerCount) {
        return maps.values().stream()
                .filter(m -> playerCount >= m.minPlayers)
                .min(Comparator.comparingInt(m -> Math.abs(m.maxPlayers - playerCount)))
                .orElse(maps.values().iterator().next());
    }
}
