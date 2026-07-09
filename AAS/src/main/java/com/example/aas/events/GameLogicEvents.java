/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket
 *  net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
 *  net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
 *  net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.MinecraftServer
 *  net.minecraft.server.ServerScoreboard
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.effect.MobEffectInstance
 *  net.minecraft.world.effect.MobEffects
 *  net.minecraft.world.entity.Display
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$RemovalReason
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.OwnableEntity
 *  net.minecraft.world.entity.item.ItemEntity
 *  net.minecraft.world.entity.player.Inventory
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.entity.projectile.Projectile
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.Items
 *  net.minecraft.world.level.GameType
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.scores.PlayerTeam
 *  net.minecraft.world.scores.Scoreboard
 *  net.minecraftforge.entity.PartEntity
 *  net.minecraftforge.event.TickEvent$LevelTickEvent
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.event.TickEvent$PlayerTickEvent
 *  net.minecraftforge.event.TickEvent$ServerTickEvent
 *  net.minecraftforge.event.entity.EntityJoinLevelEvent
 *  net.minecraftforge.event.entity.EntityLeaveLevelEvent
 *  net.minecraftforge.event.entity.EntityTravelToDimensionEvent
 *  net.minecraftforge.event.entity.item.ItemTossEvent
 *  net.minecraftforge.event.entity.living.LivingDeathEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$Clone
 *  net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedInEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$PlayerLoggedOutEvent
 *  net.minecraftforge.event.entity.player.PlayerEvent$PlayerRespawnEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.network.PacketDistributor
 *  net.minecraftforge.registries.ForgeRegistries
 *  net.minecraftforge.server.ServerLifecycleHooks
 */
package com.example.aas.events;

import com.example.aas.block.GameStartTriggerBlock;
import com.example.aas.block.HubBlockEntity;
import com.example.aas.block.ModBlocks;
import com.example.aas.block.RallyPointBlock;
import com.example.aas.block.RallyPointBlockEntity;
import com.example.aas.config.AASConfig;
import com.example.aas.events.DownedHandler;
import com.example.aas.item.ModItems;
import com.example.aas.network.MapPlayerInfo;
import com.example.aas.network.PacketCaptureNotification;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketOpenVictoryScreen;
import com.example.aas.network.PacketSquadAction;
import com.example.aas.network.PacketSyncDownedState;
import com.example.aas.network.PacketSyncGameData;
import com.example.aas.network.PacketSyncMapPlayers;
import com.example.aas.network.PacketSyncPoint;
import com.example.aas.network.PacketSyncSquads;
import com.example.aas.network.ResupplyHandler;
import com.example.aas.sound.ModSounds;
import com.example.aas.world.AASWorldData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.entity.PartEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid="aas", bus=Mod.EventBusSubscriber.Bus.FORGE)
public class GameLogicEvents {
    static final public Map<UUID, String> pendingRespawnLocations = new HashMap<UUID, String>();
    static final public Map<UUID, String> pendingTeams = new HashMap<UUID, String>();
    static final public Map<UUID, Map<Integer, CompoundTag>> PERSISTENT_NBT_STORAGE = new HashMap<UUID, Map<Integer, CompoundTag>>();
    static private final Map<String, Boolean> lastBlueBlockedMap = new HashMap<String, Boolean>();
    static private final Map<String, Boolean> lastRedBlockedMap = new HashMap<String, Boolean>();

    public static void startGameCountdown(ServerLevel level) {
        AASWorldData data = AASWorldData.get(level);
        data.countdownTicks = 100;
        data.countdownActive = true;
        data.setDirty();
    }

    public static void cancelCountdown(ServerLevel level) {
        AASWorldData data = AASWorldData.get(level);
        data.countdownActive = false;
        data.countdownTicks = 0;
        data.setDirty();
    }

    public static void cancelCountdown() {
    }

    @SubscribeEvent
    public static void onItemToss(ItemTossEvent event) {
        if (event.getPlayer() == null || event.getPlayer().level().isClientSide) {
            return;
        }
        ServerPlayer player = (ServerPlayer)event.getPlayer();
        if (player.isCreative()) {
            return;
        }
        AASWorldData data = AASWorldData.get(player.serverLevel());
        if (data.isGameStarted) {
            boolean preventAll = false;
            try {
                preventAll = (Boolean)AASConfig.PREVENT_ALL_ITEM_DROPS.get();
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (preventAll) {
                event.setCanceled(true);
                player.getInventory().add(event.getEntity().getItem());
                player.displayClientMessage((Component)Component.literal((String)"Item dropping is DISABLED during the game!").withStyle(ChatFormatting.RED), true);
            } else if (GameLogicEvents.isHeavyItem(event.getEntity().getItem().getItem())) {
                event.setCanceled(true);
                player.getInventory().add(event.getEntity().getItem());
                player.displayClientMessage((Component)Component.literal((String)"Cannot drop heavy ammo during combat!").withStyle(ChatFormatting.RED), true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTickEffects(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (event.player.tickCount % 10 == 0) {
            ServerPlayer player = (ServerPlayer)event.player;
            AASWorldData data = AASWorldData.get(player.serverLevel());
            if (data.isGameStarted && !player.isCreative()) {
                boolean hasHeavyItem = false;
                for (ItemStack stack : player.getInventory().items) {
                    if (stack.isEmpty() || !GameLogicEvents.isHeavyItem(stack.getItem())) continue;
                    hasHeavyItem = true;
                    break;
                }
                if (!hasHeavyItem) {
                    for (ItemStack stack : player.getInventory().offhand) {
                        if (stack.isEmpty() || !GameLogicEvents.isHeavyItem(stack.getItem())) continue;
                        hasHeavyItem = true;
                        break;
                    }
                }
                if (hasHeavyItem) {
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 2, false, false, true));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerTickDriveCheck(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (!((Boolean)AASConfig.REQUIRE_SPECIALIST_TO_DRIVE.get()).booleanValue()) {
            return;
        }
        ServerPlayer player = (ServerPlayer)event.player;
        Entity vehicle = player.getVehicle();
        if (vehicle == null) {
            player.getPersistentData().remove("AAS_DriveKickTimer");
            player.getPersistentData().remove("AAS_BoardingGrace");
            player.getPersistentData().remove("AAS_LastVehicleId");
            return;
        }
        int lastVehicleId = player.getPersistentData().getInt("AAS_LastVehicleId");
        if (lastVehicleId != vehicle.getId()) {
            player.getPersistentData().putInt("AAS_LastVehicleId", vehicle.getId());
            player.getPersistentData().putInt("AAS_BoardingGrace", 0);
            player.getPersistentData().remove("AAS_DriveKickTimer");
            return;
        }
        int boardingGrace = player.getPersistentData().getInt("AAS_BoardingGrace");
        if (boardingGrace < 80) {
            player.getPersistentData().putInt("AAS_BoardingGrace", boardingGrace + 1);
            return;
        }
        int seatIndex = vehicle.getPassengers().indexOf(player);
        if (seatIndex > 0) {
            player.getPersistentData().remove("AAS_DriveKickTimer");
            return;
        }
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        String vType = vehicle.getPersistentData().getString("AAS_VehicleType");
        String pKit = player.getPersistentData().getString("AAS_CurrentKit");
        boolean isAuthorized = true;
        String requiredSpecialist = "";
        if (vType.equalsIgnoreCase("HELICOPTER") || vType.contains("CAS") || vType.contains("Supply Helicopter")) {
            if (!pKit.equals("Pilot") && !pKit.equals("Pilot Officer")) {
                isAuthorized = false;
                requiredSpecialist = "PILOT";
            }
        } else if ((vType.equalsIgnoreCase("TANK") || vType.equalsIgnoreCase("APC") || vType.equalsIgnoreCase("Mobile ZU")) && !pKit.equals("Mechanic") && !pKit.equals("Mechanic Officer")) {
            isAuthorized = false;
            requiredSpecialist = "MECHANIC";
        }
        if (!isAuthorized) {
            int timer = player.getPersistentData().getInt("AAS_DriveKickTimer");
            if (++timer >= 100) {
                player.stopRiding();
                player.getPersistentData().remove("AAS_DriveKickTimer");
                player.getPersistentData().remove("AAS_BoardingGrace");
                player.getPersistentData().remove("AAS_LastVehicleId");
                player.displayClientMessage((Component)Component.literal((String)"EJECTED: You are not a qualified driver!").withStyle(ChatFormatting.RED), true);
            } else {
                player.getPersistentData().putInt("AAS_DriveKickTimer", timer);
                if (timer % 20 == 0) {
                    player.displayClientMessage((Component)Component.literal((String)("\u00a7c\u00a7lUNAUTHORIZED DRIVER! \u00a7eRequires " + requiredSpecialist + " kit. Ejecting in " + (5 - timer / 20) + "s...")).withStyle(ChatFormatting.YELLOW), true);
                    player.playNotifySound((SoundEvent)SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.PLAYERS, 1.0f, 0.5f);
                }
            }
        } else {
            player.getPersistentData().remove("AAS_DriveKickTimer");
        }
    }

    @SubscribeEvent
    public static void onPlayerItemLogic(TickEvent.PlayerTickEvent event) {
        boolean hasMonitor;
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (event.player.level().getGameTime() % 20L != 0L) {
            return;
        }
        Player player = event.player;
        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offHandStack = player.getOffhandItem();
        Item monitorItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "monitor"));
        Item walkieItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("walkietalkie", "netherite_walkietalkie"));
        if (monitorItem == null || walkieItem == null) {
            return;
        }
        boolean bl = hasMonitor = mainHandStack.getItem() == monitorItem;
        if (hasMonitor) {
            int walkieSlot;
            if (offHandStack.getItem() != walkieItem && !offHandStack.isEmpty()) {
                GameLogicEvents.moveItemToInventory(player, offHandStack);
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            }
            if (player.getOffhandItem().isEmpty() && (walkieSlot = GameLogicEvents.findItemInInventory(player.getInventory(), walkieItem)) != -1) {
                ItemStack walkieStack = player.getInventory().getItem(walkieSlot);
                player.setItemInHand(InteractionHand.OFF_HAND, walkieStack);
                player.getInventory().setItem(walkieSlot, ItemStack.EMPTY);
            }
        } else if (!offHandStack.isEmpty() && offHandStack.getItem() == walkieItem) {
            GameLogicEvents.moveItemToInventory(player, offHandStack);
            player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        }
    }

    private static boolean isHeavyItem(Item item) {
        if (item == ModItems.AGS_AMMO.get()) {
            return true;
        }
        if (item == ModItems.M2_AMMO.get()) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey((Object)item);
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
        for (int i = 0; i < inventory.items.size(); ++i) {
            if (((ItemStack)inventory.items.get(i)).getItem() != item) continue;
            return i;
        }
        return -1;
    }

    private static void moveItemToInventory(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        int globalTick = server.getTickCount();
        if (globalTick % 2 == 0) {
            HashMap<ServerLevel, List<MapPlayerInfo>> dimensionDataCache = new HashMap<ServerLevel, List<MapPlayerInfo>>();
            for (ServerLevel level : server.getAllLevels()) {
                AASWorldData data = AASWorldData.get(level);
                dimensionDataCache.put(level, GameLogicEvents.buildPlayerInfo(level.players(), data));
            }
            for (ServerPlayer p2 : server.getPlayerList().getPlayers()) {
                ServerLevel playerLevel = p2.serverLevel();
                List playersInMyDim = (List)dimensionDataCache.get(playerLevel);
                if (playersInMyDim == null || playersInMyDim.isEmpty()) continue;
                if (p2.isSpectator() || p2.isCreative()) {
                    PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p2), (Object)new PacketSyncMapPlayers(playersInMyDim));
                    continue;
                }
                if (p2.getTeam() == null) continue;
                String myTeamName = p2.getTeam().getName();
                List<MapPlayerInfo> teamOnly = playersInMyDim.stream().filter(info -> info.team.equalsIgnoreCase(myTeamName)).toList();
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p2), (Object)new PacketSyncMapPlayers(teamOnly));
            }
        }
        for (ServerLevel level : server.getAllLevels()) {
            boolean gameEnded;
            boolean bl;
            long time;
            Entity vEntity;
            AASWorldData data = AASWorldData.get(level);
            GameLogicEvents.handleMainProtectionZones(level, data);
            boolean blueIsBleeding = false;
            boolean redIsBleeding = false;
            if (data.blueCmdVoteActive) {
                --data.blueCmdVoteTimer;
                GameLogicEvents.checkCmdVoteStatus(level, server, data, "BLUE");
            }
            if (data.redCmdVoteActive) {
                --data.redCmdVoteTimer;
                GameLogicEvents.checkCmdVoteStatus(level, server, data, "RED");
            }
            if (data.voteActive && !data.isGameStarted) {
                if (globalTick % 20 == 0 && data.voteTimer > 0) {
                    --data.voteTimer;
                    data.setDirty();
                    boolean bReady = GameLogicEvents.isTeamReady(level, "Blue", data);
                    boolean rReady = GameLogicEvents.isTeamReady(level, "Red", data);
                    if (bReady != data.blueReady || rReady != data.redReady || data.voteTimer % 5 == 0 || data.voteTimer < 5) {
                        data.blueReady = bReady;
                        data.redReady = rReady;
                        PacketHandler.sendToAllClients(level, data);
                    }
                }
                if (data.blueReady && data.redReady || data.voteTimer <= 0) {
                    data.voteActive = false;
                    data.setDirty();
                    GameLogicEvents.startGameCountdown(level);
                    PacketHandler.sendToAllClients(level, data);
                }
            }
            if (globalTick % 5 == 0) {
                boolean needsSync = false;
                for (AASWorldData.VehicleRecord vehicleRecord : data.markedVehicles) {
                    vEntity = level.getEntity(vehicleRecord.uuid);
                    if (vEntity == null || !vEntity.isAlive()) continue;
                    float currentYaw = vEntity.getYRot();
                    if (vehicleRecord.x == vEntity.getX() && vehicleRecord.yaw == currentYaw) continue;
                    vehicleRecord.x = vEntity.getX();
                    vehicleRecord.y = vEntity.getY();
                    vehicleRecord.z = vEntity.getZ();
                    vehicleRecord.yaw = currentYaw;
                    needsSync = true;
                }
                if (needsSync) {
                    data.setDirty();
                    PacketHandler.sendToAllClients(level, data);
                }
            }
            if (globalTick % 40 == 0) {
                boolean changed = false;
                Iterator<AASWorldData.VehicleRecord> it = data.markedVehicles.iterator();
                while (it.hasNext()) {
                    AASWorldData.VehicleRecord vehicleRecord = it.next();
                    vEntity = level.getEntity(vehicleRecord.uuid);
                    if (vEntity == null || vEntity.isAlive()) continue;
                    it.remove();
                    changed = true;
                }
                if (changed) {
                    data.setDirty();
                    PacketHandler.sendToAllClients(level, data);
                }
            }
            if (globalTick % 200 == 0 && data.activeMarkers.removeIf(arg_0 -> GameLogicEvents.lambda$onServerTick$3(time = level.getGameTime(), arg_0))) {
                data.setDirty();
                PacketHandler.sendToAllClients(level, data);
            }
            if (globalTick % 20 == 0) {
                int rallyRadius = (Integer)AASConfig.RALLY_BLOCK_RADIUS.get();
                int rallyEnemiesRequired = (Integer)AASConfig.RALLY_BLOCK_ENEMY_COUNT.get();
                for (AASWorldData.Squad squad : data.squads) {
                    int enemies;
                    boolean currentlyBlocked;
                    if (squad.rallyPos == null || squad.isRallyBlocked == (currentlyBlocked = (enemies = GameLogicEvents.getEnemyCount(level, squad.rallyPos, squad.team, rallyRadius)) >= rallyEnemiesRequired)) continue;
                    squad.isRallyBlocked = currentlyBlocked;
                    data.setDirty();
                    PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((ServerLevel)level).dimension()), (Object)new PacketSyncSquads(data.squads));
                }
            }
            if (globalTick % 400 == 0 && ((Boolean)AASConfig.REQUIRE_OFFICER_FOR_SL.get()).booleanValue()) {
                for (AASWorldData.Squad squad : new ArrayList<AASWorldData.Squad>(data.squads)) {
                    ServerPlayer serverPlayer = server.getPlayerList().getPlayerByName(squad.leader);
                    if (serverPlayer == null) continue;
                    if (serverPlayer.isCreative()) {
                        squad.slNoOfficerSince = -1L;
                        continue;
                    }
                    String current = serverPlayer.getPersistentData().getString("AAS_CurrentKit");
                    String pending = serverPlayer.getPersistentData().getString("AAS_PendingKit");
                    Object kitName = !pending.isEmpty() ? pending : current;
                    boolean hasCommandKit = false;
                    if (!((String)kitName).isEmpty() && !((String)kitName).equals("Unassigned")) {
                        AASWorldData.KitInfo kitInfo;
                        String team = squad.team.toUpperCase();
                        AASWorldData.KitInfo kitInfo2 = kitInfo = team.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                        if (kitInfo != null && kitInfo.isLeaderOnly) {
                            hasCommandKit = true;
                        }
                    }
                    if (!hasCommandKit) {
                        long remaining;
                        if (squad.slNoOfficerSince == -1L) {
                            squad.slNoOfficerSince = level.getGameTime();
                        }
                        if ((remaining = 2400L - (level.getGameTime() - squad.slNoOfficerSince)) <= 0L) {
                            GameLogicEvents.broadcastMessage(level, "Squad " + squad.name + " disbanded: Leader is not using a Leader kit!", ChatFormatting.RED);
                            for (String memberName : new ArrayList<String>(squad.members)) {
                                ServerPlayer m = server.getPlayerList().getPlayerByName(memberName);
                                if (m == null) continue;
                                m.getPersistentData().putString("AAS_CurrentKit", "Unassigned");
                                m.getPersistentData().remove("AAS_PendingKit");
                                m.getPersistentData().remove("AAS_SquadID");
                                m.getPersistentData().remove("AAS_IsSquadLeader");
                                PacketSquadAction.removeRadio(m);
                                m.getInventory().clearContent();
                                ResupplyHandler.clearCurios(m);
                                m.inventoryMenu.broadcastChanges();
                                m.containerMenu.broadcastChanges();
                                m.displayClientMessage((Component)Component.literal((String)"\u00a7cYour squad was disbanded (No Officer kit)!"), true);
                            }
                            data.squads.remove(squad);
                            data.setDirty();
                            PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((ServerLevel)level).dimension()), (Object)new PacketSyncSquads(data.squads));
                            continue;
                        }
                        serverPlayer.displayClientMessage((Component)Component.literal((String)("\u00a76WARNING: \u00a7eSelect a \u00a7nLEADER ONLY\u00a7e kit or squad disbands in \u00a7c" + remaining / 20L + "s!")), true);
                        continue;
                    }
                    squad.slNoOfficerSince = -1L;
                }
            }
            long now = level.getGameTime();
            boolean bl2 = false;
            for (AASWorldData.Squad squad : data.squads) {
                if (squad.rhombusMarkers.removeIf(rm -> now >= rm.expiryTick)) {
                    bl = true;
                }
                if (squad.rallyPos == null || squad.rallyExpiryTick == -1L || now < squad.rallyExpiryTick) continue;
                if (level.isLoaded(squad.rallyPos)) {
                    BlockEntity be = level.getBlockEntity(squad.rallyPos);
                    if (be instanceof RallyPointBlockEntity) {
                        RallyPointBlockEntity rbe = (RallyPointBlockEntity)be;
                        rbe.isDecay = true;
                    }
                    level.removeBlock(squad.rallyPos, false);
                    continue;
                }
                data.blueRallies.remove(squad.rallyPos);
                data.redRallies.remove(squad.rallyPos);
                squad.rallyPos = null;
                squad.rallyExpiryTick = -1L;
                bl = true;
            }
            if (bl) {
                data.setDirty();
                PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncSquads(data.squads));
            }
            if (globalTick % 200 == 0) {
                long currentTick = level.getGameTime();
                boolean anyMarkerRemoved = false;
                for (AASWorldData.Squad squad : data.squads) {
                    if (squad.marker == null || currentTick < squad.marker.expiryTick) continue;
                    squad.marker = null;
                    anyMarkerRemoved = true;
                }
                if (anyMarkerRemoved) {
                    data.setDirty();
                    PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((ServerLevel)level).dimension()), (Object)new PacketSyncSquads(data.squads));
                }
            }
            if (data.countdownActive) {
                if (data.countdownTicks > 0) {
                    if (data.countdownTicks % 20 == 0) {
                        int seconds = data.countdownTicks / 20;
                        GameLogicEvents.sendTitleToLevel(level, String.valueOf(seconds), ChatFormatting.YELLOW);
                        level.playSound(null, new BlockPos(0, 100, 0), (SoundEvent)SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.MASTER, 1.0f, 1.0f);
                    }
                    --data.countdownTicks;
                    data.setDirty();
                } else {
                    data.countdownActive = false;
                    GameLogicEvents.sendTitleToLevel(level, "GO!", ChatFormatting.GREEN);
                    data.isGameStarted = true;
                    data.setDirty();
                    GameLogicEvents.sendSyncPacket(level, data);
                    for (ServerPlayer p3 : level.players()) {
                        String pending = p3.getPersistentData().getString("AAS_PendingKit");
                        String current = p3.getPersistentData().getString("AAS_CurrentKit");
                        String kitToApply = !pending.isEmpty() ? pending : current;
                        if (kitToApply == null || kitToApply.isEmpty() || kitToApply.equals("Unassigned")) continue;
                        ResupplyHandler.tryApplyPendingKit(p3, data);
                    }
                    for (BlockPos p4 : data.triggerBlocks) {
                        BlockState st;
                        if (!level.isLoaded(p4) || !(st = level.getBlockState(p4)).is((Block)ModBlocks.GAME_START_TRIGGER.get())) continue;
                        level.setBlock(p4, (BlockState)st.setValue((Property)GameStartTriggerBlock.POWERED, (Comparable)Boolean.valueOf(true)), 3);
                        level.scheduleTick(p4, (Block)ModBlocks.GAME_START_TRIGGER.get(), 20);
                    }
                    GameLogicEvents.sendSyncPacket(level, data);
                }
            }
            if (data.isGameStarted && ((Boolean)AASConfig.LOW_TICKETS_SIREN.get()).booleanValue()) {
                if (data.blueTickets <= 50 && data.blueTickets > 0 && !data.playedBlueSiren) {
                    GameLogicEvents.playSirenForTeam(level, "Blue");
                    data.playedBlueSiren = true;
                    data.setDirty();
                }
                if (data.redTickets <= 50 && data.redTickets > 0 && !data.playedRedSiren) {
                    GameLogicEvents.playSirenForTeam(level, "Red");
                    data.playedRedSiren = true;
                    data.setDirty();
                }
            }
            boolean bl3 = gameEnded = data.blueTickets <= 0 || data.redTickets <= 0;
            if (data.isGameStarted && !gameEnded && !data.capturePoints.isEmpty()) {
                int totalPoints = data.capturePoints.size();
                long blueOwned = data.capturePoints.stream().filter(p -> p.owner.equalsIgnoreCase("BLUE")).count();
                long redOwned = data.capturePoints.stream().filter(p -> p.owner.equalsIgnoreCase("RED")).count();
                blueIsBleeding = blueOwned == 0L && redOwned >= (long)(totalPoints - 1) && totalPoints > 0;
                boolean bl4 = redIsBleeding = redOwned == 0L && blueOwned >= (long)(totalPoints - 1) && totalPoints > 0;
                if (globalTick % 40 == 0) {
                    boolean changed = false;
                    if (blueIsBleeding) {
                        --data.blueTickets;
                        changed = true;
                    }
                    if (redIsBleeding) {
                        --data.redTickets;
                        changed = true;
                    }
                    if (changed) {
                        GameLogicEvents.checkGameOver(level, data);
                        data.setDirty();
                    }
                }
            }
            if (globalTick % 20 == 0) {
                GameLogicEvents.validateAndSync(level, false, blueIsBleeding, redIsBleeding);
            }
            if (data.countdownActive && data.countdownTicks == 1) {
                long cdTicks = (long)((Integer)AASConfig.ART_STRIKE_COOLDOWN_MINUTES.get()).intValue() * 60L * 20L;
                data.blueArtStrikeCD = level.getGameTime() + cdTicks;
                data.redArtStrikeCD = level.getGameTime() + cdTicks;
            }
            if (data.blueArtRequest != null) {
                --data.blueArtRequest.timer;
                if (data.blueArtRequest.timer <= 0) {
                    data.blueArtRequest = null;
                    data.setDirty();
                    PacketHandler.sendToAllClients(level, data);
                }
            }
            if (data.redArtRequest == null) continue;
            --data.redArtRequest.timer;
            if (data.redArtRequest.timer > 0) continue;
            data.redArtRequest = null;
            data.setDirty();
            PacketHandler.sendToAllClients(level, data);
        }
    }

    private static void spawnArtShell(ServerLevel level, BlockPos pos) {
        Entity shell;
        int rad = (Integer)AASConfig.ART_STRIKE_RADIUS.get();
        double x = (double)pos.getX() + level.random.nextDouble() * (double)rad * 2.0 - (double)rad;
        double z = (double)pos.getZ() + level.random.nextDouble() * (double)rad * 2.0 - (double)rad;
        double y = level.getMaxBuildHeight() - 2;
        EntityType shellType = (EntityType)ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("superbwarfare", "mortar_shell"));
        if (shellType != null && (shell = shellType.create((Level)level)) != null) {
            shell.setPos(x, y, z);
            shell.setDeltaMovement(0.0, 0.0, 0.0);
            level.addFreshEntity(shell);
        }
    }

    private static boolean isTeamReady(ServerLevel level, String teamName, AASWorldData data) {
        List<ServerPlayer> teamPlayers = level.players().stream().filter(p -> p.getTeam() != null && p.getTeam().getName().equalsIgnoreCase(teamName)).toList();
        if (teamPlayers.isEmpty()) {
            return true;
        }
        long yesVotes = teamPlayers.stream().filter(p -> data.votes.getOrDefault(p.getUUID(), false)).count();
        float percent = (float)yesVotes / (float)teamPlayers.size() * 100.0f;
        boolean isReady = percent >= (float)((Integer)AASConfig.VOTE_REQUIRED_PERCENTAGE.get()).intValue();
        return isReady;
    }

    private static List<MapPlayerInfo> buildPlayerInfo(List<ServerPlayer> players, AASWorldData data) {
        ArrayList<MapPlayerInfo> infoList = new ArrayList<MapPlayerInfo>();
        for (ServerPlayer p : players) {
            String pName = p.getScoreboardName();
            String pTeam = p.getTeam() != null ? p.getTeam().getName().toUpperCase() : "NEUTRAL";
            int squadId = p.getPersistentData().getInt("AAS_SquadID");
            if (squadId == 0) {
                squadId = -1;
            }
            boolean isLeader = p.getPersistentData().getBoolean("AAS_IsSquadLeader");
            for (AASWorldData.Squad s : data.squads) {
                if (!s.members.contains(pName)) continue;
                squadId = s.id;
                if (!s.leader.equals(pName)) break;
                isLeader = true;
                break;
            }
            boolean inVehicle = p.getVehicle() != null;
            int vId = -1;
            int seatIdx = -1;
            if (inVehicle) {
                Entity vehicle = p.getVehicle();
                vId = vehicle.getId();
                seatIdx = vehicle.getPassengers().indexOf(p);
            }
            boolean downed = p.getPersistentData().getBoolean("AAS_IsDowned");
            long shout = p.getPersistentData().getLong("AAS_LastMedicShoutTimeMS");
            infoList.add(new MapPlayerInfo(pName, p.getUUID(), p.getX(), p.getZ(), p.getYRot(), squadId, isLeader, downed, shout, inVehicle, vId, seatIdx, pTeam));
        }
        return infoList;
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        Entity entity = event.getEntity();
        ResourceLocation rl = ForgeRegistries.ENTITY_TYPES.getKey((Object)entity.getType());
        if (rl != null && (rl.toString().equals("superbwarfare:tm_62") || rl.toString().equals("superbwarfare:claymore"))) {
            ServerLevel serverLevel = (ServerLevel)event.getLevel();
            AASWorldData data = AASWorldData.get(serverLevel);
            if (data.markedVehicles.stream().anyMatch(v -> v.uuid.equals(entity.getUUID()))) {
                return;
            }
            String team = "NEUTRAL";
            if (entity.getPersistentData().contains("AAS_VehicleTeam")) {
                team = entity.getPersistentData().getString("AAS_VehicleTeam");
            } else {
                OwnableEntity ownable;
                Player p;
                Projectile proj;
                Entity entity2;
                Player owner = null;
                if (entity instanceof Projectile && (entity2 = (proj = (Projectile)entity).getOwner()) instanceof Player) {
                    owner = p = (Player)entity2;
                }
                if (owner == null && entity instanceof OwnableEntity && (entity2 = (ownable = (OwnableEntity)entity).getOwner()) instanceof Player) {
                    owner = p = (Player)entity2;
                }
                if (owner == null) {
                    owner = serverLevel.getNearestPlayer(entity, 10.0);
                }
                if (owner != null && owner.getTeam() != null) {
                    team = owner.getTeam().getName().toUpperCase();
                    entity.getPersistentData().putString("AAS_VehicleTeam", team);
                    entity.getPersistentData().putString("AAS_VehicleType", "Mine");
                }
            }
            if (!team.equals("NEUTRAL")) {
                data.markedVehicles.add(new AASWorldData.VehicleRecord(entity.getUUID(), team, "Mine", entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), null));
                data.setDirty();
                PacketHandler.sendToAllClients(serverLevel, data);
            }
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.LevelTickEvent event) {
        if (event.level.isClientSide || event.phase != TickEvent.Phase.END) {
            return;
        }
        ServerLevel level = (ServerLevel)event.level;
        AASWorldData data = AASWorldData.get(level);
        HashSet<UUID> playersInPreciseZones = new HashSet<UUID>();
        for (AASWorldData.CapturePoint point : data.capturePoints) {
            boolean isPointActive;
            boolean isTimeLocked;
            List playersInBox = level.getEntitiesOfClass(ServerPlayer.class, point.area);
            int blueOnPointLiving = 0;
            int redOnPointLiving = 0;
            for (ServerPlayer p : playersInBox) {
                if (!p.isAlive() || p.isSpectator() || p.getPersistentData().getBoolean("AAS_IsDowned") || !point.isInside(p.position()) || p.getTeam() == null) continue;
                if (p.getTeam().getName().equalsIgnoreCase("Blue")) {
                    ++blueOnPointLiving;
                    continue;
                }
                if (!p.getTeam().getName().equalsIgnoreCase("Red")) continue;
                ++redOnPointLiving;
            }
            String dominantTeam = "NONE";
            int alliesOnPoint = 0;
            boolean isContested = false;
            boolean bl = isTimeLocked = level.getGameTime() < point.lockedUntilTick;
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
            float multiplier = 1.0f;
            if (alliesOnPoint > 1) {
                multiplier += (float)(alliesOnPoint - 1) * 0.5f;
            }
            if (multiplier > 4.0f) {
                multiplier = 4.0f;
            }
            int currentRate = 0;
            String teamToSync = "NONE";
            if (!(dominantTeam.equals("NONE") || isContested || isTimeLocked)) {
                boolean isFullyCaptured;
                boolean isOwner = point.owner.equals(dominantTeam);
                boolean bl2 = isFullyCaptured = isOwner && point.progress >= 1.0f;
                if (!isFullyCaptured && GameLogicEvents.canCapture(point, dominantTeam, data)) {
                    teamToSync = dominantTeam;
                    boolean isNeutralizing = !point.owner.equals("NEUTRAL") && !point.owner.equals(dominantTeam) || point.owner.equals("NEUTRAL") && !point.capturingTeam.equals("NONE") && !point.capturingTeam.equals(dominantTeam);
                    currentRate = Math.round(multiplier);
                    if (isNeutralizing) {
                        currentRate = -currentRate;
                    }
                }
            }
            for (ServerPlayer p : playersInBox) {
                if (!point.isInside(p.position())) continue;
                playersInPreciseZones.add(p.getUUID());
                boolean lockedUI = false;
                String nextObjectiveForPlayer = "";
                if (p.getTeam() != null) {
                    String teamKey;
                    String string = teamKey = p.getTeam().getName().equalsIgnoreCase("Blue") ? "BLUE" : "RED";
                    if (isTimeLocked) {
                        lockedUI = true;
                        long totalSeconds = (point.lockedUntilTick - level.getGameTime()) / 20L;
                        nextObjectiveForPlayer = String.format("LOCKED: %d\u043c %d\u0441", totalSeconds / 60L, totalSeconds % 60L);
                    } else if (!GameLogicEvents.canCapture(point, teamKey, data)) {
                        lockedUI = true;
                        nextObjectiveForPlayer = GameLogicEvents.findRequiredPointName(point, teamKey, data);
                    }
                }
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), (Object)new PacketSyncPoint(true, point.name, point.owner, point.progress, lockedUI, nextObjectiveForPlayer, isContested, teamToSync, currentRate));
            }
            boolean isBeingActivelyCaptured = false;
            if (!dominantTeam.equals("NONE") && !isContested && !isTimeLocked && GameLogicEvents.canCapture(point, dominantTeam, data)) {
                isBeingActivelyCaptured = true;
                float baseSpeed = 1.0f / (float)(point.captureTimeMinutes * 60 * 20);
                String oldOwner = point.owner;
                GameLogicEvents.handleTeamInfluence(point, dominantTeam, multiplier, baseSpeed, data, level);
                if (!point.owner.equals(oldOwner) && !point.owner.equals("NEUTRAL") && point.lockDurationMinutes > 0) {
                    point.lockedUntilTick = level.getGameTime() + (long)point.lockDurationMinutes * 60L * 20L;
                    data.setDirty();
                }
            }
            if (!(isBeingActivelyCaptured || isContested || isTimeLocked)) {
                float baseSpeed = 1.0f / (float)(point.captureTimeMinutes * 60 * 20);
                if (point.owner.equals("NEUTRAL") && point.progress > 0.0f) {
                    point.progress -= baseSpeed / 2.0f;
                    if (point.progress <= 0.0f) {
                        point.progress = 0.0f;
                        point.capturingTeam = "NONE";
                    }
                } else if (!point.owner.equals("NEUTRAL") && point.progress < 1.0f) {
                    point.progress += baseSpeed / 2.0f;
                    if (point.progress >= 1.0f) {
                        point.progress = 1.0f;
                        point.capturingTeam = "NONE";
                    }
                }
            }
            if (level.getGameTime() % 10L != 0L || !(isPointActive = point.progress > 0.0f && point.progress < 1.0f || !point.capturingTeam.equals("NONE"))) continue;
            for (ServerPlayer player : level.players()) {
                if (playersInPreciseZones.contains(player.getUUID())) continue;
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), (Object)new PacketSyncPoint(false, point.name, point.owner, point.progress, false, "", isContested, point.capturingTeam, 0));
            }
        }
        for (ServerPlayer player : level.players()) {
            if (playersInPreciseZones.contains(player.getUUID())) continue;
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), (Object)new PacketSyncPoint(false, "", "", 0.0f, false, "", false, "NONE", 0));
        }
    }

    private static void handleMainProtectionZones(ServerLevel level, AASWorldData data) {
        if (data.mainZones.isEmpty()) {
            return;
        }
        if (!data.isGameStarted) {
            block0: for (ServerPlayer player : level.players()) {
                String pTeam;
                if (player.isCreative() || player.isSpectator() || (pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL").equals("NEUTRAL")) continue;
                for (AASWorldData.MainProtectionZone zone : data.mainZones) {
                    if (!zone.team.equalsIgnoreCase(pTeam)) continue;
                    if (zone.isInside(player.position())) continue block0;
                    String dim = level.dimension().location().toString();
                    BlockPos spawn = pTeam.equals("BLUE") ? data.blueSpawns.get(dim) : data.redSpawns.get(dim);
                    if (spawn == null) continue block0;
                    player.teleportTo((double)spawn.getX() + 0.5, (double)spawn.getY(), (double)spawn.getZ() + 0.5);
                    player.displayClientMessage((Component)Component.literal((String)"The match hasn't started yet! Wait in the Main Base.").withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}), true);
                    player.playNotifySound(SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 1.0f, 1.0f);
                    continue block0;
                }
            }
        } else {
            for (AASWorldData.MainProtectionZone zone : data.mainZones) {
                List entitiesInZone = level.getEntitiesOfClass(Entity.class, zone.area);
                for (Object entity : entitiesInZone) {
                    int secLeft;
                    int ticks;
                    if (!zone.isInside(entity.position())) continue;
                    if (entity.getPersistentData().contains("AAS_SpawnGraceTick")) {
                        long graceTick = entity.getPersistentData().getLong("AAS_SpawnGraceTick");
                        if (level.getGameTime() - graceTick < 100L) continue;
                    }
                    if (entity instanceof ServerPlayer) {
                        String pTeam;
                        ServerPlayer player = (ServerPlayer)entity;
                        if (player.isCreative() || player.isSpectator()) continue;
                        String string = pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                        if (!pTeam.equalsIgnoreCase(zone.team)) {
                            ticks = player.getPersistentData().getInt("AAS_MainZoneTimer");
                            if (++ticks >= 200) {
                                player.getPersistentData().remove("AAS_MainZoneTimer");
                                player.kill();
                                continue;
                            }
                            player.getPersistentData().putInt("AAS_MainZoneTimer", ticks);
                            if (ticks % 20 != 0) continue;
                            secLeft = 10 - ticks / 20;
                            player.connection.send((Packet)new ClientboundSetTitlesAnimationPacket(0, 30, 0));
                            player.connection.send((Packet)new ClientboundSetSubtitleTextPacket((Component)Component.literal((String)("You will be killed in " + secLeft + "s!")).withStyle(ChatFormatting.RED)));
                            player.connection.send((Packet)new ClientboundSetTitleTextPacket((Component)Component.literal((String)"ENEMY MAIN BASE").withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD})));
                            continue;
                        }
                        player.getPersistentData().remove("AAS_MainZoneTimer");
                        continue;
                    }
                    if (entity.getPersistentData().contains("AAS_VehicleTeam")) {
                        String vTeam = entity.getPersistentData().getString("AAS_VehicleTeam");
                        if (vTeam.equalsIgnoreCase("NEUTRAL") || vTeam.isEmpty()) {
                            entity.discard();
                            continue;
                        }
                        if (vTeam.equalsIgnoreCase(zone.team)) {
                            entity.getPersistentData().remove("AAS_MainZoneTimer");
                            continue;
                        }
                        ticks = entity.getPersistentData().getInt("AAS_MainZoneTimer");
                        if (++ticks >= 200) {
                            if (entity instanceof LivingEntity) {
                                LivingEntity le = (LivingEntity)entity;
                                le.kill();
                                continue;
                            }
                            entity.discard();
                            continue;
                        }
                        entity.getPersistentData().putInt("AAS_MainZoneTimer", ticks);
                        if (ticks % 20 != 0) continue;
                        secLeft = 10 - ticks / 20;
                        for (Entity passenger : entity.getPassengers()) {
                            if (!(passenger instanceof ServerPlayer)) continue;
                            ServerPlayer p = (ServerPlayer)passenger;
                            p.displayClientMessage((Component)Component.literal((String)("WARNING! Enemy base! Vehicle destroyed in " + secLeft + "s!")).withStyle(new ChatFormatting[]{ChatFormatting.RED, ChatFormatting.BOLD}), true);
                        }
                        continue;
                    }
                    if (entity instanceof PartEntity || entity instanceof Display || entity instanceof ItemEntity) continue;
                    entity.discard();
                }
            }
            for (ServerPlayer player : level.players()) {
                if (!player.getPersistentData().contains("AAS_MainZoneTimer")) continue;
                boolean inEnemyZone = false;
                String pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
                for (AASWorldData.MainProtectionZone zone : data.mainZones) {
                    if (zone.team.equalsIgnoreCase(pTeam) || !zone.isInside(player.position())) continue;
                    inEnemyZone = true;
                    break;
                }
                if (inEnemyZone) continue;
                player.getPersistentData().remove("AAS_MainZoneTimer");
            }
            for (AASWorldData.VehicleRecord record : data.markedVehicles) {
                Entity vehicle = level.getEntity(record.uuid);
                if (vehicle == null || !vehicle.getPersistentData().contains("AAS_MainZoneTimer")) continue;
                boolean inEnemyZone = false;
                String vTeam = vehicle.getPersistentData().getString("AAS_VehicleTeam");
                for (AASWorldData.MainProtectionZone zone : data.mainZones) {
                    if (zone.team.equalsIgnoreCase(vTeam) || !zone.isInside(vehicle.position())) continue;
                    inEnemyZone = true;
                    break;
                }
                if (inEnemyZone) continue;
                vehicle.getPersistentData().remove("AAS_MainZoneTimer");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerChangeDimension(EntityTravelToDimensionEvent event) {
        Entity entity = event.getEntity();
        if (entity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)entity;
            ServerLevel level = player.serverLevel();
            AASWorldData data = AASWorldData.get(level);
            PacketSquadAction.leaveCurrentSquad(player, data);
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncSquads(data.squads));
            player.sendSystemMessage((Component)Component.literal((String)"You were removed from the squad because you changed worlds.").withStyle(ChatFormatting.YELLOW));
        }
    }

    private static String findRequiredPointName(AASWorldData.CapturePoint currentPoint, String team, AASWorldData data) {
        int currentPriority = team.equals("BLUE") ? currentPoint.bluePriority : currentPoint.redPriority;
        int requiredPriority = currentPriority - 1;
        if (requiredPriority < 1) {
            return "";
        }
        for (AASWorldData.CapturePoint p : data.capturePoints) {
            int pPriority = team.equals("BLUE") ? p.bluePriority : p.redPriority;
            if (pPriority != requiredPriority) continue;
            return p.name;
        }
        return "Unknown";
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!event.getEntity().level().isClientSide) {
            ServerPlayer player = (ServerPlayer)event.getEntity();
            ServerLevel level = player.serverLevel();
            AASWorldData data = AASWorldData.get(level);
            GameLogicEvents.sendSyncPacket(level, data);
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), (Object)new PacketSyncSquads(data.squads));
            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncDownedState(player.getId(), false));
            if (!data.isGameStarted && player.hasPermissions(2)) {
                player.sendSystemMessage((Component)Component.literal((String)"AAS Game is paused. /aas gamestart true to start.").withStyle(ChatFormatting.YELLOW));
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!event.getEntity().level().isClientSide) {
            ServerPlayer newPlayer = (ServerPlayer)event.getEntity();
            if (newPlayer.gameMode.getGameModeForPlayer() != GameType.CREATIVE) {
                newPlayer.setGameMode(GameType.SURVIVAL);
            }
            newPlayer.connection.send((Packet)new ClientboundPlayerAbilitiesPacket(newPlayer.getAbilities()));
            pendingRespawnLocations.remove(newPlayer.getUUID());
            pendingTeams.remove(newPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (event.getEntity().level().isClientSide) {
            return;
        }
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity instanceof ServerPlayer) {
            ServerPlayer player = (ServerPlayer)livingEntity;
            GameLogicEvents.processEntityLoss((Entity)player);
            player.getPersistentData().putBoolean("AAS_IsDowned", false);
            if (player.getPersistentData().getBoolean("AAS_GivingUp")) {
                return;
            }
        }
    }

    private static void saveKitNbtBeforeDeath(ServerPlayer player) {
        AASWorldData.KitInfo kit;
        String team;
        AASWorldData data = AASWorldData.get(player.serverLevel());
        String kitName = player.getPersistentData().getString("AAS_CurrentKit");
        String string = team = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
        if (team.isEmpty() || kitName.isEmpty() || kitName.equals("Unassigned")) {
            return;
        }
        AASWorldData.KitInfo kitInfo = kit = team.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
        if (kit == null) {
            return;
        }
        HashMap<Integer, CompoundTag> savedTags = new HashMap<Integer, CompoundTag>();
        for (int i = 0; i < 41; ++i) {
            ItemStack item;
            if (i >= kit.saveNbtFlags.length || !kit.saveNbtFlags[i] || (item = player.getInventory().getItem(i)).isEmpty() || !item.hasTag()) continue;
            savedTags.put(i, item.getTag().copy());
        }
        if (!savedTags.isEmpty()) {
            PERSISTENT_NBT_STORAGE.put(player.getUUID(), savedTags);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        ServerPlayer oldPlayer = (ServerPlayer)event.getOriginal();
        ServerPlayer newPlayer = (ServerPlayer)event.getEntity();
        newPlayer.getPersistentData().putBoolean("AAS_IsDowned", false);
        newPlayer.getPersistentData().remove("AAS_GivingUp");
        newPlayer.getPersistentData().remove("AAS_DownedYaw");
        newPlayer.getPersistentData().remove("AAS_DownedPitch");
        newPlayer.getPersistentData().putLong("AAS_LastReviveTime", 0L);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncDownedState(newPlayer.getId(), false));
        CompoundTag oldData = oldPlayer.getPersistentData();
        CompoundTag newData = newPlayer.getPersistentData();
        if (oldData.contains("AAS_SquadID")) {
            newData.putInt("AAS_SquadID", oldData.getInt("AAS_SquadID"));
        }
        if (oldData.contains("AAS_IsSquadLeader")) {
            newData.putBoolean("AAS_IsSquadLeader", oldData.getBoolean("AAS_IsSquadLeader"));
        }
        if (oldData.contains("AAS_CurrentKit")) {
            newData.putString("AAS_CurrentKit", oldData.getString("AAS_CurrentKit"));
        }
        if (oldData.contains("AAS_PendingKit")) {
            newData.putString("AAS_PendingKit", oldData.getString("AAS_PendingKit"));
        }
        if (oldData.contains("AAS_LastFobResupply")) {
            newData.putLong("AAS_LastFobResupply", oldData.getLong("AAS_LastFobResupply"));
        }
        if (oldData.contains("AAS_LastMainResupply")) {
            newData.putLong("AAS_LastMainResupply", oldData.getLong("AAS_LastMainResupply"));
        }
        if (event.isWasDeath()) {
            Scoreboard scoreboard;
            PlayerTeam pTeam;
            String teamName;
            String string = teamName = oldPlayer.getTeam() != null ? oldPlayer.getTeam().getName() : null;
            if (teamName != null && (pTeam = (scoreboard = newPlayer.getScoreboard()).getPlayerTeam(teamName)) != null) {
                scoreboard.addPlayerToTeam(newPlayer.getScoreboardName(), pTeam);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityRemove(EntityLeaveLevelEvent event) {
        if (event.getLevel().isClientSide) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity) {
            return;
        }
        Entity.RemovalReason reason = entity.getRemovalReason();
        if (reason != null && (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED)) {
            GameLogicEvents.processEntityLoss(entity);
        }
    }

    private static void processEntityLoss(Entity entity) {
        ServerPlayer player;
        if (entity.level() == null || entity.level().getServer() == null) {
            return;
        }
        ServerLevel level = (ServerLevel)entity.level();
        AASWorldData data = AASWorldData.get(level);
        boolean markerRemoved = data.markedVehicles.removeIf(v -> v.uuid.equals(entity.getUUID()));
        if (data.blueTickets <= 0 || data.redTickets <= 0) {
            if (markerRemoved) {
                data.setDirty();
                GameLogicEvents.sendSyncPacket(level, data);
            }
            return;
        }
        boolean ticketsChanged = false;
        if (entity instanceof ServerPlayer && (player = (ServerPlayer)entity).getTeam() != null) {
            String teamName = player.getTeam().getName();
            int cost = data.deathTicketCost;
            if (teamName.equalsIgnoreCase("Blue")) {
                data.blueTickets = Math.max(0, data.blueTickets - cost);
                ticketsChanged = true;
            } else if (teamName.equalsIgnoreCase("Red")) {
                data.redTickets = Math.max(0, data.redTickets - cost);
                ticketsChanged = true;
            }
        }
        if (entity.getPersistentData().contains("AAS_TicketPenalty")) {
            int penalty = entity.getPersistentData().getInt("AAS_TicketPenalty");
            String vTeam = entity.getPersistentData().getString("AAS_VehicleTeam");
            String vType = entity.getPersistentData().getString("AAS_VehicleType");
            entity.getPersistentData().remove("AAS_TicketPenalty");
            if (penalty > 0) {
                if (vTeam.equalsIgnoreCase("BLUE")) {
                    data.blueTickets = Math.max(0, data.blueTickets - penalty);
                    ticketsChanged = true;
                    GameLogicEvents.broadcastMessage(level, "BLUE lost " + vType + " (-" + penalty + ")", ChatFormatting.BLUE);
                } else if (vTeam.equalsIgnoreCase("RED")) {
                    data.redTickets = Math.max(0, data.redTickets - penalty);
                    ticketsChanged = true;
                    GameLogicEvents.broadcastMessage(level, "RED lost " + vType + " (-" + penalty + ")", ChatFormatting.RED);
                }
            }
        }
        if (ticketsChanged || markerRemoved) {
            if (ticketsChanged) {
                GameLogicEvents.checkGameOver(level, data);
            }
            data.setDirty();
            GameLogicEvents.sendSyncPacket(level, data);
        }
    }

    private static void handleTeamInfluence(AASWorldData.CapturePoint point, String attackingTeam, float multiplier, float baseSpeed, AASWorldData data, ServerLevel level) {
        float speedBoosted = baseSpeed * multiplier;
        if (point.owner.equals("NEUTRAL")) {
            if (point.capturingTeam.equals("NONE") || point.capturingTeam.equals(attackingTeam)) {
                point.capturingTeam = attackingTeam;
                point.progress += speedBoosted;
                if (point.progress >= 1.0f) {
                    point.progress = 1.0f;
                    point.owner = attackingTeam;
                    PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketCaptureNotification(point.name, attackingTeam, false));
                    if (point.captureDeduction > 0) {
                        if (attackingTeam.equals("BLUE")) {
                            data.redTickets -= point.captureDeduction;
                        } else {
                            data.blueTickets -= point.captureDeduction;
                        }
                        GameLogicEvents.checkGameOver(level, data);
                    }
                    data.setDirty();
                    GameLogicEvents.sendSyncPacket(level, data);
                }
            } else {
                point.progress -= speedBoosted;
                if (point.progress <= 0.0f) {
                    point.progress = 0.0f;
                    point.capturingTeam = "NONE";
                }
            }
        } else if (point.owner.equals(attackingTeam)) {
            if (point.progress < 1.0f) {
                point.progress += speedBoosted;
                if (point.progress > 1.0f) {
                    point.progress = 1.0f;
                }
            }
        } else {
            point.progress -= speedBoosted;
            if (point.progress <= 0.0f) {
                String oldOwnerName = point.owner;
                if (point.owner.equals("BLUE")) {
                    data.blueTickets -= point.ticketPenalty;
                } else {
                    data.redTickets -= point.ticketPenalty;
                }
                GameLogicEvents.checkGameOver(level, data);
                point.owner = "NEUTRAL";
                point.progress = 0.0f;
                point.capturingTeam = "NONE";
                PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketCaptureNotification(point.name, attackingTeam, true));
                data.setDirty();
                GameLogicEvents.sendSyncPacket(level, data);
            }
        }
    }

    private static void validateAndSync(ServerLevel level, boolean forceSend, boolean blueBleed, boolean redBleed) {
        AASWorldData data = AASWorldData.get(level);
        boolean changed = false;
        String dimKey = level.dimension().location().toString();
        changed |= GameLogicEvents.validateRallies(level, data.blueRallies);
        changed |= GameLogicEvents.validateRallies(level, data.redRallies);
        int hubRadius = (Integer)AASConfig.HUB_BLOCK_RADIUS.get();
        int hubEnemiesRequired = (Integer)AASConfig.HUB_BLOCK_ENEMY_COUNT.get();
        for (AASWorldData.HubInfo hub : data.hubs) {
            HubBlockEntity hubBe;
            BlockEntity be;
            boolean nowBlocked;
            if (!hub.constructed || hub.dimension == null || !hub.dimension.equals(dimKey) || !level.isLoaded(hub.pos)) continue;
            boolean bl = nowBlocked = GameLogicEvents.getEnemyCount(level, hub.pos, hub.team, hubRadius) >= hubEnemiesRequired;
            if (hub.isBlocked != nowBlocked) {
                hub.isBlocked = nowBlocked;
                changed = true;
            }
            if (!((be = level.getBlockEntity(hub.pos)) instanceof HubBlockEntity) || hub.materials == (hubBe = (HubBlockEntity)be).getMaterials()) continue;
            hub.materials = hubBe.getMaterials();
            changed = true;
        }
        int rallyRadius = (Integer)AASConfig.RALLY_BLOCK_RADIUS.get();
        int rallyEnemiesRequired = (Integer)AASConfig.RALLY_BLOCK_ENEMY_COUNT.get();
        boolean anySquadStatusChanged = false;
        boolean teamBlueBlocked = false;
        boolean teamRedBlocked = false;
        for (AASWorldData.Squad squad : data.squads) {
            boolean currentlyBlocked;
            if (squad.rallyPos == null || !squad.rallyDimension.equals(dimKey)) continue;
            int enemies = GameLogicEvents.getEnemyCount(level, squad.rallyPos, squad.team, rallyRadius);
            boolean bl = currentlyBlocked = enemies >= rallyEnemiesRequired;
            if (squad.isRallyBlocked != currentlyBlocked) {
                squad.isRallyBlocked = currentlyBlocked;
                anySquadStatusChanged = true;
            }
            if (!currentlyBlocked) continue;
            if (squad.team.equalsIgnoreCase("Blue")) {
                teamBlueBlocked = true;
                continue;
            }
            teamRedBlocked = true;
        }
        Boolean cachedBlue = lastBlueBlockedMap.getOrDefault(dimKey, false);
        Boolean cachedRed = lastRedBlockedMap.getOrDefault(dimKey, false);
        if (forceSend || changed || anySquadStatusChanged || teamBlueBlocked != cachedBlue || teamRedBlocked != cachedRed || blueBleed || redBleed) {
            GameLogicEvents.sendSyncPacket(level, data, blueBleed, redBleed, teamBlueBlocked, teamRedBlocked);
            if (anySquadStatusChanged) {
                PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((ServerLevel)level).dimension()), (Object)new PacketSyncSquads(data.squads));
            }
            lastBlueBlockedMap.put(dimKey, teamBlueBlocked);
            lastRedBlockedMap.put(dimKey, teamRedBlocked);
        }
        if (changed || anySquadStatusChanged) {
            data.setDirty();
        }
    }

    private static void validateAndSync(ServerLevel level, boolean forceSend) {
        GameLogicEvents.validateAndSync(level, forceSend, false, false);
    }

    private static boolean validateRallies(ServerLevel level, List<BlockPos> rallies) {
        return rallies.removeIf(pos -> {
            if (level.isLoaded(pos)) {
                return !(level.getBlockState(pos).getBlock() instanceof RallyPointBlock);
            }
            return false;
        });
    }

    private static boolean isEnemyNearby(ServerLevel level, BlockPos pos, String allyTeamName, int radius, int minCount) {
        return GameLogicEvents.getEnemyCount(level, pos, allyTeamName, radius) >= minCount;
    }

    private static int getEnemyCount(ServerLevel level, BlockPos pos, String allyTeamName, int radius) {
        AABB checkArea = new AABB(pos).inflate((double)radius);
        List enemies = level.getEntitiesOfClass(ServerPlayer.class, checkArea);
        int count = 0;
        for (ServerPlayer p : enemies) {
            if (p.isSpectator() || p.getTeam() != null && p.getTeam().getName().equalsIgnoreCase(allyTeamName)) continue;
            ++count;
        }
        return count;
    }

    public static int getEnemyCount(ServerLevel level, BlockPos pos, String allyTeamName) {
        AABB checkArea = new AABB(pos).inflate(40.0);
        List enemies = level.getEntitiesOfClass(ServerPlayer.class, checkArea);
        int count = 0;
        for (ServerPlayer p : enemies) {
            if (p.isSpectator() || p.getTeam() != null && p.getTeam().getName().equalsIgnoreCase(allyTeamName)) continue;
            ++count;
        }
        return count;
    }

    public static void checkGameOver(ServerLevel level, AASWorldData data) {
        if (data.blueTickets <= 0) {
            data.blueTickets = 0;
            GameLogicEvents.executeVictory(level, data, false);
        } else if (data.redTickets <= 0) {
            data.redTickets = 0;
            GameLogicEvents.executeVictory(level, data, true);
        }
    }

    private static void executeVictory(ServerLevel level, AASWorldData data, boolean blueWon) {
        String winnerName;
        String winnerFaction;
        data.isGameStarted = false;
        data.setDirty();
        if (blueWon) {
            winnerFaction = data.blueFaction;
            String string = winnerName = winnerFaction == null || winnerFaction.equals("none") || winnerFaction.equals("bluefor") ? (String)AASConfig.BLUE_TEAM_CUSTOM_NAME.get() : GameLogicEvents.formatFactionName(winnerFaction);
            if (winnerName.isEmpty()) {
                winnerName = "BLUE TEAM";
            }
        } else {
            winnerFaction = data.redFaction;
            String string = winnerName = winnerFaction == null || winnerFaction.equals("none") || winnerFaction.equals("redfor") ? (String)AASConfig.RED_TEAM_CUSTOM_NAME.get() : GameLogicEvents.formatFactionName(winnerFaction);
            if (winnerName.isEmpty()) {
                winnerName = "RED TEAM";
            }
        }
        String subText = (blueWon ? data.blueTickets : data.redTickets) + " tickets remaining";
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketOpenVictoryScreen(winnerName, winnerFaction, subText, blueWon));
        ServerScoreboard scoreboard = level.getScoreboard();
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
            if (player.isCreative() || player.isSpectator()) continue;
            if (player.getPersistentData().getBoolean("AAS_IsDowned")) {
                DownedHandler.revivePlayer(player);
            }
            player.setHealth(player.getMaxHealth());
            player.getPersistentData().putString("AAS_CurrentKit", "Unassigned");
            player.getPersistentData().remove("AAS_PendingKit");
            player.getInventory().clearContent();
            ResupplyHandler.clearCurios(player);
            player.inventoryMenu.broadcastChanges();
            player.containerMenu.broadcastChanges();
            String pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
            BlockPos spawnPos = null;
            if (pTeam.equals("BLUE")) {
                spawnPos = data.blueSpawns.get(currentDim);
            } else if (pTeam.equals("RED")) {
                spawnPos = data.redSpawns.get(currentDim);
            }
            if (spawnPos == null) continue;
            player.setRespawnPosition(level.dimension(), spawnPos, 0.0f, true, false);
            if (!player.isAlive()) continue;
            player.teleportTo((double)spawnPos.getX() + 0.5, (double)spawnPos.getY(), (double)spawnPos.getZ() + 0.5);
        }
    }

    private static String formatFactionName(String faction) {
        return faction.replace("_", " ").toUpperCase();
    }

    private static void sendTitleToLevel(ServerLevel level, String text, ChatFormatting color) {
        MutableComponent title = Component.literal((String)text).withStyle(color).withStyle(ChatFormatting.BOLD);
        for (ServerPlayer player : level.players()) {
            player.connection.send((Packet)new ClientboundSetTitlesAnimationPacket(0, 20, 10));
            player.connection.send((Packet)new ClientboundSetTitleTextPacket((Component)title));
        }
    }

    private static void sendTitleToAll(MinecraftServer server, String text, ChatFormatting color) {
        MutableComponent title = Component.literal((String)text).withStyle(color).withStyle(ChatFormatting.BOLD);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send((Packet)new ClientboundSetTitlesAnimationPacket(0, 20, 10));
            player.connection.send((Packet)new ClientboundSetTitleTextPacket((Component)title));
        }
    }

    private static void broadcastMessage(ServerLevel level, String text, ChatFormatting color) {
        level.getServer().getPlayerList().broadcastSystemMessage((Component)Component.literal((String)text).withStyle(color), false);
    }

    private static void sendSyncPacket(ServerLevel level, AASWorldData data) {
        GameLogicEvents.sendSyncPacket(level, data, false, false);
    }

    private static void sendSyncPacket(ServerLevel level, AASWorldData data, boolean blueBleed, boolean redBleed) {
        String dimKey = level.dimension().location().toString();
        boolean bBlocked = lastBlueBlockedMap.getOrDefault(dimKey, false);
        boolean rBlocked = lastRedBlockedMap.getOrDefault(dimKey, false);
        GameLogicEvents.sendSyncPacket(level, data, blueBleed, redBleed, bBlocked, rBlocked);
    }

    private static void sendSyncPacket(ServerLevel level, AASWorldData data, boolean blueBleed, boolean redBleed, boolean bBlocked, boolean rBlocked) {
        PacketSyncGameData packet = GameLogicEvents.createSyncPacket(data, blueBleed, redBleed, bBlocked, rBlocked);
        PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((ServerLevel)level).dimension()), (Object)packet);
    }

    private static PacketSyncGameData createSyncPacket(AASWorldData data, boolean blueBleed, boolean redBleed, boolean bBlocked, boolean rBlocked) {
        boolean hasBlue = !data.blueRallies.isEmpty();
        boolean hasRed = !data.redRallies.isEmpty();
        String bName = data.blueFaction.equals("none") ? (String)AASConfig.BLUE_TEAM_CUSTOM_NAME.get() : data.blueFaction.toUpperCase();
        String rName = data.redFaction.equals("none") ? (String)AASConfig.RED_TEAM_CUSTOM_NAME.get() : data.redFaction.toUpperCase();
        HashMap<String, String> pKits = new HashMap<String, String>();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                String current = p.getPersistentData().getString("AAS_CurrentKit");
                String pending = p.getPersistentData().getString("AAS_PendingKit");
                String displayKit = !pending.isEmpty() ? pending : current;
                pKits.put(p.getScoreboardName(), displayKit.isEmpty() ? "Unassigned" : displayKit);
            }
        }
        return new PacketSyncGameData(data.blueTickets, data.redTickets, hasBlue, hasRed, blueBleed, redBleed, data.respawnTimer, bBlocked, rBlocked, (Boolean)AASConfig.HUB_SPAWN_COSTS_MATERIALS.get(), (Integer)AASConfig.HUB_SPAWN_MATERIAL_COST.get(), data.mapCenterX, data.mapCenterZ, data.mapSizeBlocks, data.currentMapImage, data.markedVehicles, data.hubs, data.blueFaction, data.redFaction, bName, rName, data.isGameStarted, data.capturePoints, data.blueSpawns, data.redSpawns, data.neutralSpawns, pKits, data.activeMarkers, data.voteActive, data.voteTimer, data.votes, data.blueCMDId, data.redCMDId, data.blueCmdVoteActive, data.blueCmdCandidateName, data.blueCmdCandidateId, data.blueCmdVoteTimer, data.blueCmdVotes, data.redCmdVoteActive, data.redCmdCandidateName, data.redCmdCandidateId, data.redCmdVoteTimer, data.redCmdVotes, data.activeStrikes, data.blueArtRequest != null ? data.blueArtRequest.pos : BlockPos.ZERO, data.redArtRequest != null ? data.redArtRequest.pos : BlockPos.ZERO, data.blueArtRequest != null ? data.blueArtRequest.timer : 0, data.redArtRequest != null ? data.redArtRequest.timer : 0, data.blueArtRequest != null ? data.blueArtRequest.requesterName : "", data.redArtRequest != null ? data.redArtRequest.requesterName : "", data.blueReady, data.redReady);
    }

    private static boolean canCapture(AASWorldData.CapturePoint target, String team, AASWorldData data) {
        int currentPriority;
        int n = currentPriority = team.equals("BLUE") ? target.bluePriority : target.redPriority;
        if (currentPriority <= 1) {
            return true;
        }
        int requiredPriority = currentPriority - 1;
        for (AASWorldData.CapturePoint p : data.capturePoints) {
            int pPriority = team.equals("BLUE") ? p.bluePriority : p.redPriority;
            if (pPriority != requiredPriority || !p.owner.equals(team)) continue;
            return true;
        }
        return false;
    }

    public static void leaveCurrentSquad(ServerPlayer player, AASWorldData data) {
        String pName = player.getScoreboardName();
        for (AASWorldData.Squad s2 : data.squads) {
            if (!s2.members.contains(pName)) continue;
            s2.members.remove(pName);
            if (!s2.leader.equals(pName)) continue;
            PacketSquadAction.removeRadio(player);
            if (s2.members.isEmpty()) continue;
            s2.leader = s2.members.get(0);
            ServerPlayer newLeader = player.server.getPlayerList().getPlayerByName(s2.leader);
            if (newLeader == null) continue;
            PacketSquadAction.updatePlayerTags(newLeader, s2.id, true);
            if (!((Boolean)AASConfig.AUTO_GIVE_SL_RADIO.get()).booleanValue()) continue;
            PacketSquadAction.giveRadio(newLeader);
        }
        data.squads.removeIf(s -> s.members.isEmpty());
        data.setDirty();
        PacketHandler.sendToAllClients(player.serverLevel(), data);
        player.getPersistentData().putString("AAS_CurrentKit", "Unassigned");
        player.getPersistentData().putString("AAS_PendingKit", "");
        player.getInventory().clearContent();
        ResupplyHandler.clearCurios(player);
        player.inventoryMenu.broadcastChanges();
        player.containerMenu.broadcastChanges();
        PacketSquadAction.removePlayerTags(player);
        player.displayClientMessage((Component)Component.literal((String)"\u00a7eSquad left. Kit and reservations cleared."), true);
        data.setDirty();
        PacketHandler.sendToAllClients(player.serverLevel(), data);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!event.getEntity().level().isClientSide) {
            ServerPlayer player = (ServerPlayer)event.getEntity();
            ServerLevel level = player.serverLevel();
            AASWorldData data = AASWorldData.get(level);
            if (data.isGameStarted) {
                return;
            }
            PacketSquadAction.leaveCurrentSquad(player, data);
            player.getPersistentData().putString("AAS_CurrentKit", "Unassigned");
            player.getPersistentData().remove("AAS_PendingKit");
            player.getInventory().clearContent();
            ResupplyHandler.clearCurios(player);
            player.inventoryMenu.broadcastChanges();
            data.setDirty();
            PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((ServerLevel)level).dimension()), (Object)new PacketSyncSquads(data.squads));
        }
    }

    private static void playSirenForTeam(ServerLevel level, String teamName) {
        for (ServerPlayer player : level.players()) {
            if (player.getTeam() == null || !player.getTeam().getName().equalsIgnoreCase(teamName)) continue;
            player.playNotifySound((SoundEvent)ModSounds.SIREN_ALARM.get(), SoundSource.MASTER, 1.0f, 1.0f);
        }
    }

    public static void checkSirenManual(ServerLevel level, AASWorldData data) {
        if (!data.isGameStarted || !((Boolean)AASConfig.LOW_TICKETS_SIREN.get()).booleanValue()) {
            return;
        }
        if (data.blueTickets <= 50 && data.blueTickets >= 0 && !data.playedBlueSiren) {
            GameLogicEvents.playSirenForTeam(level, "Blue");
            data.playedBlueSiren = true;
            data.setDirty();
        }
        if (data.redTickets <= 50 && data.redTickets >= 0 && !data.playedRedSiren) {
            GameLogicEvents.playSirenForTeam(level, "Red");
            data.playedRedSiren = true;
            data.setDirty();
        }
    }

    @SubscribeEvent
    public static void enforceMortarShellLimit(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (event.player.tickCount % 10 != 0) {
            return;
        }
        Player player = event.player;
        if (player.isCreative() || player.isSpectator()) {
            return;
        }
        Item mortarItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "mortar_shell"));
        if (mortarItem == null || mortarItem == Items.AIR) {
            mortarItem = Items.ARROW;
        }
        int totalCount = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); ++i) {
            ItemStack stack = inv.getItem(i);
            if (stack.getItem() != mortarItem) continue;
            totalCount += stack.getCount();
        }
        if (totalCount > 8) {
            int toRemove = totalCount - 8;
            int removedSoFar = 0;
            for (int i = 0; i < inv.getContainerSize() && removedSoFar < toRemove; ++i) {
                ItemStack stack = inv.getItem(i);
                if (stack.getItem() != mortarItem) continue;
                int shrinkAmount = Math.min(stack.getCount(), toRemove - removedSoFar);
                stack.shrink(shrinkAmount);
                removedSoFar += shrinkAmount;
                ItemStack dropped = new ItemStack((ItemLike)mortarItem, shrinkAmount);
                player.drop(dropped, false, true);
            }
            player.displayClientMessage((Component)Component.literal((String)"You can only carry up to 8 Mortar Shells!").withStyle(ChatFormatting.RED), true);
        }
    }

    private static void checkCmdVoteStatus(ServerLevel level, MinecraftServer server, AASWorldData data, String team) {
        boolean fail;
        Map<UUID, Boolean> votesMap;
        boolean isBlue = team.equals("BLUE");
        boolean active = isBlue ? data.blueCmdVoteActive : data.redCmdVoteActive;
        String candidateName = isBlue ? data.blueCmdCandidateName : data.redCmdCandidateName;
        int candidateId = isBlue ? data.blueCmdCandidateId : data.redCmdCandidateId;
        int timer = isBlue ? data.blueCmdVoteTimer : data.redCmdVoteTimer;
        Map<UUID, Boolean> map = votesMap = isBlue ? data.blueCmdVotes : data.redCmdVotes;
        if (!active) {
            return;
        }
        List<ServerPlayer> otherSLs = level.players().stream().filter(p -> p.getTeam() != null && p.getTeam().getName().equalsIgnoreCase(team)).filter(p -> p.getPersistentData().getBoolean("AAS_IsSquadLeader")).filter(p -> !p.getScoreboardName().equals(candidateName)).toList();
        int totalVoters = otherSLs.size();
        int needed = totalVoters > 0 ? (int)Math.ceil((double)totalVoters / 2.0) : 1;
        long yes = votesMap.entrySet().stream().filter(e -> server.getPlayerList().getPlayer((UUID)e.getKey()) != null).filter(Map.Entry::getValue).count();
        long no = votesMap.entrySet().stream().filter(e -> server.getPlayerList().getPlayer((UUID)e.getKey()) != null).filter(e -> (Boolean)e.getValue() == false).count();
        boolean win = totalVoters > 0 && yes >= (long)needed;
        boolean bl = fail = totalVoters > 0 && no >= (long)totalVoters || timer <= 0;
        if (win || fail) {
            if (win) {
                if (isBlue) {
                    data.blueCMDId = candidateId;
                } else {
                    data.redCMDId = candidateId;
                }
                GameLogicEvents.broadcastTeamMessage(level, team, team + " COMMANDER ASSIGNED: " + candidateName, ChatFormatting.GREEN);
            } else {
                GameLogicEvents.broadcastTeamMessage(level, team, team + " CMD Application rejected or timed out.", ChatFormatting.RED);
            }
            if (isBlue) {
                data.blueCmdVoteActive = false;
                data.blueCmdVotes.clear();
            } else {
                data.redCmdVoteActive = false;
                data.redCmdVotes.clear();
            }
            data.setDirty();
            PacketHandler.sendToAllClients(level, data);
        }
    }

    private static void broadcastTeamMessage(ServerLevel level, String team, String text, ChatFormatting color) {
        MutableComponent message = Component.literal((String)text).withStyle(color);
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            if (player.getTeam() == null || !player.getTeam().getName().equalsIgnoreCase(team)) continue;
            player.sendSystemMessage((Component)message);
        }
    }

    private static boolean lambda$onServerTick$3(long time, AASWorldData.MapMarker m) {
        return time >= m.expiryTick;
    }
}

