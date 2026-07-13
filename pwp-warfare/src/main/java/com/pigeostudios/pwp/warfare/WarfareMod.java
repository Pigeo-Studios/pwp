package com.pigeostudios.pwp.warfare;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pwp.coreserver.CoreServerApi;
import net.minecraftforge.fml.ModList;
import com.pigeostudios.pwp.warfare.client.ClientConfigRegistry;
import com.pigeostudios.pwp.warfare.command.ModCommands;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.ModEntities;
import com.pigeostudios.pwp.warfare.item.ModCreativeModeTabs;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.menu.ModMenuTypes;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pigeostudios.pwp.warfare.config.MatchConfigLoader;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig.Type;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod("pwpwarfare")
public class WarfareMod {
   private static final Logger LOGGER = LogUtils.getLogger();

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
   }

   private void commonSetup(FMLCommonSetupEvent event) {
      event.enqueueWork(() -> PacketHandler.register());
   }

   private void clientSetup(FMLClientSetupEvent event) {
   }

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
      if (data.configApplied) return;

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
      } catch (IOException ignored) {}

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
               data.blueFaction = blue.get("faction").getAsString();
               data.blueTickets = blue.get("tickets").getAsInt();
            }
            if (teams.has("RED")) {
               JsonObject red = teams.getAsJsonObject("RED");
               data.redFaction = red.get("faction").getAsString();
               data.redTickets = red.get("tickets").getAsInt();
            }
         }

          if (root.has("settings")) {
             JsonObject settings = root.getAsJsonObject("settings");
              if (settings.has("respawnTimer")) data.respawnTimer = settings.get("respawnTimer").getAsInt();
              if (settings.has("deathTicketCost")) data.deathTicketCost = settings.get("deathTicketCost").getAsInt();
           }

          if (root.has("mapBounds")) {
             JsonObject bounds = root.getAsJsonObject("mapBounds");
             if (bounds.has("centerX")) data.mapCenterX = bounds.get("centerX").getAsInt();
             if (bounds.has("centerZ")) data.mapCenterZ = bounds.get("centerZ").getAsInt();
             if (bounds.has("sizeBlocks")) data.mapSizeBlocks = bounds.get("sizeBlocks").getAsInt();
             level.getWorldBorder().setCenter(data.mapCenterX, data.mapCenterZ);
             level.getWorldBorder().setSize(data.mapSizeBlocks);
          }

          if (root.has("image")) {
            data.currentMapImage = root.get("image").getAsString();
         }

         if (root.has("mainZones")) {
            data.mainZones.clear();
            var zones = root.getAsJsonArray("mainZones");
            for (var el : zones) {
               JsonObject zone = el.getAsJsonObject();
               String team = zone.get("team").getAsString();
               String shape = zone.has("shape") ? zone.get("shape").getAsString() : "cylinder";
               JsonObject p1 = zone.getAsJsonObject("pos1");
               JsonObject p2 = zone.getAsJsonObject("pos2");
               AABB aabb = new AABB(
                  p1.get("x").getAsInt(), p1.get("y").getAsInt(), p1.get("z").getAsInt(),
                  p2.get("x").getAsInt(), p2.get("y").getAsInt(), p2.get("z").getAsInt()
               );
               data.mainZones.add(new WarfareWorldData.MainProtectionZone(team, shape, aabb));
            }
         }

          if (root.has("capturePointPatterns") && root.has("capturePointPatternIndex")) {
             int patternIdx = root.get("capturePointPatternIndex").getAsInt();
             JsonArray patterns = root.getAsJsonArray("capturePointPatterns");
             if (patternIdx >= 0 && patternIdx < patterns.size()) {
                JsonObject chosen = patterns.get(patternIdx).getAsJsonObject();
                String patternName = chosen.has("name") ? chosen.get("name").getAsString() : "Pattern " + patternIdx;
                JsonArray points = chosen.getAsJsonArray("points");
                data.capturePoints.clear();
                for (var el : points) {
                   data.capturePoints.add(parseCapturePoint(el.getAsJsonObject()));
                }
                LOGGER.info("Loaded capture point pattern: {} ({} points)", patternName, data.capturePoints.size());
             } else {
                LOGGER.warn("Invalid capture point pattern index {}, falling back to default", patternIdx);
             }
          }

          if (root.has("capturePoints") && data.capturePoints.isEmpty()) {
             data.capturePoints.clear();
             var points = root.getAsJsonArray("capturePoints");
             for (var el : points) {
                data.capturePoints.add(parseCapturePoint(el.getAsJsonObject()));
             }
          }

         if (root.has("teams")) {
            JsonObject teams = root.getAsJsonObject("teams");
            for (String teamKey : new String[]{"BLUE", "RED"}) {
               if (teams.has(teamKey)) {
                  JsonObject team = teams.getAsJsonObject(teamKey);
                  if (team.has("spawn")) {
                     JsonObject spawn = team.getAsJsonObject("spawn");
                     String dim = spawn.has("dimension") ? spawn.get("dimension").getAsString() : "minecraft:overworld";
                     int sx = spawn.get("x").getAsInt();
                     int sy = spawn.has("y") ? spawn.get("y").getAsInt() : 64;
                     int sz = spawn.get("z").getAsInt();
                     BlockPos pos = new BlockPos(sx, sy, sz);
                     if (teamKey.equals("BLUE")) {
                        data.blueSpawns.put(dim, pos);
                     } else {
                        data.redSpawns.put(dim, pos);
                     }
                  }
               }
            }
         }

           if (ModList.get().isLoaded("pwpcore")) {
             try {
                String[] factions = {data.blueFaction, data.redFaction};
                String[] teams = {"BLUE", "RED"};
                for (int i = 0; i < 2; i++) {
                   String faction = factions[i];
                   if (faction == null || faction.isEmpty() || faction.equals("none")) continue;
                    JsonObject response = CoreServerApi.getFactionKits(faction);
                   if (response != null && response.has("data")) {
                      data.loadKitsFromApi(teams[i], response);
                      LOGGER.info("Loaded {} kits from API for faction {} on team {}",
                         response.getAsJsonArray("data").size(), faction, teams[i]);
                   }
                    JsonObject vehResponse = CoreServerApi.getFactionVehicles(faction);
                   if (vehResponse != null && vehResponse.has("data")) {
                      data.loadFactionVehiclesFromApi(teams[i], vehResponse);
                      LOGGER.info("Loaded faction vehicles from API for faction {} on team {}", faction, teams[i]);
                   }
                }
             } catch (Exception e) {
                LOGGER.warn("Failed to load kits/vehicles from API: {}", e.getMessage());
             }
          } else {
             LOGGER.info("Core API not available, using NBT-based kits/vehicles");
          }

          if (root.has("vehicleSpawners")) {
             JsonObject vsObj = root.getAsJsonObject("vehicleSpawners");
             if (vsObj.has("BLUE")) {
                data.parseConfigVehicleSpawners(vsObj.getAsJsonArray("BLUE"), "BLUE", level);
             }
             if (vsObj.has("RED")) {
                data.parseConfigVehicleSpawners(vsObj.getAsJsonArray("RED"), "RED", level);
             }
          }

          if (data.hideDeathMessages) {
             level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_SHOWDEATHMESSAGES).set(false, level.getServer());
          }
          if (data.disableNaturalRegen) {
              level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_NATURAL_REGENERATION).set(false, level.getServer());
           }
           if (data.disableBlockDrops) {
              level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS).set(false, level.getServer());
           }
           if (data.disableEntityDrops) {
              level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOENTITYDROPS).set(false, level.getServer());
              level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT).set(false, level.getServer());
           }
           if (data.disableFireSpread) {
              level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOFIRETICK).set(false, level.getServer());
           }
           if (data.disableWeatherCycle) {
              level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, level.getServer());
              level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_WEATHER_CYCLE).set(false, level.getServer());
           }

          data.configApplied = true;
          data.setDirty();
          LOGGER.info("Map config applied successfully for map: {}", root.get("name").getAsString());

          // INVASION mode: set all points to defender, adjust tickets
          String mode = root.has("mode") ? root.get("mode").getAsString() : "aas";
          data.gameMode = mode;
          if (mode.equals("invasion") && !data.capturePoints.isEmpty()) {
             String defender = root.has("invasionDefender") ? root.get("invasionDefender").getAsString() : "RED";
             data.invasionDefender = defender;
              for (WarfareWorldData.CapturePoint cp : data.capturePoints) {
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
                      data.invasionCaptureBonus = inv.get("captureBonus").getAsInt();
                   }
                }
             }
             LOGGER.info("INVASION mode: all points set to defender {}, capture bonus={}", defender, data.invasionCaptureBonus);
          }

          // Start waiting phase (5 min timer), then voting
          data.waitingActive = true;
          data.waitingTimer = 300;
          data.voteActive = false;
          data.votes.clear();
          PacketHandler.sendToAllClients(level, data);
       } catch (IOException e) {
          LOGGER.error("Failed to read map_config.json", e);
       }
    }

    private static WarfareWorldData.CapturePoint parseCapturePoint(JsonObject cp) {
       String name = cp.get("name").getAsString();
       String shape = cp.has("shape") ? cp.get("shape").getAsString() : "cylinder";
       JsonObject p1 = cp.getAsJsonObject("pos1");
       JsonObject p2 = cp.getAsJsonObject("pos2");
       AABB aabb = new AABB(
          p1.get("x").getAsInt(), p1.get("y").getAsInt(), p1.get("z").getAsInt(),
          p2.get("x").getAsInt(), p2.get("y").getAsInt(), p2.get("z").getAsInt()
       );
       int bp = cp.has("bluePriority") ? cp.get("bluePriority").getAsInt() : 10;
       int rp = cp.has("redPriority") ? cp.get("redPriority").getAsInt() : 10;
       int ctm = cp.has("captureTimeMinutes") ? cp.get("captureTimeMinutes").getAsInt() : 2;
       int pen = cp.has("ticketPenalty") ? cp.get("ticketPenalty").getAsInt() : 60;
       int deduct = cp.has("captureDeduction") ? cp.get("captureDeduction").getAsInt() : 0;
       int lockMin = cp.has("lockDurationMinutes") ? cp.get("lockDurationMinutes").getAsInt() : 0;
       return new WarfareWorldData.CapturePoint(name, aabb, bp, rp, ctm, pen, deduct, shape, lockMin);
    }
}
