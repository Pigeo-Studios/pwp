package com.pigeostudios.pwp.warfare.events;

import com.pigeostudios.pwp.warfare.block.GameStartTriggerBlock;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.RallyPointBlock;
import com.pigeostudios.pwp.warfare.block.RallyPointBlockEntity;
import com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlockEntity;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.PlayerWarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.item.ModItems;
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
import com.pigeostudios.pwp.warfare.server.MarkerManager;
import com.pigeostudios.pwp.warfare.server.PathManager;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import com.pigeostudios.pwp.warfare.squad.SquadActions;
import com.pigeostudios.pwp.warfare.staging.StagingPackets;
import com.pigeostudios.pwp.warfare.staging.StagingService;
import com.pigeostudios.pwp.warfare.stats.MatchStatsTracker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import com.pwp.coreclient.network.ConnectToServerPacket;
import com.pwp.coreserver.TransferHosts;
import com.pwp.cosmetics.CosmeticService;
import java.nio.file.Files;
import java.nio.file.Path;
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
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent.LevelTickEvent;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.EventPriority;
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
    private static final Map<String, Boolean> lastBlueBlockedMap = new HashMap<>();
    private static final Map<String, Boolean> lastRedBlockedMap = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(GameLogicEvents.class);
    private static int returnToLobbyTimer = -1;
    private static MinecraftServer returnToLobbyServer = null;

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
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() != null && !event.getPlayer().level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.getPlayer();
            if (!player.isCreative()) {
                WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                if (data.match.isGameStarted) {
                    boolean preventAll = false;

                    try {
                        preventAll = (Boolean) WarfareConfig.PREVENT_ALL_ITEM_DROPS.get();
                    } catch (Exception ex) {
                        com.pwp.api.util.LogOnce.warn(
                                LOGGER, "GameLogicEvents:156", "GameLogicEvents.onItemToss: исключение", ex);
                    }

                    if (preventAll) {
                        event.setCanceled(true);
                        player.getInventory().add(event.getEntity().getItem());
                        player.displayClientMessage(
                                Component.literal("Выбрасывание предметов ОТКЛЮЧЕНО во время игры!")
                                        .withStyle(ChatFormatting.RED),
                                true);
                    } else if (isHeavyItem(event.getEntity().getItem().getItem())) {
                        event.setCanceled(true);
                        player.getInventory().add(event.getEntity().getItem());
                        player.displayClientMessage(
                                Component.literal("Нельзя выбрасывать тяжёлый БК во время боя!")
                                        .withStyle(ChatFormatting.RED),
                                true);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTickEffects(PlayerTickEvent event) {
        if (event.phase == Phase.END && !event.player.level().isClientSide) {
            if (event.player.tickCount % 10 == 0) {
                ServerPlayer player = (ServerPlayer) event.player;
                WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                if (data.match.isGameStarted && !player.isCreative()) {
                    boolean hasHeavyItem = false;

                    for (ItemStack stack : player.getInventory().items) {
                        if (!stack.isEmpty() && isHeavyItem(stack.getItem())) {
                            hasHeavyItem = true;
                            break;
                        }
                    }

                    if (!hasHeavyItem) {
                        for (ItemStack stack : player.getInventory().offhand) {
                            if (!stack.isEmpty() && isHeavyItem(stack.getItem())) {
                                hasHeavyItem = true;
                                break;
                            }
                        }
                    }

                    if (hasHeavyItem) {
                        player.addEffect(
                                new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false, true));
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTickDriveCheck(PlayerTickEvent event) {
        if (event.phase == Phase.END && !event.player.level().isClientSide) {
            if ((Boolean) WarfareConfig.REQUIRE_SPECIALIST_TO_DRIVE.get()) {
                ServerPlayer player = (ServerPlayer) event.player;
                Entity vehicle = player.getVehicle();
                if (vehicle == null) {
                    WarfareData.of(player).remove(WarfareKeys.DRIVE_KICK_TIMER);
                    WarfareData.of(player).remove(WarfareKeys.BOARDING_GRACE);
                    WarfareData.of(player).remove(WarfareKeys.LAST_VEHICLE_ID);
                } else {
                    int lastVehicleId = WarfareData.of(player).getInt(WarfareKeys.LAST_VEHICLE_ID);
                    if (lastVehicleId != vehicle.getId()) {
                        WarfareData.of(player).putInt(WarfareKeys.LAST_VEHICLE_ID, vehicle.getId());
                        WarfareData.of(player).putInt(WarfareKeys.BOARDING_GRACE, 0);
                        WarfareData.of(player).remove(WarfareKeys.DRIVE_KICK_TIMER);
                    } else {
                        if (vehicle.getFirstPassenger() != player) {
                            WarfareData.of(player).remove(WarfareKeys.DRIVE_KICK_TIMER);
                        } else if (!player.isCreative() && !player.isSpectator()) {
                            String vType = WarfareData.of(vehicle).getString(WarfareKeys.VEHICLE_TYPE);
                            if (vType.isEmpty() || vType.equals("DEFAULT")) {
                                WarfareData.of(player).remove(WarfareKeys.DRIVE_KICK_TIMER);
                                return;
                            }
                            String pKit = KitUtil.getEffectiveKit(player);
                            if (!hasCorrectKit(vType, pKit)) {
                                player.stopRiding();
                                player.displayClientMessage(
                                        Component.literal("Вам нужен кит Механик/Пилот")
                                                .withStyle(ChatFormatting.RED),
                                        true);
                            } else {
                                WarfareData.of(player).remove(WarfareKeys.DRIVE_KICK_TIMER);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerItemLogic(PlayerTickEvent event) {}

    private static boolean isHeavyItem(Item item) {
        if (item == ModItems.AGS_AMMO.get()) {
            return true;
        }

        if (item == ModItems.M2_AMMO.get()) {
            return true;
        }

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id != null) {
            String path = id.getPath();
            if (path.equals("mortar_shell")) {
                return true;
            }

            if (path.equals("medium_anti_ground_missile")) {
                return true;
            }
        }

        return false;
    }

    private static int findItemInInventory(Inventory inventory, Item item) {
        for (int i = 0; i < inventory.items.size(); i++) {
            if (((ItemStack) inventory.items.get(i)).getItem() == item) {
                return i;
            }
        }

        return -1;
    }

    private static void moveItemToInventory(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (event.phase == Phase.END) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                long globalTick = GameClock.serverTicks(server);

                for (ServerLevel level : server.getAllLevels()) {
                    WarfareWorldData data = WarfareWorldData.get(level);
                    handleMainProtectionZones(level, data);
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
                                    level, "§e[PWP] §fФаза подготовки: осталось §e1 минута!", ChatFormatting.GOLD);
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
                                    level, "§e[PWP] §fФаза подготовки: осталось §e30 секунд!", ChatFormatting.GOLD);
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
                            sendTitleToLevel(level, "§aАТАКА!", ChatFormatting.GREEN);
                            broadcastMessage(
                                    level,
                                    "§a[PWP] Фаза подготовки окончена! Атакующие освобождены!",
                                    ChatFormatting.GREEN);
                        }
                    }

                    if (globalTick % 2 == 0) {
                        List<MapPlayerInfo> allPlayersInfo = buildPlayerInfo(level.players(), data);
                        PacketSyncMapPlayers allPlayersPacket = new PacketSyncMapPlayers(allPlayersInfo);

                        for (ServerPlayer p : level.players()) {
                            if (p.isSpectator() || p.isCreative()) {
                                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), allPlayersPacket);
                            } else if (p.getTeam() != null) {
                                String myTeamName = p.getTeam().getName();
                                List<MapPlayerInfo> teamOnly = allPlayersInfo.stream()
                                        .filter(info -> info.team.equalsIgnoreCase(myTeamName))
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
                                sendTitleToLevel(level, String.valueOf(seconds), ChatFormatting.YELLOW);
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
                                sendTitleToLevel(level, "ПОДГОТОВКА!", ChatFormatting.GOLD);
                            } else {
                                sendTitleToLevel(level, "GO!", ChatFormatting.GREEN);
                            }

                            data.setDirty();

                            Scoreboard scoreboard = level.getScoreboard();
                            PlayerTeam blueTeam = scoreboard.getPlayerTeam("Blue");
                            PlayerTeam redTeam = scoreboard.getPlayerTeam("Red");
                            if (blueTeam != null) blueTeam.setAllowFriendlyFire(true);
                            if (redTeam != null) redTeam.setAllowFriendlyFire(true);
                            if (data.match.hideNametags) {
                                if (blueTeam != null) blueTeam.setNameTagVisibility(PlayerTeam.Visibility.NEVER);
                                if (redTeam != null) redTeam.setNameTagVisibility(PlayerTeam.Visibility.NEVER);
                            }
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

            if (returnToLobbyTimer > 0) {
                returnToLobbyTimer--;
                if (returnToLobbyTimer == 0 && returnToLobbyServer != null) {
                    int lobbyPort = 25565;
                    for (ServerPlayer player :
                            returnToLobbyServer.getPlayerList().getPlayers()) {
                        // Локальные игроки получают LAN-адрес лобби, внешние — домен
                        String lobbyHost = TransferHosts.resolveTransferHost(player);
                        player.sendSystemMessage(Component.literal("§e[PWP] Возврат в лобби..."), false);
                        com.pwp.coreclient.network.PacketHandler.INSTANCE.send(
                                net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                new ConnectToServerPacket(lobbyHost, lobbyPort));
                    }
                    returnToLobbyServer.halt(false);
                    returnToLobbyServer = null;
                }
            }

            // Check for admin stop signal from lobby
            Path stopSignal = Path.of("stop.signal");
            if (Files.exists(stopSignal)) {
                LOGGER.info("Stop signal received, shutting down gracefully");
                try {
                    MinecraftServer srv = ServerLifecycleHooks.getCurrentServer();
                    WarfareWorldData matchData = null;
                    for (ServerLevel sl : srv.getAllLevels()) {
                        matchData = WarfareWorldData.get(sl);
                        break;
                    }
                    if (matchData != null) {
                        if (matchData.match.isGameStarted) {
                            String leader = matchData.match.blueTickets > matchData.match.redTickets
                                    ? "BLUE"
                                    : (matchData.match.redTickets > matchData.match.blueTickets ? "RED" : "NONE");
                            MatchStatsTracker.get()
                                    .finalizeMatch(leader, matchData.match.blueTickets, matchData.match.redTickets);
                        } else {
                            MatchStatsTracker tracker = MatchStatsTracker.get();
                            if (tracker.isActive()) {
                                tracker.finalizeMatch("NONE", matchData.match.blueTickets, matchData.match.redTickets);
                            } else if (returnToLobbyTimer <= 0) {
                                returnToLobbyTimer = 10;
                                returnToLobbyServer = srv;
                            }
                        }
                    }
                    Files.delete(stopSignal);
                } catch (Exception e) {
                    LOGGER.warn("Failed to process stop signal: {}", e.getMessage());
                }
            }
        }
    }

    private static void spawnArtShell(ServerLevel level, BlockPos pos) {
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

    private static boolean isTeamReady(ServerLevel level, String teamName, WarfareWorldData data) {
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

    private static List<MapPlayerInfo> buildPlayerInfo(List<ServerPlayer> players, WarfareWorldData data) {
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

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        if (!(level instanceof ServerLevel serverLevel)) return;

        WarfareWorldData data = WarfareWorldData.get(serverLevel);
        if (data.structures.mainZones.isEmpty()) return;

        event.getAffectedBlocks().removeIf(pos -> data.structures.mainZones.stream()
                .anyMatch(zone -> zone.isInside(Vec3.atCenterOf(pos))));
    }

    private static void handleMainProtectionZones(ServerLevel level, WarfareWorldData data) {
        if (!data.structures.mainZones.isEmpty()) {
            boolean restrictMovement = !data.match.isGameStarted || data.match.invasionSetupActive;

            // Phase 1: CONFINE certain players to their own main base
            if (restrictMovement) {
                for (ServerPlayer player : level.players()) {
                    if (!player.isCreative() && !player.isSpectator()) {
                        String pTeam = player.getTeam() != null
                                ? player.getTeam().getName().toUpperCase()
                                : "NEUTRAL";
                        if (!pTeam.equals("NEUTRAL")) {
                            // During invasion setup, defenders are NOT restricted
                            if (data.match.invasionSetupActive && pTeam.equalsIgnoreCase(data.match.invasionDefender))
                                continue;

                            for (WarfareWorldData.MainProtectionZone zone : data.structures.mainZones) {
                                if (zone.team.equalsIgnoreCase(pTeam)) {
                                    if (zone.isInside(player.position())) {
                                        StagingService.rememberInside(player);
                                    } else {
                                        // техника с игроками тоже не выезжает до GO: высадить, вернуть
                                        StagingService.holdVehicle(player);
                                        String dim =
                                                level.dimension().location().toString();
                                        BlockPos spawn = pTeam.equals("BLUE")
                                                ? data.match.blueSpawns.get(dim)
                                                : data.match.redSpawns.get(dim);
                                        if (spawn != null) {
                                            player.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
                                            Component msg = data.match.invasionSetupActive
                                                    ? Component.translatable(
                                                            "pwpwarfare.main.hold_setup",
                                                            formatTime(data.match.invasionSetupTimer))
                                                    : Component.translatable(
                                                            "pwpwarfare.main.hold_waiting",
                                                            formatTime(data.match.waitingTimer));
                                            player.displayClientMessage(
                                                    msg.copy().withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                                                    true);
                                            player.playNotifySound(
                                                    SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 1.0F, 1.0F);
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }

            // Phase 2: KILL enemies who enter main bases (runs when game started)
            if (data.match.isGameStarted) {
                label207:
                for (WarfareWorldData.MainProtectionZone zone : data.structures.mainZones) {
                    List<Entity> entitiesInZone = level.getEntitiesOfClass(Entity.class, zone.area);
                    Iterator zoneEntities = entitiesInZone.iterator();

                    while (true) {
                        Entity entity;
                        while (true) {
                            if (!zoneEntities.hasNext()) {
                                continue label207;
                            }

                            entity = (Entity) zoneEntities.next();
                            if (zone.isInside(entity.position())) {
                                if (!WarfareData.of(entity).contains(WarfareKeys.SPAWN_GRACE_TICK)) {
                                    break;
                                }

                                long graceTick = WarfareData.of(entity).getLong(WarfareKeys.SPAWN_GRACE_TICK);
                                if (GameClock.worldTicks(level) - graceTick >= 100L) {
                                    break;
                                }
                            }
                        }

                        if (entity instanceof ServerPlayer player) {
                            if (!player.isCreative() && !player.isSpectator()) {
                                String pTeam = player.getTeam() != null
                                        ? player.getTeam().getName().toUpperCase()
                                        : "NEUTRAL";
                                if (!pTeam.equalsIgnoreCase(zone.team)) {
                                    int ticks = WarfareData.of(player).getInt(WarfareKeys.MAIN_ZONE_TIMER);
                                    if (++ticks >= 200) {
                                        WarfareData.of(player).remove(WarfareKeys.MAIN_ZONE_TIMER);
                                        player.kill();
                                    } else {
                                        WarfareData.of(player).putInt(WarfareKeys.MAIN_ZONE_TIMER, ticks);
                                        if (ticks % 20 == 0) {
                                            int secLeft = 10 - ticks / 20;
                                            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 30, 0));
                                            player.connection.send(new ClientboundSetSubtitleTextPacket(
                                                    Component.literal("Вы будете убиты через " + secLeft + "с!")
                                                            .withStyle(ChatFormatting.RED)));
                                            player.connection.send(new ClientboundSetTitleTextPacket(
                                                    Component.literal("ВРАЖЕСКАЯ ОСНОВНАЯ БАЗА")
                                                            .withStyle(new ChatFormatting[] {
                                                                ChatFormatting.DARK_RED, ChatFormatting.BOLD
                                                            })));
                                        }
                                    }
                                } else {
                                    WarfareData.of(player).remove(WarfareKeys.MAIN_ZONE_TIMER);
                                }
                            }
                        } else if (WarfareData.of(entity).contains(WarfareKeys.VEHICLE_TEAM)) {
                            String vTeam = WarfareData.of(entity).getString(WarfareKeys.VEHICLE_TEAM);
                            if (!vTeam.equalsIgnoreCase("NEUTRAL") && !vTeam.isEmpty()) {
                                if (vTeam.equalsIgnoreCase(zone.team)) {
                                    WarfareData.of(entity).remove(WarfareKeys.MAIN_ZONE_TIMER);
                                } else {
                                    int ticks = WarfareData.of(entity).getInt(WarfareKeys.MAIN_ZONE_TIMER);
                                    if (++ticks >= 200) {
                                        if (entity instanceof LivingEntity le) {
                                            le.kill();
                                        } else {
                                            entity.discard();
                                        }
                                    } else {
                                        WarfareData.of(entity).putInt(WarfareKeys.MAIN_ZONE_TIMER, ticks);
                                        if (ticks % 20 == 0) {
                                            int secLeft = 10 - ticks / 20;

                                            for (Entity passenger : entity.getPassengers()) {
                                                if (passenger instanceof ServerPlayer p) {
                                                    p.displayClientMessage(
                                                            Component.literal(
                                                                            "WARNING! Enemy base! Vehicle destroyed in "
                                                                                    + secLeft + "s!")
                                                                    .withStyle(new ChatFormatting[] {
                                                                        ChatFormatting.RED, ChatFormatting.BOLD
                                                                    }),
                                                            true);
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                entity.discard();
                            }
                        } else if (!(entity instanceof PartEntity)
                                && !(entity instanceof Display)
                                && !(entity instanceof ItemEntity)) {
                            entity.discard();
                        }
                    }
                }

                for (ServerPlayer player : level.players()) {
                    if (WarfareData.of(player).contains(WarfareKeys.MAIN_ZONE_TIMER)) {
                        boolean inEnemyZone = false;
                        String pTeam = player.getTeam() != null
                                ? player.getTeam().getName().toUpperCase()
                                : "NEUTRAL";

                        for (WarfareWorldData.MainProtectionZone zone : data.structures.mainZones) {
                            if (!zone.team.equalsIgnoreCase(pTeam) && zone.isInside(player.position())) {
                                inEnemyZone = true;
                                break;
                            }
                        }

                        if (inEnemyZone) {
                            WarfareData.of(player).remove(WarfareKeys.MAIN_ZONE_EXIT_TICK);
                        } else {
                            long exitTick = WarfareData.of(player).getLong(WarfareKeys.MAIN_ZONE_EXIT_TICK);
                            if (exitTick == 0) {
                                WarfareData.of(player)
                                        .putLong(WarfareKeys.MAIN_ZONE_EXIT_TICK, GameClock.worldTicks(level));
                            } else if (GameClock.worldTicks(level) - exitTick >= 200) {
                                WarfareData.of(player).remove(WarfareKeys.MAIN_ZONE_TIMER);
                                WarfareData.of(player).remove(WarfareKeys.MAIN_ZONE_EXIT_TICK);
                            }
                        }
                    }
                }

                for (WarfareWorldData.VehicleRecord record : data.vehicles.markedVehicles) {
                    Entity vehicle = level.getEntity(record.uuid);
                    if (vehicle != null && WarfareData.of(vehicle).contains(WarfareKeys.MAIN_ZONE_TIMER)) {
                        boolean inEnemyZone = false;
                        String vTeam = WarfareData.of(vehicle).getString(WarfareKeys.VEHICLE_TEAM);

                        for (WarfareWorldData.MainProtectionZone zone : data.structures.mainZones) {
                            if (!zone.team.equalsIgnoreCase(vTeam) && zone.isInside(vehicle.position())) {
                                inEnemyZone = true;
                                break;
                            }
                        }

                        if (inEnemyZone) {
                            WarfareData.of(vehicle).remove(WarfareKeys.MAIN_ZONE_EXIT_TICK);
                        } else {
                            long exitTick = WarfareData.of(vehicle).getLong(WarfareKeys.MAIN_ZONE_EXIT_TICK);
                            if (exitTick == 0) {
                                WarfareData.of(vehicle)
                                        .putLong(WarfareKeys.MAIN_ZONE_EXIT_TICK, GameClock.worldTicks(level));
                            } else if (GameClock.worldTicks(level) - exitTick >= 200) {
                                WarfareData.of(vehicle).remove(WarfareKeys.MAIN_ZONE_TIMER);
                                WarfareData.of(vehicle).remove(WarfareKeys.MAIN_ZONE_EXIT_TICK);
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) return;
        Entity entity = event.getEntity();
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
        if (rl != null
                && (rl.toString().equals("superbwarfare:tm_62") || rl.toString().equals("superbwarfare:claymore"))) {
            ServerLevel serverLevel = (ServerLevel) event.getLevel();
            WarfareWorldData data = WarfareWorldData.get(serverLevel);
            if (data.vehicles.markedVehicles.stream().anyMatch(v -> v.uuid.equals(entity.getUUID()))) return;
            String team = "NEUTRAL";
            if (WarfareData.of(entity).contains(WarfareKeys.VEHICLE_TEAM)) {
                team = WarfareData.of(entity).getString(WarfareKeys.VEHICLE_TEAM);
            } else {
                Player owner = serverLevel.getNearestPlayer(entity, 10.0);
                if (owner != null && owner.getTeam() != null) {
                    team = owner.getTeam().getName().toUpperCase();
                    WarfareData.of(entity).putString(WarfareKeys.VEHICLE_TEAM, team);
                    WarfareData.of(entity).putString(WarfareKeys.VEHICLE_TYPE, "Mine");
                }
            }
            if (!team.equals("NEUTRAL")) {
                data.vehicles.markedVehicles.add(new WarfareWorldData.VehicleRecord(
                        entity.getUUID(),
                        team,
                        "Mine",
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        entity.getYRot(),
                        null,
                        0));
                data.setDirty();
                PacketHandler.sendToAllClients(serverLevel, data);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            WarfareWorldData data = WarfareWorldData.get(level);
            SquadActions.leaveCurrentSquad(player, data);
            PacketHandler.syncSquadsAll(data.squadRegistry.squads);
            player.sendSystemMessage(Component.literal("Вы покинули отряд, потому что сменили мир.")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private static String findRequiredPointName(
            WarfareWorldData.CapturePoint currentPoint, String team, WarfareWorldData data) {
        int currentPriority = team.equals("BLUE") ? currentPoint.bluePriority : currentPoint.redPriority;
        int requiredPriority = currentPriority - 1;
        if (requiredPriority < 1) {
            return "";
        }

        for (WarfareWorldData.CapturePoint p : data.match.capturePoints) {
            int pPriority = team.equals("BLUE") ? p.bluePriority : p.redPriority;
            if (pPriority == requiredPriority) {
                return p.name;
            }
        }

        return "Unknown";
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            CosmeticService.loadForPlayer(player);
            PacketHandler.broadcastPlayerSkin(player);
            KitUtil.syncMyKit(player);
            ServerLevel level = player.serverLevel();
            WarfareWorldData data = WarfareWorldData.get(level);
            sendSyncPacket(level, data);
            PacketHandler.syncSquadsTo(player, data.squadRegistry.squads);
            PacketHandler.syncDowned(player, false);
            MarkerManager.syncToPlayer(player);
            PathManager.syncToPlayer(player);
            // решение 5: без команды — сразу в меньшую (сменить можно в меню K)
            StagingService.autoAssign(player);
            String teamName = player.getTeam() != null ? player.getTeam().getName() : "";
            if (!teamName.equalsIgnoreCase("Blue") && !teamName.equalsIgnoreCase("Red")) {
                player.setGameMode(GameType.ADVENTURE);
                player.sendSystemMessage(Component.translatable(
                                "pwpwarfare.team.choose", Component.keybind("key.pwpwarfare.open_squad_menu"))
                        .withStyle(ChatFormatting.GOLD));
            }
            StagingPackets.sendState(player, data);
            if (!data.match.isGameStarted && !data.match.waitingActive && player.hasPermissions(2)) {
                player.sendSystemMessage(Component.translatable(
                                "pwpwarfare.admin.paused_hint", Component.keybind("key.pwp_core.admin_panel"))
                        .withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!event.getEntity().level().isClientSide) {
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
            MatchStatsTracker.get().recordSpawn(newPlayer);
            String teamName = newPlayer.getTeam() != null ? newPlayer.getTeam().getName() : "";
            if (!teamName.equalsIgnoreCase("Blue") && !teamName.equalsIgnoreCase("Red")) {
                newPlayer.setGameMode(GameType.ADVENTURE);
            } else if (newPlayer.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
                newPlayer.setGameMode(GameType.SURVIVAL);
            }

            newPlayer.connection.send(new ClientboundPlayerAbilitiesPacket(newPlayer.getAbilities()));
            pendingRespawnLocations.remove(newPlayer.getUUID());
            pendingTeams.remove(newPlayer.getUUID());

            BlockPos spawnPos = pendingSpawnPositions.remove(newPlayer.getUUID());
            if (spawnPos != null) {
                newPlayer.teleportTo(
                        newPlayer.serverLevel(),
                        spawnPos.getX() + 0.5,
                        spawnPos.getY(),
                        spawnPos.getZ() + 0.5,
                        newPlayer.getYRot(),
                        0.0F);
            }

            ServerLevel level = newPlayer.serverLevel();
            WarfareWorldData data = WarfareWorldData.get(level);
            if (data.match.isGameStarted) {
                String pTeam = newPlayer.getTeam() != null
                        ? newPlayer.getTeam().getName().toUpperCase()
                        : "";
                boolean isSetupAttacker =
                        data.match.invasionSetupActive && !pTeam.equalsIgnoreCase(data.match.invasionDefender);
                if (!isSetupAttacker) {
                    ResupplyHandler.applyKitOnSpawn(newPlayer, data);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!event.getEntity().level().isClientSide) {
            Entity entity = event.getEntity();
            if (entity instanceof ServerPlayer victim) {
                Entity sourceEntity = event.getSource().getEntity();
                String weapon = event.getSource().getMsgId();

                // Прямое убийство игроком
                if (sourceEntity instanceof ServerPlayer killer) {
                    boolean isTeamkill = killer.getTeam() != null
                            && victim.getTeam() != null
                            && killer.getTeam().isAlliedTo(victim.getTeam());

                    if (!WarfareData.of(victim).getBoolean(WarfareKeys.IS_DOWNED) && isTeamkill) {
                        DownedHandler.handleTeamkill(killer);
                    }

                    if (isTeamkill) {
                        MatchStatsTracker.get().recordTeamKill(killer, victim);
                    } else {
                        double dist = killer.distanceTo(victim);
                        MatchStatsTracker.get().recordKill(killer, victim, weapon, dist);
                    }

                    // Убийство техникой
                } else if (sourceEntity != null && WarfareData.of(sourceEntity).contains(WarfareKeys.VEHICLE_TEAM)) {
                    List<Entity> passengers = sourceEntity.getPassengers();
                    if (!passengers.isEmpty() && passengers.get(0) instanceof ServerPlayer driver) {
                        MatchStatsTracker.get().recordVehicleKill(driver);
                    }
                    // Record victim death for vehicle kills
                    MatchStatsTracker.get().recordDeath(victim);
                    MatchStatsTracker.get().getOrCreate(victim).recordDeath();

                    // Смерть через downed system (forceGiveUp/bleed out)
                } else {
                    net.minecraft.world.entity.LivingEntity lastAttacker = victim.getLastHurtByMob();
                    if (lastAttacker instanceof ServerPlayer killer && killer.isAlive()) {
                        double dist = killer.distanceTo(victim);
                        MatchStatsTracker.get().recordKill(killer, victim, "knockout", dist);
                    }
                }

                MatchStatsTracker.get().recordDeath(victim);
                processEntityLoss(victim);
                WarfareData.of(victim).putBoolean(WarfareKeys.IS_DOWNED, false);
                if (WarfareData.of(victim).getBoolean(WarfareKeys.GIVING_UP)) {
                    return;
                }
            } else if (WarfareData.of(entity).contains(WarfareKeys.TICKET_PENALTY)) {
                Entity sourceEntity = event.getSource().getEntity();
                Entity vehicleDestroyer = null;

                if (sourceEntity instanceof ServerPlayer destroyer) {
                    vehicleDestroyer = destroyer;
                } else if (sourceEntity != null && WarfareData.of(sourceEntity).contains(WarfareKeys.VEHICLE_TEAM)) {
                    List<Entity> passengers = sourceEntity.getPassengers();
                    if (!passengers.isEmpty() && passengers.get(0) instanceof ServerPlayer driver) {
                        vehicleDestroyer = driver;
                    }
                } else if (sourceEntity instanceof net.minecraft.world.entity.projectile.Projectile proj) {
                    Entity owner = proj.getOwner();
                    if (owner instanceof ServerPlayer shooter) {
                        vehicleDestroyer = shooter;
                    }
                }

                if (vehicleDestroyer == null && WarfareData.of(entity).contains(WarfareKeys.LAST_DAMAGER)) {
                    String lastUuid = WarfareData.of(entity).getString(WarfareKeys.LAST_DAMAGER);
                    if (event.getEntity().level() != null) {
                        net.minecraft.server.level.ServerLevel sl = (net.minecraft.server.level.ServerLevel)
                                event.getEntity().level();
                        Entity lastPlayer = sl.getEntity(java.util.UUID.fromString(lastUuid));
                        if (lastPlayer instanceof ServerPlayer lp) {
                            vehicleDestroyer = lp;
                        }
                    }
                }

                if (vehicleDestroyer instanceof ServerPlayer vd) {
                    MatchStatsTracker.get().recordVehicleDestroyed(vd, entity);
                }

                processEntityLoss(entity);
            }
        }
    }

    private static void saveKitNbtBeforeDeath(ServerPlayer player) {
        WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
        String kitName = WarfareData.of(player).getString(WarfareKeys.CURRENT_KIT);
        String team = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
        if (!team.isEmpty() && !kitName.isEmpty() && !kitName.equals("Unassigned")) {
            WarfareWorldData.KitInfo kit = team.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
            if (kit != null) {
                Map<Integer, CompoundTag> savedTags = new HashMap<>();

                for (int i = 0; i < 41; i++) {
                    if (i < kit.saveNbtFlags.length && kit.saveNbtFlags[i]) {
                        ItemStack item = player.getInventory().getItem(i);
                        if (!item.isEmpty() && item.hasTag()) {
                            savedTags.put(i, item.getTag().copy());
                        }
                    }
                }

                if (!savedTags.isEmpty()) {
                    PERSISTENT_NBT_STORAGE.put(player.getUUID(), savedTags);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(Clone event) {
        ServerPlayer oldPlayer = (ServerPlayer) event.getOriginal();
        ServerPlayer newPlayer = (ServerPlayer) event.getEntity();
        // capability старого игрока после клона инвалидирована — оживляем на время копирования.
        // Смерть: переживает только PlayerCloneRules.SURVIVE_DEATH; возврат из Энда — всё.
        oldPlayer.reviveCaps();
        try {
            PlayerWarfareData.copyOnClone(oldPlayer, newPlayer, event.isWasDeath());
        } finally {
            oldPlayer.invalidateCaps();
        }
        // Нок не переживает пересоздание игрока ни в каком случае (как и раньше)
        WarfareData.of(newPlayer).putBoolean(WarfareKeys.IS_DOWNED, false);
        WarfareData.of(newPlayer).remove(WarfareKeys.GIVING_UP);
        WarfareData.of(newPlayer).remove(WarfareKeys.DOWNED_YAW);
        WarfareData.of(newPlayer).remove(WarfareKeys.DOWNED_PITCH);
        WarfareData.of(newPlayer).putLong(WarfareKeys.LAST_REVIVE_TIME, 0L);
        PacketHandler.syncDowned(newPlayer, false);

        if (event.isWasDeath()) {
            String teamName = oldPlayer.getTeam() != null ? oldPlayer.getTeam().getName() : null;
            if (teamName != null) {
                Scoreboard scoreboard = newPlayer.getScoreboard();
                PlayerTeam pTeam = scoreboard.getPlayerTeam(teamName);
                if (pTeam != null) {
                    scoreboard.addPlayerToTeam(newPlayer.getScoreboardName(), pTeam);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityRemove(EntityLeaveLevelEvent event) {
        if (!event.getLevel().isClientSide) {
            Entity entity = event.getEntity();
            if (!(entity instanceof LivingEntity)) {
                RemovalReason reason = entity.getRemovalReason();
                if (reason != null && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
                    // Check if this was a tagged vehicle with a known last damager
                    if (WarfareData.of(entity).contains(WarfareKeys.TICKET_PENALTY)
                            && WarfareData.of(entity).contains(WarfareKeys.LAST_DAMAGER)) {
                        String lastUuid = WarfareData.of(entity).getString(WarfareKeys.LAST_DAMAGER);
                        net.minecraft.server.level.ServerLevel sl =
                                (net.minecraft.server.level.ServerLevel) event.getLevel();
                        Entity lastPlayer = sl.getEntity(java.util.UUID.fromString(lastUuid));
                        if (lastPlayer instanceof ServerPlayer destroyer) {
                            MatchStatsTracker.get().recordVehicleDestroyed(destroyer, entity);
                        }
                    }
                    processEntityLoss(entity);
                }
            }
        }
    }

    private static void processEntityLoss(Entity entity) {
        if (entity.level() != null && entity.level().getServer() != null) {
            ServerLevel level = (ServerLevel) entity.level();
            WarfareWorldData data = WarfareWorldData.get(level);
            boolean markerRemoved = data.vehicles.markedVehicles.removeIf(v -> v.uuid.equals(entity.getUUID()));
            data.removeApprovedDrivers(entity.getUUID());
            if (data.match.isGameStarted && data.match.blueTickets > 0 && data.match.redTickets > 0) {
                boolean ticketsChanged = false;
                if (entity instanceof ServerPlayer player && player.getTeam() != null) {
                    String teamName = player.getTeam().getName();
                    boolean isInSetupAttacker =
                            data.match.invasionSetupActive && !teamName.equalsIgnoreCase(data.match.invasionDefender);
                    if (!isInSetupAttacker) {
                        int cost = data.match.deathTicketCost;
                        if (teamName.equalsIgnoreCase("Blue")) {
                            data.match.blueTickets = Math.max(0, data.match.blueTickets - cost);
                            ticketsChanged = true;
                        } else if (teamName.equalsIgnoreCase("Red")) {
                            data.match.redTickets = Math.max(0, data.match.redTickets - cost);
                            ticketsChanged = true;
                        }
                    }
                }

                if (WarfareData.of(entity).contains(WarfareKeys.TICKET_PENALTY)) {
                    String vTeam = WarfareData.of(entity).getString(WarfareKeys.VEHICLE_TEAM);
                    boolean isInSetupAttackerVehicle =
                            data.match.invasionSetupActive && !vTeam.equalsIgnoreCase(data.match.invasionDefender);
                    if (!isInSetupAttackerVehicle) {
                        int penalty = WarfareData.of(entity).getInt(WarfareKeys.TICKET_PENALTY);
                        String vType = WarfareData.of(entity).getString(WarfareKeys.VEHICLE_TYPE);
                        WarfareData.of(entity).remove(WarfareKeys.TICKET_PENALTY);
                        if (penalty > 0) {
                            if (vTeam.equalsIgnoreCase("BLUE")) {
                                data.match.blueTickets = Math.max(0, data.match.blueTickets - penalty);
                                ticketsChanged = true;
                                broadcastMessage(
                                        level, "BLUE lost " + vType + " (-" + penalty + ")", ChatFormatting.BLUE);
                            } else if (vTeam.equalsIgnoreCase("RED")) {
                                data.match.redTickets = Math.max(0, data.match.redTickets - penalty);
                                ticketsChanged = true;
                                broadcastMessage(
                                        level, "RED lost " + vType + " (-" + penalty + ")", ChatFormatting.RED);
                            }
                        }
                    }
                }

                if (ticketsChanged || markerRemoved) {
                    if (ticketsChanged) {
                        checkGameOver(level, data);
                    }

                    data.setDirty();
                    sendSyncPacket(level, data);
                }
            } else {
                if (markerRemoved) {
                    data.setDirty();
                    sendSyncPacket(level, data);
                }
            }
        }
    }

    private static boolean hasCorrectKit(String vType, String kit) {
        if (vType.equalsIgnoreCase("HELICOPTER")
                || vType.toUpperCase().contains("CAS")
                || vType.toUpperCase().contains("SUPPLY HELICOPTER")) {
            return kit.equals("Pilot") || kit.equals("Pilot Officer");
        }
        if (vType.equalsIgnoreCase("TANK") || vType.equalsIgnoreCase("APC") || vType.equalsIgnoreCase("Mobile ZU")) {
            return kit.equals("Mechanic") || kit.equals("Mechanic Officer");
        }
        return true;
    }

    private static float getVehicleHealth(Entity entity) {
        try {
            Object result = entity.getClass().getMethod("getHealth").invoke(entity);
            if (result instanceof Float health) {
                return health;
            }
        } catch (Exception ignored) {
            com.pwp.api.util.LogOnce.warn(
                    LOGGER, "GameLogicEvents:1697", "GameLogicEvents.getVehicleHealth: исключение", ignored);
        }
        return Float.MAX_VALUE;
    }

    private static void processWreckLoss(Entity entity, WarfareWorldData data, ServerLevel level) {
        if (WarfareData.of(entity).contains(WarfareKeys.TICKET_PENALTY)) {
            String vTeam = WarfareData.of(entity).getString(WarfareKeys.VEHICLE_TEAM);
            // During invasion setup: skip ticket deduction for attackers' vehicles
            if (data.match.invasionSetupActive && !vTeam.equalsIgnoreCase(data.match.invasionDefender)) {
                boolean markerRemoved = data.vehicles.markedVehicles.removeIf(v -> v.uuid.equals(entity.getUUID()));
                data.removeApprovedDrivers(entity.getUUID());
                WarfareData.of(entity).remove(WarfareKeys.TICKET_PENALTY);
                if (markerRemoved) {
                    data.setDirty();
                    sendSyncPacket(level, data);
                }
                return;
            }

            int penalty = WarfareData.of(entity).getInt(WarfareKeys.TICKET_PENALTY);
            String vType = WarfareData.of(entity).getString(WarfareKeys.VEHICLE_TYPE);
            WarfareData.of(entity).remove(WarfareKeys.TICKET_PENALTY);
            boolean markerRemoved = data.vehicles.markedVehicles.removeIf(v -> v.uuid.equals(entity.getUUID()));
            data.removeApprovedDrivers(entity.getUUID());
            if (penalty > 0 && data.match.blueTickets > 0 && data.match.redTickets > 0) {
                boolean ticketsChanged = false;
                if (vTeam.equalsIgnoreCase("BLUE")) {
                    data.match.blueTickets = Math.max(0, data.match.blueTickets - penalty);
                    ticketsChanged = true;
                    broadcastMessage(level, "BLUE lost " + vType + " (-" + penalty + ")", ChatFormatting.BLUE);
                } else if (vTeam.equalsIgnoreCase("RED")) {
                    data.match.redTickets = Math.max(0, data.match.redTickets - penalty);
                    ticketsChanged = true;
                    broadcastMessage(level, "RED lost " + vType + " (-" + penalty + ")", ChatFormatting.RED);
                }
                if (ticketsChanged || markerRemoved) {
                    if (ticketsChanged) {
                        checkGameOver(level, data);
                    }
                    data.setDirty();
                    sendSyncPacket(level, data);
                }
            } else if (markerRemoved) {
                data.setDirty();
                sendSyncPacket(level, data);
            }
        }
    }

    private static void handleTeamInfluence(
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

    private static void applyTickets(WarfareWorldData data, CaptureLogic.Tickets delta) {
        data.match.blueTickets += delta.blue();
        data.match.redTickets += delta.red();
    }

    private static void validateAndSync(ServerLevel level, boolean forceSend, boolean blueBleed, boolean redBleed) {
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

    private static void validateAndSync(ServerLevel level, boolean forceSend) {
        validateAndSync(level, forceSend, false, false);
    }

    private static boolean validateRallies(ServerLevel level, List<BlockPos> rallies) {
        return rallies.removeIf(
                pos -> level.isLoaded(pos) ? !(level.getBlockState(pos).getBlock() instanceof RallyPointBlock) : false);
    }

    private static boolean isEnemyNearby(
            ServerLevel level, BlockPos pos, String allyTeamName, int radius, int minCount) {
        return getEnemyCount(level, pos, allyTeamName, radius) >= minCount;
    }

    private static int getEnemyCount(ServerLevel level, BlockPos pos, String allyTeamName, int radius) {
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

    private static void executeVictory(ServerLevel level, WarfareWorldData data, boolean blueWon) {
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

        returnToLobbyTimer = 300;
        returnToLobbyServer = level.getServer();
    }

    private static String formatFactionName(String faction) {
        return faction.replace("_", " ").toUpperCase();
    }

    private static String formatTime(int totalSecs) {
        int m = totalSecs / 60;
        int s = totalSecs % 60;
        if (m >= 1) return "\u00a7e" + m + " \u043C\u0438\u043D " + (s > 0 ? "\u00a77" + s + " \u0441" : "");
        return "\u00a7e" + s + " \u0441";
    }

    private static void sendTitleToLevel(ServerLevel level, String text, ChatFormatting color) {
        Component title = Component.literal(text).withStyle(color).withStyle(ChatFormatting.BOLD);

        for (ServerPlayer player : level.players()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    private static void sendTitleToAll(MinecraftServer server, String text, ChatFormatting color) {
        Component title = Component.literal(text).withStyle(color).withStyle(ChatFormatting.BOLD);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(new ClientboundSetTitlesAnimationPacket(0, 20, 10));
            player.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    private static void broadcastTranslatable(ServerLevel level, String key, ChatFormatting color) {
        Component c = Component.translatable(key).withStyle(color);
        for (ServerPlayer p : level.players()) p.sendSystemMessage(c);
    }

    /** Есть ли хоть один игрок в командах (пустой сервер не «готов» к досрочному старту). */
    private static boolean anyTeamPlayers(ServerLevel level) {
        for (ServerPlayer p : level.players()) {
            if (!StagingService.teamOf(p).isEmpty()) return true;
        }
        return false;
    }

    private static void broadcastMessage(ServerLevel level, String text, ChatFormatting color) {
        level.getServer()
                .getPlayerList()
                .broadcastSystemMessage(Component.literal(text).withStyle(color), false);
    }

    private static void sendSyncPacket(ServerLevel level, WarfareWorldData data) {
        sendSyncPacket(level, data, false, false);
    }

    private static void sendSyncPacket(ServerLevel level, WarfareWorldData data, boolean blueBleed, boolean redBleed) {
        String dimKey = level.dimension().location().toString();
        boolean bBlocked = lastBlueBlockedMap.getOrDefault(dimKey, false);
        boolean rBlocked = lastRedBlockedMap.getOrDefault(dimKey, false);
        sendSyncPacket(level, data, blueBleed, redBleed, bBlocked, rBlocked);
    }

    private static void sendSyncPacket(
            ServerLevel level,
            WarfareWorldData data,
            boolean blueBleed,
            boolean redBleed,
            boolean bBlocked,
            boolean rBlocked) {
        // единая сборка и рассылка по командам — PacketHandler (тут была копия createSyncPacket)
        PacketHandler.sendSync(level.players(), data, blueBleed, redBleed, bBlocked, rBlocked);
    }

    private static boolean canCapture(WarfareWorldData.CapturePoint target, String team, WarfareWorldData data) {
        // Invasion locked points cannot be recaptured by the former owner
        if ("invasion".equals(data.match.gameMode)) {
            if (target.invLocked && !team.equals(target.owner)) {
                return false;
            }
            // Neutral points cannot be captured by the defender
            if (target.owner.equals("NEUTRAL") && team.equals(data.match.invasionDefender)) {
                return false;
            }
        }

        int currentPriority = team.equals("BLUE") ? target.bluePriority : target.redPriority;
        if (currentPriority <= 1) {
            return true;
        }

        int requiredPriority = currentPriority - 1;

        for (WarfareWorldData.CapturePoint p : data.match.capturePoints) {
            int pPriority = team.equals("BLUE") ? p.bluePriority : p.redPriority;
            if (pPriority == requiredPriority && p.owner.equals(team)) {
                return true;
            }
        }

        return false;
    }

    public static void leaveCurrentSquad(ServerPlayer player, WarfareWorldData data) {
        String pName = player.getScoreboardName();

        for (WarfareWorldData.Squad s : data.squadRegistry.squads) {
            if (s.members.contains(pName)) {
                s.members.remove(pName);
                if (s.leader.equals(pName)) {
                    SquadActions.removeRadio(player);
                    if (!s.members.isEmpty()) {
                        s.leader = s.members.get(0);
                        ServerPlayer newLeader = player.server.getPlayerList().getPlayerByName(s.leader);
                        if (newLeader != null) {
                            SquadActions.updatePlayerTags(newLeader, s.id, true);
                            if ((Boolean) WarfareConfig.AUTO_GIVE_SL_RADIO.get()) {
                                SquadActions.giveRadio(newLeader);
                            }
                        }
                    }
                }
            }
        }

        data.squadRegistry.squads.removeIf(sx -> sx.members.isEmpty());
        data.setDirty();
        PacketHandler.sendToAllClients(player.serverLevel(), data);
        WarfareData.of(player).putString(WarfareKeys.CURRENT_KIT, "Unassigned");
        WarfareData.of(player).putString(WarfareKeys.PENDING_KIT, "");
        player.getInventory().clearContent();
        ResupplyHandler.clearCurios(player);
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        SquadActions.removePlayerTags(player);
        player.displayClientMessage(Component.literal("§eВы вышли из отряда. Кит и брони очищены."), true);
        data.setDirty();
        PacketHandler.sendToAllClients(player.serverLevel(), data);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        if (!event.getEntity().level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            ServerLevel level = player.serverLevel();
            WarfareWorldData data = WarfareWorldData.get(level);
            if (data.match.isGameStarted) {
                String team =
                        player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
                boolean isSetupAttacker =
                        data.match.invasionSetupActive && !team.equalsIgnoreCase(data.match.invasionDefender);
                if (!isSetupAttacker) {
                    return;
                }
            }

            SquadActions.leaveCurrentSquad(player, data);
            WarfareData.of(player).putString(WarfareKeys.CURRENT_KIT, "Unassigned");
            WarfareData.of(player).remove(WarfareKeys.PENDING_KIT);
            player.getInventory().clearContent();
            ResupplyHandler.clearCurios(player);
            player.inventoryMenu.broadcastChanges();
            data.setDirty();
            PacketHandler.syncSquadsAll(data.squadRegistry.squads);
        }
    }

    private static void playSirenForTeam(ServerLevel level, String teamName) {
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

    @SubscribeEvent
    public static void onProjectileImpact(net.minecraftforge.event.entity.ProjectileImpactEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit) {
            Entity hitEntity = hit.getEntity();
            if (WarfareData.of(hitEntity).contains(WarfareKeys.TICKET_PENALTY)) {
                Entity projectile = event.getEntity();
                if (projectile instanceof net.minecraft.world.entity.projectile.Projectile proj) {
                    Entity owner = proj.getOwner();
                    if (owner instanceof ServerPlayer shooter) {
                        WarfareData.of(hitEntity).putString(WarfareKeys.LAST_DAMAGER, shooter.getStringUUID());
                    }
                }
            }
        }
    }

    public static void enforceMortarShellLimit(PlayerTickEvent event) {
        if (event.phase == Phase.END && !event.player.level().isClientSide) {
            if (event.player.tickCount % 10 == 0) {
                Player player = event.player;
                if (!player.isCreative() && !player.isSpectator()) {
                    Item mortarItem = (Item)
                            ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "mortar_shell"));
                    if (mortarItem == null || mortarItem == Items.AIR) {
                        mortarItem = Items.ARROW;
                    }

                    int totalCount = 0;
                    Inventory inv = player.getInventory();

                    for (int i = 0; i < inv.getContainerSize(); i++) {
                        ItemStack stack = inv.getItem(i);
                        if (stack.getItem() == mortarItem) {
                            totalCount += stack.getCount();
                        }
                    }

                    if (totalCount > 8) {
                        int toRemove = totalCount - 8;
                        int removedSoFar = 0;

                        for (int i = 0; i < inv.getContainerSize() && removedSoFar < toRemove; i++) {
                            ItemStack stack = inv.getItem(i);
                            if (stack.getItem() == mortarItem) {
                                int shrinkAmount = Math.min(stack.getCount(), toRemove - removedSoFar);
                                stack.shrink(shrinkAmount);
                                removedSoFar += shrinkAmount;
                                ItemStack dropped = new ItemStack(mortarItem, shrinkAmount);
                                player.drop(dropped, false, true);
                            }
                        }

                        player.displayClientMessage(
                                Component.literal("Можно нести максимум 8 миномётных снарядов!")
                                        .withStyle(ChatFormatting.RED),
                                true);
                    }
                }
            }
        }
    }

    private static void checkCmdVoteStatus(
            ServerLevel level, MinecraftServer server, WarfareWorldData data, String team) {
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
                            level, team, team + " COMMANDER ASSIGNED: " + candidateName, ChatFormatting.GREEN);
                } else {
                    broadcastTeamMessage(
                            level, team, team + " CMD Application rejected or timed out.", ChatFormatting.RED);
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

    private static void broadcastTeamMessage(ServerLevel level, String team, String text, ChatFormatting color) {
        Component message = Component.literal(text).withStyle(color);

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.getTeam() != null && player.getTeam().getName().equalsIgnoreCase(team)) {
                player.sendSystemMessage(message);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getPlacedBlock().getBlock() instanceof com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlock))
            return;
        net.minecraft.world.level.block.entity.BlockEntity be =
                ((net.minecraft.world.level.LevelAccessor) event.getLevel()).getBlockEntity(event.getPos());
        if (be instanceof VehicleSpawnerBlockEntity spawner) {
            net.minecraft.server.level.ServerLevel sLevel = (net.minecraft.server.level.ServerLevel) event.getLevel();
            WarfareWorldData data = WarfareWorldData.get(sLevel);
            data.vehicles.spawnerInfos.removeIf(s -> s.pos != null && s.pos.equals(event.getPos()));
            WarfareWorldData.SpawnerInfo info = spawner.getSpawnerInfo();
            if (info != null) data.vehicles.spawnerInfos.add(info);
        }
    }

    @SubscribeEvent
    public static void onBlockBroken(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getState().getBlock() instanceof com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlock)) return;
        net.minecraft.server.level.ServerLevel sLevel = (net.minecraft.server.level.ServerLevel) event.getLevel();
        WarfareWorldData data = WarfareWorldData.get(sLevel);
        data.vehicles.spawnerInfos.removeIf(s -> s.pos != null && s.pos.equals(event.getPos()));
    }
}
