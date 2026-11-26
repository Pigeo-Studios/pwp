package com.pigeostudios.pwp.warfare.events;

import static com.pigeostudios.pwp.warfare.events.PlayerItemEvents.*;
import static com.pigeostudios.pwp.warfare.events.PlayerLifecycleEvents.*;
import static com.pigeostudios.pwp.warfare.events.VehicleLossEvents.*;

import com.pigeostudios.pwp.warfare.block.GameStartTriggerBlock;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.RallyPointBlock;
import com.pigeostudios.pwp.warfare.block.RallyPointBlockEntity;
import com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlockEntity;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.match.CaptureLogic;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.network.PacketCaptureNotification;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketNotification;
import com.pigeostudios.pwp.warfare.network.PacketOpenVictoryScreen;
import com.pigeostudios.pwp.warfare.network.PacketSyncMapPlayers;
import com.pigeostudios.pwp.warfare.network.PacketSyncPoint;
import com.pigeostudios.pwp.warfare.network.PacketSyncSpawners;
import com.pigeostudios.pwp.warfare.network.ResupplyHandler;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import com.pigeostudios.pwp.warfare.squad.SquadActions;
import com.pigeostudios.pwp.warfare.staging.StagingPackets;
import com.pigeostudios.pwp.warfare.staging.StagingService;
import com.pigeostudios.pwp.warfare.stats.MatchStatsTracker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EventBusSubscriber(modid = "pwpwarfare", bus = Bus.FORGE)
// Основной обработчик игровой логики
// Управляет захватом точек, тикетами, возрождением, защитными зонами и артиллерией
public class GameLogicEvents {
    public static final Map<UUID, String> pendingRespawnLocations = new HashMap<>();
    public static final Map<UUID, String> pendingTeams = new HashMap<>();
    public static final Map<UUID, BlockPos> pendingSpawnPositions = new HashMap<>();
    public static final Map<UUID, Map<Integer, CompoundTag>> PERSISTENT_NBT_STORAGE = new HashMap<>();
    static final Map<String, Boolean> lastBlueBlockedMap = new HashMap<>();
    static final Map<String, Boolean> lastRedBlockedMap = new HashMap<>();
    static final Logger LOGGER = LoggerFactory.getLogger(GameLogicEvents.class);

    /** Матч окончен и сервер уходит в лобби (экран победы) — админ-рестарт в это время запрещён. */
    public static boolean returningToLobby() {
        return com.pigeostudios.pwp.warfare.server.MatchShutdown.returningToLobby();
    }

    /** Запуск обратного отсчёта без force (автоматические пути: конец ожидания, голосование «готовы»). */
    public static com.pigeostudios.pwp.warfare.layout.StartGate.Decision startGameCountdown(ServerLevel level) {
        return startGameCountdown(level, false);
    }

    /**
     * Запуск обратного отсчёта перед началом игры — ЕДИНСТВЕННАЯ точка старта (панель, команда, автостарт).
     * ERROR валидатора раскладки блокируют старт (флаг {@code blockStartOnLayoutErrors}, по умолчанию вкл.),
     * если не {@code force}. Заблокировано — ничего не меняется, админам онлайн уходит сообщение.
     */
    public static com.pigeostudios.pwp.warfare.layout.StartGate.Decision startGameCountdown(
            ServerLevel level, boolean force) {
        WarfareWorldData data = WarfareWorldData.get(level);
        com.pigeostudios.pwp.warfare.config.MapLayoutState.log("match start");
        var decision = com.pigeostudios.pwp.warfare.admin.MatchAdmin.gate(force);
        if (decision.blocked()) {
            com.pigeostudios.pwp.warfare.admin.MatchAdmin.notifyBlocked(level, decision);
            return decision;
        }
        if (decision.forced()) {
            LOGGER.warn(
                    "[MapLayout] Старт матча ПРИНУДИТЕЛЬНО (force) при {} ошибках раскладки",
                    decision.errors().size());
        }
        data.match.waitingActive = false;
        // голосование «готовы» живёт только до старта (иначе панель висела бы весь матч после «пропустить ожидание»)
        data.match.voteActive = false;
        data.match.countdownTicks = 100;
        data.match.countdownActive = true;
        data.setDirty();
        return decision;
    }

    public static void cancelCountdown(ServerLevel level) {
        WarfareWorldData data = WarfareWorldData.get(level);
        data.match.countdownActive = false;
        data.match.countdownTicks = 0;
        data.setDirty();
    }

    public static void cancelCountdown() {}

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.END) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                long globalTick = GameClock.serverTicks(server);

                for (ServerLevel level : server.getAllLevels()) {
                    WarfareWorldData data = WarfareWorldData.get(level);
                    com.pigeostudios.pwp.warfare.match.MainZoneGuard.tick(level, data);
                    boolean blueIsBleeding = false;
                    boolean redIsBleeding = false;
                    if (data.match.isPaused) continue;
                    if (data.squadRegistry.blueCmdVoteActive) {
                        data.squadRegistry.blueCmdVoteTimer--;
                        checkCmdVoteStatus(level, server, data, "BLUE");
                    }

                    if (data.squadRegistry.redCmdVoteActive) {
                        data.squadRegistry.redCmdVoteTimer--;
                        checkCmdVoteStatus(level, server, data, "RED");
                    }

                    if (data.match.waitingActive) {
                        if (globalTick % 20 == 0 && data.match.waitingTimer > 0) {
                            data.match.waitingTimer--;
                            data.setDirty();
                            int secs = data.match.waitingTimer;
                            // вместо ванильного титра — плашка «ПОДГОТОВКА · М:СС» на клиенте (этап 4)
                            StagingPackets.broadcastState(level, data);
                            StagingService.tickVehicles(level, data);
                            if (secs == 300 || secs == 240 || secs == 180 || secs == 120) {
                                level.playSound(
                                        null,
                                        new BlockPos(0, 100, 0),
                                        SoundEvents.NOTE_BLOCK_PLING.value(),
                                        SoundSource.MASTER,
                                        1.0F,
                                        1.0F);
                            }
                            if (secs == 60) {
                                level.playSound(
                                        null,
                                        new BlockPos(0, 100, 0),
                                        SoundEvents.NOTE_BLOCK_PLING.value(),
                                        SoundSource.MASTER,
                                        1.0F,
                                        2.0F);
                                broadcastTranslatable(level, "pwpwarfare.staging.one_minute", ChatFormatting.GOLD);
                            }
                            if (secs == 30) {
                                level.playSound(
                                        null,
                                        new BlockPos(0, 100, 0),
                                        SoundEvents.NOTE_BLOCK_PLING.value(),
                                        SoundSource.MASTER,
                                        1.0F,
                                        2.0F);
                                broadcastTranslatable(level, "pwpwarfare.staging.thirty_seconds", ChatFormatting.GOLD);
                            }
                            if (secs <= 10 && secs > 0) {
                                level.playSound(
                                        null,
                                        new BlockPos(0, 100, 0),
                                        SoundEvents.NOTE_BLOCK_HAT.value(),
                                        SoundSource.MASTER,
                                        1.0F,
                                        1.0F);
                            }
                        }
                        if (data.match.waitingTimer <= 0) {
                            data.match.waitingActive = false;
                            // голосование «готовы» живёт только в ожидании
                            data.match.voteActive = false;
                            data.setDirty();
                            // старт может заблокировать валидатор раскладки — «Игра начинается» только при успехе
                            if (!startGameCountdown(level).blocked()) {
                                broadcastTranslatable(level, "pwpwarfare.staging.starting", ChatFormatting.GREEN);
                            }
                            StagingPackets.broadcastState(level, data);
                            PacketHandler.sendToAllClients(level, data);
                        }
                    }

                    if (data.match.voteActive && !data.match.isGameStarted) {
                        // в ожидании голосование «готовы» идёт само (решение 4): таймер = таймер ожидания,
                        // по таймеру старт делает ветка ожидания, здесь — только досрочный старт
                        if (data.match.waitingActive) data.match.voteTimer = data.match.waitingTimer;
                        if (globalTick % 20 == 0 && data.match.voteTimer > 0) {
                            data.match.voteTimer--;
                            data.setDirty();
                            boolean bReady = isTeamReady(level, "Blue", data);
                            boolean rReady = isTeamReady(level, "Red", data);
                            if (bReady != data.match.blueReady
                                    || rReady != data.match.redReady
                                    || data.match.voteTimer % 5 == 0
                                    || data.match.voteTimer < 5) {
                                data.match.blueReady = bReady;
                                data.match.redReady = rReady;
                                PacketHandler.sendToAllClients(level, data);
                            }
                        }

                        boolean allReady = data.match.blueReady && data.match.redReady && anyTeamPlayers(level);
                        if (allReady || (!data.match.waitingActive && data.match.voteTimer <= 0)) {
                            data.match.voteActive = false;
                            data.setDirty();
                            if (!startGameCountdown(level).blocked() && allReady) {
                                broadcastTranslatable(level, "pwpwarfare.staging.all_ready", ChatFormatting.GREEN);
                            }
                            StagingPackets.broadcastState(level, data);
                            PacketHandler.sendToAllClients(level, data);
                        }
                    }

                    if (data.match.invasionSetupActive && globalTick % 20 == 0) {
                        data.match.invasionSetupTimer--;
                        data.setDirty();
                        sendSyncPacket(level, data);
                        int secs = data.match.invasionSetupTimer;
                        if (secs == 60) {
                            level.playSound(
                                    null,
                                    new BlockPos(0, 100, 0),
                                    SoundEvents.NOTE_BLOCK_PLING.value(),
                                    SoundSource.MASTER,
                                    1.0F,
                                    2.0F);
                            broadcastMessage(
                                    level,
                                    Component.translatable("pwpwarfare.msg.invasion.setup_1min"),
                                    ChatFormatting.GOLD);
                        }
                        if (secs == 30) {
                            level.playSound(
                                    null,
                                    new BlockPos(0, 100, 0),
                                    SoundEvents.NOTE_BLOCK_PLING.value(),
                                    SoundSource.MASTER,
                                    1.0F,
                                    2.0F);
                            broadcastMessage(
                                    level,
                                    Component.translatable("pwpwarfare.msg.invasion.setup_30s"),
                                    ChatFormatting.GOLD);
                        }
                        if (secs <= 10 && secs > 0) {
                            level.playSound(
                                    null,
                                    new BlockPos(0, 100, 0),
                                    SoundEvents.NOTE_BLOCK_HAT.value(),
                                    SoundSource.MASTER,
                                    1.0F,
                                    1.0F);
                        }
                        if (secs <= 0) {
                            data.match.invasionSetupActive = false;
                            data.setDirty();
                            sendSyncPacket(level, data);
                            sendTitleToLevel(
                                    level,
                                    Component.translatable("pwpwarfare.msg.invasion.attack_title"),
                                    ChatFormatting.GREEN);
                            broadcastMessage(
                                    level,
                                    Component.translatable("pwpwarfare.msg.invasion.setup_over"),
                                    ChatFormatting.GREEN);
                        }
                    }

                    if (globalTick % 2 == 0) {
                        List<MapPlayerInfo> allPlayersInfo = buildPlayerInfo(level.players(), data);
                        PacketSyncMapPlayers allPlayersPacket = new PacketSyncMapPlayers(allPlayersInfo);

                        // кто кого видит — TeamVisibility через PacketHandler.viewerKey (этап 12): всё — только
                        // модератор-наблюдатель; наказанный спектатор и игроки — только своя команда
                        for (ServerPlayer p : level.players()) {
                            String key = PacketHandler.viewerKey(p);
                            if (com.pigeostudios.pwp.warfare.match.TeamVisibility.ALL.equals(key)) {
                                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), allPlayersPacket);
                            } else if (!com.pigeostudios.pwp.warfare.match.TeamVisibility.NONE.equals(key)) {
                                List<MapPlayerInfo> teamOnly = allPlayersInfo.stream()
                                        .filter(info -> key.equalsIgnoreCase(info.team))
                                        .toList();
                                PacketHandler.INSTANCE.send(
                                        PacketDistributor.PLAYER.with(() -> p), new PacketSyncMapPlayers(teamOnly));
                            }
                        }

                        if (globalTick % 5 == 0) {
                            boolean needsSync = false;

                            for (WarfareWorldData.VehicleRecord record : data.vehicles.markedVehicles) {
                                Entity vEntity = level.getEntity(record.uuid);
                                if (vEntity != null && vEntity.isAlive()) {
                                    float currentYaw = vEntity.getYRot();
                                    if (record.x != vEntity.getX() || record.yaw != currentYaw) {
                                        record.x = vEntity.getX();
                                        record.y = vEntity.getY();
                                        record.z = vEntity.getZ();
                                        record.yaw = currentYaw;
                                        needsSync = true;
                                    }
                                }
                            }

                            if (needsSync) {
                                data.setDirty();
                                PacketHandler.sendToAllClients(level, data);
                            }
                        }

                        if (globalTick % 40 == 0) {
                            boolean changed = false;
                            Iterator<WarfareWorldData.VehicleRecord> it = data.vehicles.markedVehicles.iterator();

                            while (it.hasNext()) {
                                WarfareWorldData.VehicleRecord record = it.next();
                                Entity vEntity = level.getEntity(record.uuid);
                                if (vEntity != null && !vEntity.isAlive()) {
                                    it.remove();
                                    changed = true;
                                }
                            }

                            if (changed) {
                                data.setDirty();
                                PacketHandler.sendToAllClients(level, data);
                            }
                        }

                        if (globalTick % 40 == 0) {
                            List<Entity> wrecksToProcess = new ArrayList<>();
                            for (WarfareWorldData.VehicleRecord record : data.vehicles.markedVehicles) {
                                Entity vehicle = level.getEntity(record.uuid);
                                if (vehicle != null
                                        && vehicle.isAlive()
                                        && !(vehicle instanceof LivingEntity)
                                        && WarfareData.of(vehicle).contains(WarfareKeys.TICKET_PENALTY)) {
                                    float health = getVehicleHealth(vehicle);
                                    if (health <= 0.0F) {
                                        wrecksToProcess.add(vehicle);
                                    }
                                }
                            }
                            for (Entity wreck : wrecksToProcess) {
                                ServerPlayer destroyer = null;
                                if (!(wreck instanceof LivingEntity)) {
                                    // Priority 1: WARFARE_LastDamager NBT tag (from ProjectileImpactEvent or
                                    // LivingHurtEvent)
                                    if (WarfareData.of(wreck).contains(WarfareKeys.LAST_DAMAGER)) {
                                        String lastUuid = WarfareData.of(wreck).getString(WarfareKeys.LAST_DAMAGER);
                                        Entity lp = level.getEntity(java.util.UUID.fromString(lastUuid));
                                        if (lp instanceof ServerPlayer sp) destroyer = sp;
                                    }
                                    // Priority 2: getLastDamageSource via reflection (for non-LivingEntity mod
                                    // vehicles)
                                    if (destroyer == null) {
                                        try {
                                            Object lastSrc = wreck.getClass()
                                                    .getMethod("getLastDamageSource")
                                                    .invoke(wreck);
                                            if (lastSrc instanceof net.minecraft.world.damagesource.DamageSource ds) {
                                                Entity srcEnt = ds.getEntity();
                                                if (srcEnt instanceof ServerPlayer sp) destroyer = sp;
                                                else if (srcEnt
                                                                instanceof
                                                                net.minecraft.world.entity.projectile.Projectile
                                                                proj
                                                        && proj.getOwner() instanceof ServerPlayer sp) destroyer = sp;
                                            }
                                        } catch (Exception ignored) {
                                            com.pwp.api.util.LogOnce.warn(
                                                    LOGGER,
                                                    "GameLogicEvents:487",
                                                    "GameLogicEvents.onServerTick: исключение",
                                                    ignored);
                                        }
                                    }
                                    if (destroyer != null) {
                                        MatchStatsTracker.get().recordVehicleDestroyed(destroyer, wreck);
                                    }
                                }
                                processWreckLoss(wreck, data, level);
                            }
                        }
                    }

                    if (globalTick % 200 == 0) {
                        long time = GameClock.worldTicks(level);
                        boolean removed = data.markers.activeMarkers.removeIf(mx -> time >= mx.expiryTick);
                        if (removed) {
                            data.setDirty();
                            PacketHandler.sendToAllClients(level, data);
                        }
                    }

                    if (globalTick % 20 == 0) {
                        int rallyRadius = (Integer) WarfareConfig.RALLY_BLOCK_RADIUS.get();
                        int rallyEnemiesRequired = (Integer) WarfareConfig.RALLY_BLOCK_ENEMY_COUNT.get();

                        for (WarfareWorldData.Squad squad : data.squadRegistry.squads) {
                            if (squad.rallyPos != null) {
                                int enemies = getEnemyCount(level, squad.rallyPos, squad.team, rallyRadius);
                                boolean currentlyBlocked = enemies >= rallyEnemiesRequired;
                                if (squad.isRallyBlocked != currentlyBlocked) {
                                    squad.isRallyBlocked = currentlyBlocked;
                                    data.setDirty();
                                    PacketHandler.syncSquadsAll(data.squadRegistry.squads);
                                }
                            }
                        }
                    }

                    if (globalTick % 400 == 0 && (Boolean) WarfareConfig.REQUIRE_OFFICER_FOR_SL.get()) {
                        for (WarfareWorldData.Squad squad : new ArrayList<>(data.squadRegistry.squads)) {
                            ServerPlayer leader = server.getPlayerList().getPlayerByName(squad.leader);
                            if (leader != null) {
                                if (leader.isCreative()) {
                                    squad.slNoOfficerSince = -1L;
                                } else {
                                    String current = WarfareData.of(leader).getString(WarfareKeys.CURRENT_KIT);
                                    String pending = WarfareData.of(leader).getString(WarfareKeys.PENDING_KIT);
                                    String kitName = !pending.isEmpty() ? pending : current;
                                    boolean hasCommandKit = false;
                                    if (!kitName.isEmpty() && !kitName.equals("Unassigned")) {
                                        String team = squad.team.toUpperCase();
                                        WarfareWorldData.KitInfo kitInfo = team.equals("BLUE")
                                                ? data.blueKits.get(kitName)
                                                : data.redKits.get(kitName);
                                        if (kitInfo != null && kitInfo.isLeaderOnly) {
                                            hasCommandKit = true;
                                        }
                                    }

                                    if (hasCommandKit) {
                                        squad.slNoOfficerSince = -1L;
                                    } else {
                                        if (squad.slNoOfficerSince == -1L) {
                                            squad.slNoOfficerSince = GameClock.worldTicks(level);
                                        }

                                        long remaining = 2400L - (GameClock.worldTicks(level) - squad.slNoOfficerSince);
                                        if (remaining > 0L) {
                                            // Предупреждение лидеру — в ленту уведомлений (раз в секунду, дедик на
                                            // клиенте)
                                            if (GameClock.worldTicks(level) % 20L == 0L) {
                                                PacketHandler.INSTANCE.send(
                                                        PacketDistributor.PLAYER.with(() -> leader),
                                                        new PacketNotification("squad.leader_kit_warning", (byte) 0));
                                            }
                                        } else {
                                            // рассылка на языке каждого игрока (раньше — английский литерал)
                                            level.getServer()
                                                    .getPlayerList()
                                                    .broadcastSystemMessage(
                                                            Component.translatable(
                                                                            "squad.disbanded_broadcast", squad.name)
                                                                    .withStyle(ChatFormatting.RED),
                                                            false);

                                            for (String memberName : new ArrayList<>(squad.members)) {
                                                ServerPlayer m =
                                                        server.getPlayerList().getPlayerByName(memberName);
                                                if (m != null) {
                                                    // Важное событие отряда — плашка в ленте уведомлений
                                                    PacketHandler.INSTANCE.send(
                                                            PacketDistributor.PLAYER.with(() -> m),
                                                            new PacketNotification("squad.disbanded_leader", (byte) 1));
                                                    WarfareData.of(m).putString(WarfareKeys.CURRENT_KIT, "Unassigned");
                                                    WarfareData.of(m).remove(WarfareKeys.PENDING_KIT);
                                                    WarfareData.of(m).remove(WarfareKeys.SQUAD_ID);
                                                    WarfareData.of(m).remove(WarfareKeys.IS_SQUAD_LEADER);
                                                    SquadActions.removeRadio(m);
                                                    m.getInventory().clearContent();
                                                    ResupplyHandler.clearCurios(m);
                                                    m.inventoryMenu.broadcastChanges();
                                                    m.containerMenu.broadcastChanges();
                                                    m.displayClientMessage(
                                                            Component.translatable("squad.disbanded_no_officer")
                                                                    .withStyle(ChatFormatting.RED),
                                                            true);
                                                }
                                            }

                                            data.squadRegistry.squads.remove(squad);
                                            data.setDirty();
                                            PacketHandler.syncSquadsAll(data.squadRegistry.squads);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    long now = GameClock.worldTicks(level);
                    boolean squadsChanged = false;

                    for (WarfareWorldData.Squad squad : data.squadRegistry.squads) {
                        if (squad.rhombusMarkers.removeIf(rm -> now >= rm.expiryTick)) {
                            squadsChanged = true;
                        }

                        if (squad.rallyPos != null && squad.rallyExpiryTick != -1L && now >= squad.rallyExpiryTick) {
                            if (level.isLoaded(squad.rallyPos)) {
                                if (level.getBlockEntity(squad.rallyPos) instanceof RallyPointBlockEntity rbe) {
                                    rbe.isDecay = true;
                                }

                                level.removeBlock(squad.rallyPos, false);
                            }

                            data.structures.blueRallies.remove(squad.rallyPos);
                            data.structures.redRallies.remove(squad.rallyPos);
                            squad.rallyPos = null;
                            squad.rallyExpiryTick = -1L;
                            squadsChanged = true;
                        }
                    }

                    if (squadsChanged) {
                        data.setDirty();
                        PacketHandler.syncSquadsAll(data.squadRegistry.squads);
                    }

                    if (globalTick % 200 == 0) {
                        long currentTick = GameClock.worldTicks(level);
                        boolean anyMarkerRemoved = false;

                        for (WarfareWorldData.Squad squad : data.squadRegistry.squads) {
                            if (squad.marker != null && currentTick >= squad.marker.expiryTick) {
                                squad.marker = null;
                                anyMarkerRemoved = true;
                            }
                        }

                        if (anyMarkerRemoved) {
                            data.setDirty();
                            PacketHandler.syncSquadsAll(data.squadRegistry.squads);
                        }
                    }

                    if (data.match.isGameStarted && globalTick % 20 == 0) {
                        // Доустановка спавнеров техники, чей чанк не был загружен на старте матча
                        data.retryPendingVehicleSpawners(level);
                    }

                    if (data.match.isGameStarted && globalTick % 100 == 0) {
                        if (data.match.disableHunger) {
                            for (ServerPlayer p : level.players()) {
                                p.getFoodData().setFoodLevel(20);
                                p.getFoodData().setSaturation(20.0F);
                            }
                        }
                    }

                    if (data.match.countdownActive) {
                        if (data.match.countdownTicks > 0) {
                            if (data.match.countdownTicks % 20 == 0) {
                                int seconds = data.match.countdownTicks / 20;
                                StagingPackets.broadcastState(level, data);
                                sendTitleToLevel(
                                        level, Component.literal(String.valueOf(seconds)), ChatFormatting.YELLOW);
                                level.playSound(
                                        null,
                                        new BlockPos(0, 100, 0),
                                        (SoundEvent) SoundEvents.NOTE_BLOCK_HAT.value(),
                                        SoundSource.MASTER,
                                        1.0F,
                                        1.0F);
                            }

                            data.match.countdownTicks--;
                            data.setDirty();
                        } else {
                            data.match.countdownActive = false;
                            // Маркеры и пути стейджинга НЕ стираются на GO (решение 2) — чистка в начале
                            // ожидания нового матча (WarfareMod, вместе со сбросом голосов)
                            data.match.isGameStarted = true;

                            if ("invasion".equals(data.match.gameMode)) {
                                data.match.invasionSetupActive = true;
                                sendTitleToLevel(
                                        level,
                                        Component.translatable("pwpwarfare.msg.invasion.setup_title"),
                                        ChatFormatting.GOLD);
                            } else {
                                sendTitleToLevel(
                                        level,
                                        Component.translatable("pwpwarfare.msg.match.go_title"),
                                        ChatFormatting.GREEN);
                            }

                            data.setDirty();

                            Scoreboard scoreboard = level.getScoreboard();
                            PlayerTeam blueTeam = scoreboard.getPlayerTeam("Blue");
                            PlayerTeam redTeam = scoreboard.getPlayerTeam("Red");
                            if (blueTeam != null) blueTeam.setAllowFriendlyFire(true);
                            if (redTeam != null) redTeam.setAllowFriendlyFire(true);
                            // ники над головой (этап 12): врагу — никогда (ванильные ники видны сквозь стены —
                            // бесплатный wallhack), своим — да; hideNametags — никому
                            PlayerTeam.Visibility tags = data.match.hideNametags
                                    ? PlayerTeam.Visibility.NEVER
                                    : PlayerTeam.Visibility.HIDE_FOR_OTHER_TEAMS;
                            if (blueTeam != null) blueTeam.setNameTagVisibility(tags);
                            if (redTeam != null) redTeam.setNameTagVisibility(tags);
                            if (data.match.disableHunger) {
                                for (ServerPlayer p : level.players()) {
                                    p.getFoodData().setFoodLevel(20);
                                    p.getFoodData().setSaturation(20.0F);
                                }
                            }
                            sendSyncPacket(level, data);

                            MatchStatsTracker.get()
                                    .startMatch(
                                            data.match.currentMapImage,
                                            data.match.gameMode != null ? data.match.gameMode : "AAS");

                            for (ServerPlayer p : level.players()) {
                                String pTeam = p.getTeam() != null
                                        ? p.getTeam().getName().toUpperCase()
                                        : "";
                                boolean isSetupAttacker = data.match.invasionSetupActive
                                        && !pTeam.equalsIgnoreCase(data.match.invasionDefender);
                                if (!isSetupAttacker) {
                                    String pending = WarfareData.of(p).getString(WarfareKeys.PENDING_KIT);
                                    String current = WarfareData.of(p).getString(WarfareKeys.CURRENT_KIT);
                                    String kitToApply = !pending.isEmpty() ? pending : current;
                                    if (kitToApply != null
                                            && !kitToApply.isEmpty()
                                            && !kitToApply.equals("Unassigned")) {
                                        ResupplyHandler.tryApplyPendingKit(p, data);
                                    }
                                }
                            }

                            for (BlockPos p : data.structures.triggerBlocks) {
                                if (level.isLoaded(p)) {
                                    BlockState st = level.getBlockState(p);
                                    if (st.is((Block) ModBlocks.GAME_START_TRIGGER.get())) {
                                        level.setBlock(
                                                p, (BlockState) st.setValue(GameStartTriggerBlock.POWERED, true), 3);
                                        level.scheduleTick(p, (Block) ModBlocks.GAME_START_TRIGGER.get(), 20);
                                    }
                                }
                            }

                            // техника стоит с начала ожидания (решение 8) — повторно не заполняем
                            StagingService.onGo(level, data);
                            StagingPackets.broadcastState(level, data);

                            sendSyncPacket(level, data);
                        }
                    }

                    if (data.match.isGameStarted && (Boolean) WarfareConfig.LOW_TICKETS_SIREN.get()) {
                        if (data.match.blueTickets <= 50 && data.match.blueTickets > 0 && !data.match.playedBlueSiren) {
                            playSirenForTeam(level, "Blue");
                            data.match.playedBlueSiren = true;
                            data.setDirty();
                        }

                        if (data.match.redTickets <= 50 && data.match.redTickets > 0 && !data.match.playedRedSiren) {
                            playSirenForTeam(level, "Red");
                            data.match.playedRedSiren = true;
                            data.setDirty();
                        }
                    }

                    boolean gameEnded = data.match.blueTickets <= 0 || data.match.redTickets <= 0;
                    if (data.match.isGameStarted
                            && !data.match.invasionSetupActive
                            && !gameEnded
                            && !data.match.capturePoints.isEmpty()) {
                        int totalPoints = data.match.capturePoints.size();
                        long blueOwned = data.match.capturePoints.stream()
                                .filter(p -> p.owner.equalsIgnoreCase("BLUE"))
                                .count();
                        long redOwned = data.match.capturePoints.stream()
                                .filter(p -> p.owner.equalsIgnoreCase("RED"))
                                .count();

                        if ("invasion".equals(data.match.gameMode)) {
                            // No ticket bleed in INVASION for either team
                            blueIsBleeding = false;
                            redIsBleeding = false;
                        } else {
                            // Standard AAS bleed
                            blueIsBleeding = blueOwned == 0L && redOwned >= totalPoints - 1 && totalPoints > 0;
                            redIsBleeding = redOwned == 0L && blueOwned >= totalPoints - 1 && totalPoints > 0;
                            if (globalTick % 40 == 0) {
                                boolean changed = false;
                                if (blueIsBleeding) {
                                    data.match.blueTickets--;
                                    changed = true;
                                }

                                if (redIsBleeding) {
                                    data.match.redTickets--;
                                    changed = true;
                                }

                                if (changed) {
                                    checkGameOver(level, data);
                                    data.setDirty();
                                }
                            }
                        }
                    }

                    if (globalTick % 20 == 0) {
                        validateAndSync(level, false, blueIsBleeding, redIsBleeding);
                    }

                    if (globalTick % 20 == 0) {
                        for (WarfareWorldData.SpawnerInfo info : data.vehicles.spawnerInfos) {
                            info.isAlive = data.vehicles.markedVehicles.stream()
                                    .anyMatch(v -> v.spawnerPos != null && v.spawnerPos.equals(info.pos));
                            if (level.getBlockEntity(info.pos) instanceof VehicleSpawnerBlockEntity spawner) {
                                info.targetSpawnTick = spawner.targetSpawnTick;
                                info.hasSpawnedOnce = spawner.hasSpawnedOnce;
                            }
                        }
                        PacketHandler.INSTANCE.send(
                                PacketDistributor.DIMENSION.with(level::dimension),
                                new PacketSyncSpawners(new java.util.ArrayList<>(data.vehicles.spawnerInfos)));
                    }

                    if (data.match.countdownActive && data.match.countdownTicks == 1) {
                        long cdTicks =
                                ((Integer) WarfareConfig.ART_STRIKE_COOLDOWN_MINUTES.get()).intValue() * 60L * 20L;
                        data.match.blueArtStrikeCD = GameClock.worldTicks(level) + cdTicks;
                        data.match.redArtStrikeCD = GameClock.worldTicks(level) + cdTicks;
                    }

                    if (data.match.blueArtRequest != null) {
                        data.match.blueArtRequest.timer--;
                        if (data.match.blueArtRequest.timer <= 0) {
                            data.match.blueArtRequest = null;
                            data.setDirty();
                            PacketHandler.sendToAllClients(level, data);
                        }
                    }

                    if (data.match.redArtRequest != null) {
                        data.match.redArtRequest.timer--;
                        if (data.match.redArtRequest.timer <= 0) {
                            data.match.redArtRequest = null;
                            data.setDirty();
                            PacketHandler.sendToAllClients(level, data);
                        }
                    }
                }
            }

            // возврат в лобби по таймеру + stop.signal от лобби (M5: server/MatchShutdown)
            com.pigeostudios.pwp.warfare.server.MatchShutdown.tick(server);
            // сторож: пустой/никем не занятый/слишком долгий матч не держит слот лобби (этап 4 ROADMAP)
            com.pigeostudios.pwp.warfare.server.MatchWatchdogTicker.tick(server);
            // кик за AFK (этап 10a, community.afkKickMinutes)
            com.pigeostudios.pwp.warfare.server.AfkTicker.tick(server);
        }
    }

    static void spawnArtShell(ServerLevel level, BlockPos pos) {
        int rad = (Integer) WarfareConfig.ART_STRIKE_RADIUS.get();
        double x = pos.getX() + level.random.nextDouble() * rad * 2.0 - rad;
        double z = pos.getZ() + level.random.nextDouble() * rad * 2.0 - rad;
        double y = level.getMaxBuildHeight() - 2;
        EntityType<?> shellType = (EntityType<?>)
                ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("superbwarfare", "mortar_shell"));
        if (shellType != null) {
            Entity shell = shellType.create(level);
            if (shell != null) {
                shell.setPos(x, y, z);
                shell.setDeltaMovement(0.0, 0.0, 0.0);
                level.addFreshEntity(shell);
            }
        }
    }

    static boolean isTeamReady(ServerLevel level, String teamName, WarfareWorldData data) {
        List<ServerPlayer> allPlayers = level.players();
        if (allPlayers.isEmpty()) return false;
        List<ServerPlayer> teamPlayers = allPlayers.stream()
                .filter(p -> p.getTeam() != null && p.getTeam().getName().equalsIgnoreCase(teamName))
                .toList();
        if (teamPlayers.isEmpty()) {
            return true;
        }

        long yesVotes = teamPlayers.stream()
                .filter(p -> data.match.votes.getOrDefault(p.getUUID(), false))
                .count();
        float percent = (float) yesVotes / teamPlayers.size() * 100.0F;
        return percent >= ((Integer) WarfareConfig.VOTE_REQUIRED_PERCENTAGE.get()).intValue();
    }

    static List<MapPlayerInfo> buildPlayerInfo(List<ServerPlayer> players, WarfareWorldData data) {
        List<MapPlayerInfo> infoList = new ArrayList<>();

        for (ServerPlayer p : players) {
            String pName = p.getScoreboardName();
            String pTeam = p.getTeam() != null ? p.getTeam().getName().toUpperCase() : "NEUTRAL";
            int squadId = WarfareData.of(p).getInt(WarfareKeys.SQUAD_ID);
            if (squadId == 0) {
                squadId = -1;
            }

            boolean isLeader = WarfareData.of(p).getBoolean(WarfareKeys.IS_SQUAD_LEADER);

            for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
                if (s.members.contains(pName)) {
                    squadId = s.id;
                    if (s.leader.equals(pName)) {
                        isLeader = true;
                    }
                    break;
                }
            }

            boolean inVehicle = p.getVehicle() != null;
            int vId = -1;
            int seatIdx = -1;
            if (inVehicle) {
                Entity vehicle = p.getVehicle();
                vId = vehicle.getId();
                seatIdx = vehicle.getPassengers().indexOf(p);
            }

            boolean downed = WarfareData.of(p).getBoolean(WarfareKeys.IS_DOWNED);
            long shout = WarfareData.of(p).getLong(WarfareKeys.LAST_MEDIC_SHOUT_TIME_MS);
            infoList.add(new MapPlayerInfo(
                    pName,
                    p.getUUID(),
                    p.getX(),
                    p.getZ(),
                    p.getYRot(),
                    squadId,
                    isLeader,
                    downed,
                    shout,
                    inVehicle,
                    vId,
                    seatIdx,
                    pTeam));
        }

        return infoList;
    }

    @SubscribeEvent
    public static void onWorldTick(LevelTickEvent event) {
        if (!event.level.isClientSide && event.phase == Phase.END) {
            ServerLevel level = (ServerLevel) event.level;
            WarfareWorldData data = WarfareWorldData.get(level);
            if (data.match.isPaused) return;
            Set<UUID> playersInPreciseZones = new HashSet<>();

            for (WarfareWorldData.CapturePoint point : data.match.capturePoints) {
                List<ServerPlayer> playersInBox = level.getEntitiesOfClass(ServerPlayer.class, point.area);
                int blueOnPointLiving = 0;
                int redOnPointLiving = 0;

                for (ServerPlayer p : playersInBox) {
                    if (p.isAlive()
                            && !p.isSpectator()
                            && !WarfareData.of(p).getBoolean(WarfareKeys.IS_DOWNED)
                            && point.isInside(p.position())
                            && p.getTeam() != null) {
                        if (p.getTeam().getName().equalsIgnoreCase("Blue")) {
                            blueOnPointLiving++;
                        } else if (p.getTeam().getName().equalsIgnoreCase("Red")) {
                            redOnPointLiving++;
                        }
                    }
                }

                String dominantTeam = "NONE";
                int alliesOnPoint = 0;
                boolean isContested = false;
                boolean isTimeLocked = GameClock.worldTicks(level) < point.lockedUntilTick;
                if (blueOnPointLiving > 0 && redOnPointLiving > 0) {
                    if (blueOnPointLiving >= redOnPointLiving * 2) {
                        dominantTeam = "BLUE";
                        alliesOnPoint = blueOnPointLiving;
                    } else if (redOnPointLiving >= blueOnPointLiving * 2) {
                        dominantTeam = "RED";
                        alliesOnPoint = redOnPointLiving;
                    } else {
                        isContested = true;
                    }
                } else if (blueOnPointLiving > 0) {
                    dominantTeam = "BLUE";
                    alliesOnPoint = blueOnPointLiving;
                } else if (redOnPointLiving > 0) {
                    dominantTeam = "RED";
                    alliesOnPoint = redOnPointLiving;
                }

                float multiplier = 1.0F;
                if (alliesOnPoint > 1) {
                    multiplier += (alliesOnPoint - 1) * 0.5F;
                }

                if (multiplier > 4.0F) {
                    multiplier = 4.0F;
                }

                int currentRate = 0;
                String teamToSync = "NONE";
                if (!dominantTeam.equals("NONE") && !isContested && !isTimeLocked) {
                    boolean isOwner = point.owner.equals(dominantTeam);
                    boolean isFullyCaptured = isOwner && point.progress >= 1.0F;
                    if (!isFullyCaptured && canCapture(point, dominantTeam, data)) {
                        teamToSync = dominantTeam;
                        boolean isNeutralizing = !point.owner.equals("NEUTRAL") && !point.owner.equals(dominantTeam)
                                || point.owner.equals("NEUTRAL")
                                        && !point.capturingTeam.equals("NONE")
                                        && !point.capturingTeam.equals(dominantTeam);
                        currentRate = Math.round(multiplier);
                        if (isNeutralizing) {
                            currentRate = -currentRate;
                        }
                    }
                }

                for (ServerPlayer p : playersInBox) {
                    if (point.isInside(p.position())) {
                        playersInPreciseZones.add(p.getUUID());
                        boolean lockedUI = false;
                        String nextObjectiveForPlayer = "";
                        if (p.getTeam() != null) {
                            String teamKey = p.getTeam().getName().equalsIgnoreCase("Blue") ? "BLUE" : "RED";
                            if (isTimeLocked) {
                                lockedUI = true;
                                long totalSeconds = (point.lockedUntilTick - GameClock.worldTicks(level)) / 20L;
                                nextObjectiveForPlayer =
                                        String.format("LOCKED: %dм %dс", totalSeconds / 60L, totalSeconds % 60L);
                            } else if (!canCapture(point, teamKey, data)) {
                                lockedUI = true;
                                nextObjectiveForPlayer = findRequiredPointName(point, teamKey, data);
                            }
                        }

                        PacketHandler.INSTANCE.send(
                                PacketDistributor.PLAYER.with(() -> p),
                                new PacketSyncPoint(
                                        true,
                                        point.name,
                                        point.owner,
                                        point.progress,
                                        lockedUI,
                                        nextObjectiveForPlayer,
                                        isContested,
                                        teamToSync,
                                        currentRate));
                    }
                }

                boolean isBeingActivelyCaptured = false;
                if (!dominantTeam.equals("NONE")
                        && !isContested
                        && !isTimeLocked
                        && canCapture(point, dominantTeam, data)) {
                    isBeingActivelyCaptured = true;
                    float baseSpeed = 1.0F / (point.captureTimeMinutes * 60 * 20);
                    String oldOwner = point.owner;
                    handleTeamInfluence(point, dominantTeam, multiplier, baseSpeed, data, level);
                    if (!point.owner.equals(oldOwner)) {
                        for (ServerPlayer p : playersInBox) {
                            if (p.isAlive()
                                    && !p.isSpectator()
                                    && p.getTeam() != null
                                    && p.getTeam().getName().equalsIgnoreCase(dominantTeam)) {
                                MatchStatsTracker.get().recordCapture(p);
                            }
                        }
                    }
                    if (!point.owner.equals(oldOwner)
                            && !point.owner.equals("NEUTRAL")
                            && point.lockDurationMinutes > 0) {
                        point.lockedUntilTick = GameClock.worldTicks(level) + point.lockDurationMinutes * 60L * 20L;
                        data.setDirty();
                    }
                }

                if (!isBeingActivelyCaptured && !isContested && !isTimeLocked) {
                    float baseSpeed = 1.0F / (point.captureTimeMinutes * 60 * 20);
                    if (point.owner.equals("NEUTRAL") && point.progress > 0.0F) {
                        point.progress -= baseSpeed / 2.0F;
                        if (point.progress <= 0.0F) {
                            point.progress = 0.0F;
                            point.capturingTeam = "NONE";
                        }
                    } else if (!point.owner.equals("NEUTRAL") && point.progress < 1.0F) {
                        point.progress += baseSpeed / 2.0F;
                        if (point.progress >= 1.0F) {
                            point.progress = 1.0F;
                            point.capturingTeam = "NONE";
                        }
                    }
                }

                if (GameClock.worldTicks(level) % 10L == 0L) {
                    boolean isPointActive =
                            point.progress > 0.0F && point.progress < 1.0F || !point.capturingTeam.equals("NONE");
                    if (isPointActive) {
                        for (ServerPlayer player : level.players()) {
                            if (!playersInPreciseZones.contains(player.getUUID())) {
                                PacketHandler.INSTANCE.send(
                                        PacketDistributor.PLAYER.with(() -> player),
                                        new PacketSyncPoint(
                                                false,
                                                point.name,
                                                point.owner,
                                                point.progress,
                                                false,
                                                "",
                                                isContested,
                                                point.capturingTeam,
                                                0));
                            }
                        }
                    }
                }
            }

            for (ServerPlayer player : level.players()) {
                if (!playersInPreciseZones.contains(player.getUUID())) {
                    PacketHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new PacketSyncPoint(false, "", "", 0.0F, false, "", false, "NONE", 0));
                }
            }
        }
    }

    static void handleTeamInfluence(
            WarfareWorldData.CapturePoint point,
            String attackingTeam,
            float multiplier,
            float baseSpeed,
            WarfareWorldData data,
            ServerLevel level) {
        // Чистая логика захвата и тикетов — match.CaptureLogic; здесь только побочные эффекты
        float speedBoosted = baseSpeed * multiplier;
        boolean isInvasion = "invasion".equals(data.match.gameMode);
        switch (CaptureLogic.step(point, attackingTeam, speedBoosted)) {
            case CAPTURED -> {
                PacketHandler.INSTANCE.send(
                        PacketDistributor.ALL.noArg(), new PacketCaptureNotification(point.name, attackingTeam, false));
                if (isInvasion) point.invLocked = true;
                applyTickets(
                        data,
                        CaptureLogic.onCapturedPrimary(
                                point, attackingTeam, isInvasion, data.match.invasionCaptureBonus));
                if (isInvasion) {
                    // Защитник потерял последнюю точку — теряет все тикеты
                    String defenderTeam = CaptureLogic.opposite(attackingTeam);
                    boolean defenderHasPoints = false;
                    for (WarfareWorldData.CapturePoint cp : data.match.capturePoints) {
                        if (cp.owner.equals(defenderTeam)) {
                            defenderHasPoints = true;
                            break;
                        }
                    }
                    if (!defenderHasPoints) {
                        if (defenderTeam.equals("BLUE")) data.match.blueTickets = 0;
                        else data.match.redTickets = 0;
                    }
                }
                if (CaptureLogic.checkGameOverAfterCapture(point, isInvasion)) checkGameOver(level, data);
                applyTickets(data, CaptureLogic.onCapturedGain(point, attackingTeam));
                data.setDirty();
                sendSyncPacket(level, data);
            }
            case NEUTRALIZED -> {
                applyTickets(data, CaptureLogic.onNeutralizedPrimary(point, isInvasion));
                if (!isInvasion) checkGameOver(level, data);
                applyTickets(data, CaptureLogic.onNeutralizedGain(point, attackingTeam));
                CaptureLogic.resetToNeutral(point);
                PacketHandler.INSTANCE.send(
                        PacketDistributor.ALL.noArg(), new PacketCaptureNotification(point.name, attackingTeam, true));
                data.setDirty();
                sendSyncPacket(level, data);
            }
            case NONE -> {}
        }
    }

    static void applyTickets(WarfareWorldData data, CaptureLogic.Tickets delta) {
        data.match.blueTickets += delta.blue();
        data.match.redTickets += delta.red();
    }

    static void validateAndSync(ServerLevel level, boolean forceSend, boolean blueBleed, boolean redBleed) {
        WarfareWorldData data = WarfareWorldData.get(level);
        boolean changed = false;
        String dimKey = level.dimension().location().toString();
        changed |= validateRallies(level, data.structures.blueRallies);
        changed |= validateRallies(level, data.structures.redRallies);
        int hubRadius = (Integer) WarfareConfig.HUB_BLOCK_RADIUS.get();
        int hubEnemiesRequired = (Integer) WarfareConfig.HUB_BLOCK_ENEMY_COUNT.get();

        for (WarfareWorldData.HubInfo hub : data.structures.hubs) {
            if (hub.constructed && hub.dimension != null && hub.dimension.equals(dimKey) && level.isLoaded(hub.pos)) {
                boolean nowBlocked = getEnemyCount(level, hub.pos, hub.team, hubRadius) >= hubEnemiesRequired;
                if (hub.isBlocked != nowBlocked) {
                    hub.isBlocked = nowBlocked;
                    changed = true;
                }

                if (level.getBlockEntity(hub.pos) instanceof HubBlockEntity hubBe
                        && hub.materials != hubBe.getMaterials()) {
                    hub.materials = hubBe.getMaterials();
                    changed = true;
                }
            }
        }

        int rallyRadius = (Integer) WarfareConfig.RALLY_BLOCK_RADIUS.get();
        int rallyEnemiesRequired = (Integer) WarfareConfig.RALLY_BLOCK_ENEMY_COUNT.get();
        boolean anySquadStatusChanged = false;
        boolean teamBlueBlocked = false;
        boolean teamRedBlocked = false;

        for (WarfareWorldData.Squad squad : data.squadRegistry.squads) {
            if (squad.rallyPos != null && squad.rallyDimension.equals(dimKey)) {
                int enemies = getEnemyCount(level, squad.rallyPos, squad.team, rallyRadius);
                boolean currentlyBlocked = enemies >= rallyEnemiesRequired;
                if (squad.isRallyBlocked != currentlyBlocked) {
                    squad.isRallyBlocked = currentlyBlocked;
                    anySquadStatusChanged = true;
                }

                if (currentlyBlocked) {
                    if (squad.team.equalsIgnoreCase("Blue")) {
                        teamBlueBlocked = true;
                    } else {
                        teamRedBlocked = true;
                    }
                }
            }
        }

        Boolean cachedBlue = lastBlueBlockedMap.getOrDefault(dimKey, false);
        Boolean cachedRed = lastRedBlockedMap.getOrDefault(dimKey, false);
        if (forceSend
                || changed
                || anySquadStatusChanged
                || teamBlueBlocked != cachedBlue
                || teamRedBlocked != cachedRed
                || blueBleed
                || redBleed) {
            sendSyncPacket(level, data, blueBleed, redBleed, teamBlueBlocked, teamRedBlocked);
            if (anySquadStatusChanged) {
                PacketHandler.syncSquadsAll(data.squadRegistry.squads);
            }

            lastBlueBlockedMap.put(dimKey, teamBlueBlocked);
            lastRedBlockedMap.put(dimKey, teamRedBlocked);
        }

        if (changed || anySquadStatusChanged) {
            data.setDirty();
        }
    }

    static void validateAndSync(ServerLevel level, boolean forceSend) {
        validateAndSync(level, forceSend, false, false);
    }

    static boolean validateRallies(ServerLevel level, List<BlockPos> rallies) {
        return rallies.removeIf(
                pos -> level.isLoaded(pos) ? !(level.getBlockState(pos).getBlock() instanceof RallyPointBlock) : false);
    }

    static boolean isEnemyNearby(ServerLevel level, BlockPos pos, String allyTeamName, int radius, int minCount) {
        return getEnemyCount(level, pos, allyTeamName, radius) >= minCount;
    }

    static int getEnemyCount(ServerLevel level, BlockPos pos, String allyTeamName, int radius) {
        AABB checkArea = new AABB(pos).inflate(radius);
        List<ServerPlayer> enemies = level.getEntitiesOfClass(ServerPlayer.class, checkArea);
        int count = 0;

        for (ServerPlayer p : enemies) {
            if (!p.isSpectator()
                    && p.getTeam() != null
                    && !p.getTeam().getName().equalsIgnoreCase(allyTeamName)) {
                count++;
            }
        }

        return count;
    }

    public static int getEnemyCount(ServerLevel level, BlockPos pos, String allyTeamName) {
        AABB checkArea = new AABB(pos).inflate(40.0);
        List<ServerPlayer> enemies = level.getEntitiesOfClass(ServerPlayer.class, checkArea);
        int count = 0;

        for (ServerPlayer p : enemies) {
            if (!p.isSpectator()
                    && p.getTeam() != null
                    && !p.getTeam().getName().equalsIgnoreCase(allyTeamName)) {
                count++;
            }
        }

        return count;
    }

    public static void checkGameOver(ServerLevel level, WarfareWorldData data) {
        if (data.match.blueTickets <= 0) {
            data.match.blueTickets = 0;
            executeVictory(level, data, false);
        } else if (data.match.redTickets <= 0) {
            data.match.redTickets = 0;
            executeVictory(level, data, true);
        }
    }

    /** Принудительная победа (админ-панель): тот же путь, что окончание тикетов. */
    public static void forceVictory(ServerLevel level, WarfareWorldData data, boolean blueWon) {
        if (blueWon) data.match.redTickets = 0;
        else data.match.blueTickets = 0;
        executeVictory(level, data, blueWon);
    }

    static void executeVictory(ServerLevel level, WarfareWorldData data, boolean blueWon) {
        try {
            data.match.isGameStarted = false;
            // этап 3: правка раскладки «только на этот матч» пропадает с концом матча
            com.pigeostudios.pwp.warfare.server.map.MapLayoutService.onMatchEnded(level);
            data.setDirty();

            String winner = blueWon ? "BLUE" : "RED";

            // Capture stats snapshot + duration BEFORE finalizeMatch resets the tracker
            java.util.Map<String, com.pigeostudios.pwp.warfare.stats.PlayerMatchStats> matchStatsSnapshot =
                    new java.util.HashMap<>(MatchStatsTracker.get().getAllPlayers());
            int matchDurationSec =
                    (int) ((System.currentTimeMillis() - MatchStatsTracker.get().getStartedAt()) / 1000);

            MatchStatsTracker.get().finalizeMatch(winner, data.match.blueTickets, data.match.redTickets);

            String winnerName;
            String winnerFaction;
            if (blueWon) {
                winnerFaction = data.match.blueFaction;
                String factionName =
                        winnerFaction != null && !winnerFaction.equals("none") && !winnerFaction.equals("bluefor")
                                ? formatFactionName(winnerFaction)
                                : null;
                winnerName = factionName != null
                        ? factionName
                        : WarfareConfig.BLUE_TEAM_CUSTOM_NAME.get().toString();
                if (winnerName.isEmpty()) winnerName = "BLUE TEAM";
            } else {
                winnerFaction = data.match.redFaction;
                String factionName =
                        winnerFaction != null && !winnerFaction.equals("none") && !winnerFaction.equals("redfor")
                                ? formatFactionName(winnerFaction)
                                : null;
                winnerName = factionName != null
                        ? factionName
                        : WarfareConfig.RED_TEAM_CUSTOM_NAME.get().toString();
                if (winnerName.isEmpty()) winnerName = "RED TEAM";
            }

            String subText = (blueWon ? data.match.blueTickets : data.match.redTickets) + " tickets remaining";

            for (net.minecraft.server.level.ServerPlayer player : level.players()) {
                String uuid = player.getStringUUID();
                com.pigeostudios.pwp.warfare.stats.PlayerMatchStats ps = matchStatsSnapshot.get(uuid);
                if (ps == null) {
                    PacketHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new PacketOpenVictoryScreen(
                                    winnerName,
                                    winnerFaction,
                                    subText,
                                    blueWon,
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    0,
                                    matchDurationSec));
                } else {
                    PacketHandler.INSTANCE.send(
                            PacketDistributor.PLAYER.with(() -> player),
                            new PacketOpenVictoryScreen(
                                    winnerName,
                                    winnerFaction,
                                    subText,
                                    blueWon,
                                    ps.kills,
                                    ps.deaths,
                                    ps.vehicleKills,
                                    ps.vehiclesDestroyed,
                                    ps.airVehiclesDestroyed,
                                    ps.captures,
                                    ps.revives,
                                    0,
                                    ps.score,
                                    matchDurationSec));
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger("executeVictory")
                    .error("Error in victory sequence: {}", e.getMessage(), e);
        }

        Scoreboard scoreboard = level.getScoreboard();
        PlayerTeam blueTeam = scoreboard.getPlayerTeam("Blue");
        PlayerTeam redTeam = scoreboard.getPlayerTeam("Red");
        if (blueTeam != null) {
            blueTeam.setAllowFriendlyFire(false);
        }

        if (redTeam != null) {
            redTeam.setAllowFriendlyFire(false);
        }

        String currentDim = level.dimension().location().toString();

        for (ServerPlayer player : level.players()) {
            if (WarfareData.of(player).getBoolean(WarfareKeys.TEAM_KILL_SPECTATOR)) {
                WarfareData.of(player).remove(WarfareKeys.TEAM_KILL_SPECTATOR);
                WarfareData.of(player).remove(WarfareKeys.TEAM_KILLS);
                WarfareData.of(player).remove(WarfareKeys.BASE_RESTRICTED_UNTIL);
                player.setGameMode(GameType.SURVIVAL);
            }

            if (!player.isCreative() && !player.isSpectator()) {
                if (WarfareData.of(player).getBoolean(WarfareKeys.IS_DOWNED)) {
                    DownedHandler.revivePlayer(player);
                }

                player.setHealth(player.getMaxHealth());
                WarfareData.of(player).putString(WarfareKeys.CURRENT_KIT, "Unassigned");
                WarfareData.of(player).remove(WarfareKeys.PENDING_KIT);
                player.getInventory().clearContent();
                ResupplyHandler.clearCurios(player);
                player.inventoryMenu.broadcastChanges();
                player.containerMenu.broadcastChanges();
                String pTeam =
                        player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                BlockPos spawnPos = null;
                if (pTeam.equals("BLUE")) {
                    spawnPos = data.match.blueSpawns.get(currentDim);
                } else if (pTeam.equals("RED")) {
                    spawnPos = data.match.redSpawns.get(currentDim);
                }

                if (spawnPos != null) {
                    player.setRespawnPosition(level.dimension(), spawnPos, 0.0F, true, false);
                    if (player.isAlive()) {
                        player.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    }
                }
            }
        }

        com.pigeostudios.pwp.warfare.server.MatchShutdown.schedule(level.getServer(), 300);
    }

    static String formatFactionName(String faction) {
        return faction.replace("_", " ").toUpperCase();
    }

    public static Component formatTime(int totalSecs) {
        int m = totalSecs / 60;
        int s = totalSecs % 60;
        Component c = m >= 1
                ? (s > 0
                        ? Component.translatable("pwpwarfare.msg.time.min_sec", m, s)
                        : Component.translatable("pwpwarfare.msg.time.min", m))
                : Component.translatable("pwpwarfare.msg.time.sec", s);
        return c.copy().withStyle(ChatFormatting.YELLOW);
    }

    static void sendTitleToLevel(ServerLevel level, Component text, ChatFormatting color) {
        Component title = text.copy().withStyle(color).withStyle(ChatFormatting.BOLD);

        for (ServerPlayer player : level.players()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    static void sendTitleToAll(MinecraftServer server, Component text, ChatFormatting color) {
        Component title = text.copy().withStyle(color).withStyle(ChatFormatting.BOLD);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    static void broadcastTranslatable(ServerLevel level, String key, ChatFormatting color) {
        Component c = Component.translatable(key).withStyle(color);
        for (ServerPlayer p : level.players()) p.sendSystemMessage(c);
    }

    /** Есть ли хоть один игрок в командах (пустой сервер не «готов» к досрочному старту). */
    static boolean anyTeamPlayers(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            if (!StagingService.teamOf(p).isEmpty()) return true;
        }
        return false;
    }

    static void broadcastMessage(ServerLevel level, Component text, ChatFormatting color) {
        level.getServer().getPlayerList().broadcastSystemMessage(text.copy().withStyle(color), false);
    }

    static void sendSyncPacket(ServerLevel level, WarfareWorldData data) {
        sendSyncPacket(level, data, false, false);
    }

    static void sendSyncPacket(ServerLevel level, WarfareWorldData data, boolean blueBleed, boolean redBleed) {
        String dimKey = level.dimension().location().toString();
        boolean bBlocked = lastBlueBlockedMap.getOrDefault(dimKey, false);
        boolean rBlocked = lastRedBlockedMap.getOrDefault(dimKey, false);
        sendSyncPacket(level, data, blueBleed, redBleed, bBlocked, rBlocked);
    }

    static void sendSyncPacket(
            ServerLevel level,
            WarfareWorldData data,
            boolean blueBleed,
            boolean redBleed,
            boolean bBlocked,
            boolean rBlocked) {
        // единая сборка и рассылка по командам — PacketHandler (тут была копия createSyncPacket)
        PacketHandler.sendSync(level.players(), data, blueBleed, redBleed, bBlocked, rBlocked);
    }

    static boolean canCapture(WarfareWorldData.CapturePoint target, String team, WarfareWorldData data) {
        // правило порядка захвата — реестр режимов (AAS: цепочка, Invasion: + блокировки, Стычка: любой порядок)
        java.util.List<com.pwp.api.match.GameModes.PointState> all = new java.util.ArrayList<>();
        for (WarfareWorldData.CapturePoint p : data.match.capturePoints) all.add(pointState(p));
        return com.pwp.api.match.GameModes.canCapture(
                com.pwp.api.match.GameModes.orAas(data.match.gameMode),
                pointState(target),
                team,
                all,
                data.match.invasionDefender);
    }

    static com.pwp.api.match.GameModes.PointState pointState(WarfareWorldData.CapturePoint p) {
        return new com.pwp.api.match.GameModes.PointState(p.bluePriority, p.redPriority, p.owner, p.invLocked);
    }

    static void playSirenForTeam(ServerLevel level, String teamName) {
        for (ServerPlayer player : level.players()) {
            if (player.getTeam() != null && player.getTeam().getName().equalsIgnoreCase(teamName)) {
                player.playNotifySound((SoundEvent) ModSounds.SIREN_ALARM.get(), SoundSource.MASTER, 1.0F, 1.0F);
            }
        }
    }

    public static void checkSirenManual(ServerLevel level, WarfareWorldData data) {
        if (data.match.isGameStarted && (Boolean) WarfareConfig.LOW_TICKETS_SIREN.get()) {
            if (data.match.blueTickets <= 50 && data.match.blueTickets >= 0 && !data.match.playedBlueSiren) {
                playSirenForTeam(level, "Blue");
                data.match.playedBlueSiren = true;
                data.setDirty();
            }

            if (data.match.redTickets <= 50 && data.match.redTickets >= 0 && !data.match.playedRedSiren) {
                playSirenForTeam(level, "Red");
                data.match.playedRedSiren = true;
                data.setDirty();
            }
        }
    }

    static void checkCmdVoteStatus(ServerLevel level, MinecraftServer server, WarfareWorldData data, String team) {
        boolean isBlue = team.equals("BLUE");
        boolean active = isBlue ? data.squadRegistry.blueCmdVoteActive : data.squadRegistry.redCmdVoteActive;
        String candidateName =
                isBlue ? data.squadRegistry.blueCmdCandidateName : data.squadRegistry.redCmdCandidateName;
        int candidateId = isBlue ? data.squadRegistry.blueCmdCandidateId : data.squadRegistry.redCmdCandidateId;
        int timer = isBlue ? data.squadRegistry.blueCmdVoteTimer : data.squadRegistry.redCmdVoteTimer;
        Map<UUID, Boolean> votesMap = isBlue ? data.squadRegistry.blueCmdVotes : data.squadRegistry.redCmdVotes;
        if (active) {
            List<ServerPlayer> otherSLs = level.players().stream()
                    .filter(p -> p.getTeam() != null && p.getTeam().getName().equalsIgnoreCase(team))
                    .filter(p -> WarfareData.of(p).getBoolean(WarfareKeys.IS_SQUAD_LEADER))
                    .filter(p -> !p.getScoreboardName().equals(candidateName))
                    .toList();
            int totalVoters = otherSLs.size();
            int needed = totalVoters > 0 ? (int) Math.ceil(totalVoters / 2.0) : 1;
            long yes = votesMap.entrySet().stream()
                    .filter(e -> server.getPlayerList().getPlayer(e.getKey()) != null)
                    .filter(Entry::getValue)
                    .count();
            long no = votesMap.entrySet().stream()
                    .filter(e -> server.getPlayerList().getPlayer(e.getKey()) != null)
                    .filter(e -> !e.getValue())
                    .count();
            boolean win = totalVoters > 0 && yes >= needed;
            boolean fail = totalVoters > 0 && no >= totalVoters || timer <= 0;
            if (win || fail) {
                if (win) {
                    if (isBlue) {
                        data.squadRegistry.blueCMDId = candidateId;
                    } else {
                        data.squadRegistry.redCMDId = candidateId;
                    }

                    broadcastTeamMessage(
                            level,
                            team,
                            Component.translatable("pwpwarfare.msg.cmd.assigned", team, candidateName),
                            ChatFormatting.GREEN);
                } else {
                    broadcastTeamMessage(
                            level,
                            team,
                            Component.translatable("pwpwarfare.msg.cmd.rejected", team),
                            ChatFormatting.RED);
                }

                if (isBlue) {
                    data.squadRegistry.blueCmdVoteActive = false;
                    data.squadRegistry.blueCmdVotes.clear();
                } else {
                    data.squadRegistry.redCmdVoteActive = false;
                    data.squadRegistry.redCmdVotes.clear();
                }

                data.setDirty();
                PacketHandler.sendToAllClients(level, data);
            }
        }
    }

    static void broadcastTeamMessage(ServerLevel level, String team, Component text, ChatFormatting color) {
        Component message = text.copy().withStyle(color);

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.getTeam() != null && player.getTeam().getName().equalsIgnoreCase(team)) {
                player.sendSystemMessage(message);
            }
        }
    }
}
