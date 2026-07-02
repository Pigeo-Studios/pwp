package com.pwp.lobby.maps;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class MapRegistry {

    private static final Logger log = LoggerFactory.getLogger(MapRegistry.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, MapConfig> maps = new LinkedHashMap<>();
    private static String mapsDirectory = "../server-template/maps";

    public static void configure(String dir) {
        mapsDirectory = dir;
    }

    public static void loadAll() {
        maps.clear();
        File dir = new File(mapsDirectory);
        if (!dir.exists() || !dir.isDirectory()) {
            log.warn("Maps directory not found: {}", mapsDirectory);
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files == null) return;

        for (File f : files) {
            try (FileReader reader = new FileReader(f)) {
                MapConfig cfg = GSON.fromJson(reader, MapConfig.class);
                if (cfg.name != null && !cfg.name.isEmpty()) {
                    maps.put(cfg.name, cfg);
                    log.info("Loaded map: {} ({})", cfg.displayName, cfg.name);
                }
            } catch (Exception e) {
                log.warn("Failed to load map config: {} - {}", f.getName(), e.getMessage());
            }
        }

        if (maps.isEmpty()) {
            log.warn("No map configs found, generating defaults");
            generateDefaults();
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

    private static void generateDefaults() {
        add("fools_road", "Fool's Road", 100, 10, "map1",
                "BLUE", "usa", 800, "RED", "russia", 800,
                List.of(cp("Village", 50, 50, 30)));

        add("chora_valley", "Chora Valley", 80, 10, "map2",
                "BLUE", "nato", 600, "RED", "insurgency", 600,
                List.of(cp("Town", -30, 20, 25)));

        add("tallil_outskirts", "Tallil Outskirts", 100, 10, "map3",
                "BLUE", "ukraine", 800, "RED", "russia", 800,
                List.of(cp("Airbase", 100, -50, 40)));

        log.info("Generated {} default map configs", maps.size());
    }

    private static void add(String name, String display, int max, int min, String img,
                             String bTeam, String bFac, int bTickets,
                             String rTeam, String rFac, int rTickets,
                             List<MapConfig.CapturePointConfig> points) {
        MapConfig cfg = new MapConfig();
        cfg.name = name;
        cfg.displayName = display;
        cfg.maxPlayers = max;
        cfg.minPlayers = min;
        cfg.image = img;
        cfg.BLUE.faction = bFac;
        cfg.BLUE.tickets = bTickets;
        cfg.RED.faction = rFac;
        cfg.RED.tickets = rTickets;
        cfg.capturePoints = points;
        maps.put(name, cfg);
    }

    private static MapConfig.CapturePointConfig cp(String name, int x, int z, int r) {
        MapConfig.CapturePointConfig p = new MapConfig.CapturePointConfig();
        p.name = name; p.x = x; p.z = z; p.radius = r;
        return p;
    }
}
