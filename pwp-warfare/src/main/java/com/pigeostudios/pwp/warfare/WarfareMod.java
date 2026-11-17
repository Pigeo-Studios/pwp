package com.pigeostudios.pwp.warfare;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.client.ClientConfigRegistry;
import com.pigeostudios.pwp.warfare.command.ModCommands;
import com.pigeostudios.pwp.warfare.config.MapLayoutState;
import com.pigeostudios.pwp.warfare.config.MatchConfigLoader;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.ModEntities;
import com.pigeostudios.pwp.warfare.item.ModCreativeModeTabs;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.layout.Box;
import com.pigeostudios.pwp.warfare.layout.Issue;
import com.pigeostudios.pwp.warfare.layout.LayoutDerive;
import com.pigeostudios.pwp.warfare.layout.LayoutValidator;
import com.pigeostudios.pwp.warfare.layout.MapLayout;
import com.pigeostudios.pwp.warfare.layout.MapLayout.Border;
import com.pigeostudios.pwp.warfare.layout.MapLayoutCodec;
import com.pigeostudios.pwp.warfare.layout.Team;
import com.pigeostudios.pwp.warfare.menu.ModMenuTypes;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.server.FactionDataLoader;
import com.pigeostudios.pwp.warfare.server.MatchHeartbeatSender;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.config.MapConfigLegacy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod("pwpwarfare")
public class WarfareMod {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final java.util.concurrent.atomic.AtomicBoolean LEGACY_WARNED =
            new java.util.concurrent.atomic.AtomicBoolean();

    public WarfareMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        ModLoadingContext.get().registerConfig(Type.COMMON, WarfareConfig.SPEC);
        ModLoadingContext.get().registerConfig(Type.CLIENT, WarfareConfig.CLIENT_SPEC);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientConfigRegistry::registerConfigScreen);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModSounds.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        MatchHeartbeatSender.register();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            PacketHandler.register();
            // Типы запросов данных warfare — в единой системе core-client
            com.pigeostudios.pwp.warfare.network.WarfareDataRequests.register();
            // тайлы динамической карты (mapTile): отвечает только на запросы клиентов с включённой картой
            com.pigeostudios.pwp.warfare.server.map.MapTileService.register();
        });
    }

    private void clientSetup(FMLClientSetupEvent event) {}

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("PWP Warfare Server starting...");
        applyMapConfig(event.getServer().overworld());
        MatchConfigLoader.load(event.getServer().overworld());
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
    }

    public static void applyMapConfig(ServerLevel level) {
        WarfareWorldData data = WarfareWorldData.get(level);
        if (data.match.configApplied) return;

        String levelName = "world";
        try {
            Path propsPath = Path.of("server.properties");
            if (Files.exists(propsPath)) {
                for (String line : Files.readAllLines(propsPath)) {
                    if (line.startsWith("level-name=")) {
                        levelName = line.substring("level-name=".length()).trim();
                        break;
                    }
                }
            }
        } catch (IOException ignored) {
            LOGGER.warn("WarfareMod.applyMapConfig: исключение", ignored);
        }

        Path configPath = Path.of(levelName, "map_config.json");
        if (!Files.exists(configPath)) {
            configPath = Path.of("world", "map_config.json");
        }
        if (!Files.exists(configPath)) {
            configPath = Path.of("map_config.json");
        }
        if (!Files.exists(configPath)) {
            LOGGER.warn("map_config.json not found, skipping config apply");
            return;
        }

        try {
            String content = Files.readString(configPath);
            Gson gson = new Gson();
            JsonObject root = gson.fromJson(content, JsonObject.class);

            if (root.has("teams")) {
                JsonObject teams = root.getAsJsonObject("teams");
                if (teams.has("BLUE")) {
                    JsonObject blue = teams.getAsJsonObject("BLUE");
                    data.match.blueFaction = blue.get("faction").getAsString();
                    data.match.blueTickets = blue.get("tickets").getAsInt();
                }
                if (teams.has("RED")) {
                    JsonObject red = teams.getAsJsonObject("RED");
                    data.match.redFaction = red.get("faction").getAsString();
                    data.match.redTickets = red.get("tickets").getAsInt();
                }
            }

            if (root.has("settings")) {
                JsonObject settings = root.getAsJsonObject("settings");
                if (settings.has("respawnTimer"))
                    data.match.respawnTimer = settings.get("respawnTimer").getAsInt();
                if (settings.has("deathTicketCost"))
                    data.match.deathTicketCost = settings.get("deathTicketCost").getAsInt();
            }

            // Раскладка (точки, мейны, спавны, техника, граница, режим) — через чистую модель: кодек + авто-вывод
            // незаданного (явное в конфиге приоритетнее) + валидатор (пока только лог, старт не блокируется)
            MapLayoutCodec.Parsed parsed = MapLayoutCodec.read(root);
            LayoutDerive.Resolved resolved = LayoutDerive.resolve(parsed.layout());
            MapLayout layout = resolved.layout();
            List<Issue> issues = new ArrayList<>(parsed.issues());
            issues.addAll(resolved.notes());
            issues.addAll(LayoutValidator.validate(layout));
            MapLayoutState.set(layout, issues, configPath.toString());
            MapLayoutState.log("map_config load");

            Border border = layout.border();
            if (border != null) {
                if (border.centerX() != null) data.match.mapCenterX = border.centerX();
                if (border.centerZ() != null) data.match.mapCenterZ = border.centerZ();
                if (border.sizeBlocks() != null) data.match.mapSizeBlocks = border.sizeBlocks();
                level.getWorldBorder().setCenter(data.match.mapCenterX, data.match.mapCenterZ);
                level.getWorldBorder().setSize(data.match.mapSizeBlocks);
            }

            if (root.has("image")) {
                data.match.currentMapImage = root.get("image").getAsString();
            }

            if (root.has("mainZones")) {
                data.structures.mainZones.clear();
                for (MapLayout.MainZoneDef zone : layout.mainZones()) {
                    data.structures.mainZones.add(
                            new WarfareWorldData.MainProtectionZone(zone.team(), zone.effShape(), aabb(zone.box())));
                }
            }

            warnLegacyKeys(root, configPath.toString());

            // Точки захвата: единый источник — map_config (паттерн или capturePoints). SavedData — лишь копия на
            // время матча; applyMapConfig идёт на каждом старте сервера (configApplied в NBT не пишется).
            boolean pointsFromConfig = false;
            if (layout.patternIndex() >= 0) {
                int patternIdx = layout.patternIndex();
                JsonObject chosen = root.getAsJsonArray("capturePointPatterns")
                        .get(patternIdx)
                        .getAsJsonObject();
                String patternName = chosen.has("name") ? chosen.get("name").getAsString() : "Pattern " + patternIdx;
                data.match.capturePoints.clear();
                for (MapLayout.CapturePointDef p : layout.points()) {
                    data.match.capturePoints.add(toCapturePoint(p));
                }
                pointsFromConfig = true;
                LOGGER.info(
                        "Loaded capture point pattern: {} ({} points)", patternName, data.match.capturePoints.size());
            } else if (root.has("capturePointPatterns") && root.has("capturePointPatternIndex")) {
                LOGGER.warn(
                        "Invalid capture point pattern index {}, falling back to default",
                        root.get("capturePointPatternIndex"));
            }

            if (!pointsFromConfig && !layout.points().isEmpty()) {
                int fromWorld = data.match.capturePoints.size();
                data.match.capturePoints.clear();
                for (MapLayout.CapturePointDef p : layout.points()) {
                    data.match.capturePoints.add(toCapturePoint(p));
                }
                pointsFromConfig = true;
                LOGGER.info(
                        "Capture points from map_config: {} (replaced {} from world SavedData)",
                        data.match.capturePoints.size(),
                        fromWorld);
            }

            if (!pointsFromConfig && !data.match.capturePoints.isEmpty()) {
                // В конфиге точек нет, в мире есть — не теряем: оставляем мировые и просим перенести в map_config
                LOGGER.warn(
                        "map_config {} has no capturePoints; keeping {} capture points from world SavedData —"
                                + " move them into map_config (single source of truth)",
                        configPath,
                        data.match.capturePoints.size());
            }

            // Спавн мейна: teams.*.spawn, иначе выведен из центра мейн-зоны (Y — дефолт 64, см. SPAWN_DERIVED)
            for (Team team : Team.values()) {
                MapLayout.TeamSpawn spawn = layout.spawn(team);
                if (spawn == null) continue;
                BlockPos pos = new BlockPos(spawn.x(), spawn.effY(), spawn.z());
                if (team == Team.BLUE) {
                    data.match.blueSpawns.put(spawn.effDimension(), pos);
                } else {
                    data.match.redSpawns.put(spawn.effDimension(), pos);
                }
            }

            if (ModList.get().isLoaded("pwpcore")) {
                // Киты и пулы техники — из API асинхронно; применяются на главном потоке,
                // задолго до конца фазы ожидания (300 с)
                FactionDataLoader.start(level, data);
            } else {
                LOGGER.info("Core API not available, using NBT-based kits/vehicles");
            }

            for (MapLayout.VehicleSlot slot : layout.vehicleSlots()) {
                data.addConfigVehicleSpawner(
                        slot.team().name(),
                        slot.role(),
                        slot.vehicleName(),
                        slot.pos().x(),
                        slot.pos().y(),
                        slot.pos().z(),
                        slot.effYaw(),
                        slot.effFixed());
            }

            if (data.match.hideDeathMessages) {
                level.getGameRules()
                        .getRule(net.minecraft.world.level.GameRules.RULE_SHOWDEATHMESSAGES)
                        .set(false, level.getServer());
            }
            if (data.match.disableNaturalRegen) {
                level.getGameRules()
                        .getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION)
                        .set(false, level.getServer());
            }
            if (data.match.disableBlockDrops) {
                level.getGameRules()
                        .getRule(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)
                        .set(false, level.getServer());
            }
            if (data.match.disableEntityDrops) {
                level.getGameRules()
                        .getRule(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS)
                        .set(false, level.getServer());
                level.getGameRules()
                        .getRule(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)
                        .set(false, level.getServer());
            }
            if (data.match.disableFireSpread) {
                level.getGameRules()
                        .getRule(net.minecraft.world.level.GameRules.RULE_DOFIRETICK)
                        .set(false, level.getServer());
            }
            if (data.match.disableWeatherCycle) {
                level.getGameRules()
                        .getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT)
                        .set(false, level.getServer());
                level.getGameRules()
                        .getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE)
                        .set(false, level.getServer());
            }

            data.match.configApplied = true;
            data.setDirty();
            LOGGER.info(
                    "Map config applied successfully for map: {}",
                    root.get("name").getAsString());

            // INVASION mode: set all points to defender, adjust tickets
            String mode = layout.effectiveMode();
            data.match.gameMode = mode;
            if (mode.equals("invasion") && !data.match.capturePoints.isEmpty()) {
                String defender = root.has("invasionDefender")
                        ? root.get("invasionDefender").getAsString()
                        : "RED";
                data.match.invasionDefender = defender;
                for (WarfareWorldData.CapturePoint cp : data.match.capturePoints) {
                    cp.owner = defender;
                    cp.progress = 1.0F;
                    cp.capturingTeam = "NONE";
                    cp.invLocked = false;
                }
                if (root.has("modes")) {
                    JsonObject modes = root.getAsJsonObject("modes");
                    if (modes.has("invasion")) {
                        JsonObject inv = modes.getAsJsonObject("invasion");
                        if (inv.has("captureBonus")) {
                            data.match.invasionCaptureBonus =
                                    inv.get("captureBonus").getAsInt();
                        }
                        if (inv.has("setupPhaseSeconds")) {
                            data.match.invasionSetupTimer =
                                    inv.get("setupPhaseSeconds").getAsInt();
                        }
                    }
                }
                LOGGER.info(
                        "INVASION mode: defender={}, captureBonus={}, setupTimer={}s",
                        defender,
                        data.match.invasionCaptureBonus,
                        data.match.invasionSetupTimer);
            }

            // Start waiting phase (5 min timer), then voting
            data.match.waitingActive = true;
            data.match.waitingTimer = 300;
            data.match.voteActive = false;
            data.match.votes.clear();
            PacketHandler.sendToAllClients(level, data);
        } catch (IOException e) {
            LOGGER.error("Failed to read map_config.json", e);
        }
    }

    /** Устаревшие ключи map_config ({@link MapConfigLegacy}) — WARN один раз за запуск сервера. */
    private static void warnLegacyKeys(JsonObject root, String source) {
        if (!LEGACY_WARNED.compareAndSet(false, true)) return;
        var settingsKeys = root.has("settings") && root.get("settings").isJsonObject()
                ? root.getAsJsonObject("settings").keySet()
                : java.util.Set.<String>of();
        var keys = MapConfigLegacy.ignoredKeys(root.keySet(), settingsKeys);
        if (!keys.isEmpty()) LOGGER.warn(MapConfigLegacy.warning(source, keys));
    }

    /** Точка из раскладки → состояние матча. Приоритет, который не удалось вывести, — прежний дефолт 10. */
    private static WarfareWorldData.CapturePoint toCapturePoint(MapLayout.CapturePointDef p) {
        int legacy = MapLayout.CapturePointDef.LEGACY_DEFAULT_PRIORITY;
        return new WarfareWorldData.CapturePoint(
                p.name(),
                aabb(p.box()),
                p.bluePriority() != null ? p.bluePriority() : legacy,
                p.redPriority() != null ? p.redPriority() : legacy,
                p.effCaptureTimeMinutes(),
                p.effTicketPenalty(),
                p.effCaptureDeduction(),
                p.effShape(),
                p.effLockDurationMinutes(),
                p.effGainNeutralizeTickets(),
                p.effGainCaptureTickets());
    }

    /** Бокс как его строил прежний парсер: углы pos1/pos2 как есть (AABB сам нормализует min/max). */
    private static AABB aabb(Box b) {
        return new AABB(
                b.pos1().x(),
                b.pos1().y(),
                b.pos1().z(),
                b.pos2().x(),
                b.pos2().y(),
                b.pos2().z());
    }
}
