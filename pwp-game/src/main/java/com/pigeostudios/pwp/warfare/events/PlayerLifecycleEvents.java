package com.pigeostudios.pwp.warfare.events;

import static com.pigeostudios.pwp.warfare.events.GameLogicEvents.*;
import static com.pigeostudios.pwp.warfare.events.PlayerItemEvents.*;
import static com.pigeostudios.pwp.warfare.events.VehicleLossEvents.*;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.PlayerWarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.ResupplyHandler;
import com.pigeostudios.pwp.warfare.server.MarkerManager;
import com.pigeostudios.pwp.warfare.server.PathManager;
import com.pigeostudios.pwp.warfare.squad.SquadActions;
import com.pigeostudios.pwp.warfare.staging.StagingPackets;
import com.pigeostudios.pwp.warfare.staging.StagingService;
import com.pigeostudios.pwp.warfare.stats.MatchStatsTracker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.cosmetics.CosmeticService;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.Clone;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Жизненный цикл игрока в матче: вход/выход, смерть, возрождение, клон после смерти, смена измерения, выход из отряда.
 *
 * <p>Вынесено из {@link GameLogicEvents} без изменения кода (этап 13 ROADMAP).
 */
@EventBusSubscriber(modid = "pwpwarfare", bus = Bus.FORGE)
public final class PlayerLifecycleEvents {
    private PlayerLifecycleEvents() {}

    @SubscribeEvent
    public static void onPlayerChangeDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ServerLevel level = player.serverLevel();
            WarfareWorldData data = WarfareWorldData.get(level);
            SquadActions.leaveCurrentSquad(player, data);
            PacketHandler.syncSquadsAll(data.squadRegistry.squads);
            player.sendSystemMessage(Component.translatable("pwpwarfare.msg.squad.left_world_change")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    static String findRequiredPointName(
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
            if (!data.match.isGameStarted
                    && !data.match.waitingActive
                    && com.pwp.api.security.PwpPermissions.isAdmin(player)) {
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
    public static void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        if (!event.getEntity().level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.getEntity();
            com.pigeostudios.pwp.warfare.server.AfkTicker.forget(player);
            com.pigeostudios.pwp.warfare.server.VehicleActionService.forget(player);
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
        player.displayClientMessage(
                Component.translatable("pwpwarfare.msg.squad.left_kit_cleared").withStyle(ChatFormatting.YELLOW), true);
        data.setDirty();
        PacketHandler.sendToAllClients(player.serverLevel(), data);
    }
}
