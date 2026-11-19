package com.pigeostudios.pwp.warfare.match;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.events.GameLogicEvents;
import com.pigeostudios.pwp.warfare.staging.StagingService;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.entity.PartEntity;

/**
 * Защита мейн-зон каждый тик сервера: удержание игроков и техники в своём мейне до GO / в подготовке
 * Invasion, урон и выталкивание чужих. Вынесено из {@code GameLogicEvents.handleMainProtectionZones}
 * без изменений логики (M5, 19.11.2026); вызывается из {@code GameLogicEvents.onServerTick}.
 */
public final class MainZoneGuard {
    private MainZoneGuard() {}

    public static void tick(ServerLevel level, WarfareWorldData data) {
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
                                                            GameLogicEvents.formatTime(data.match.invasionSetupTimer))
                                                    : Component.translatable(
                                                            "pwpwarfare.main.hold_waiting",
                                                            GameLogicEvents.formatTime(data.match.waitingTimer));
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
                                                    Component.translatable("pwpwarfare.msg.main.enemy_kill_in", secLeft)
                                                            .withStyle(ChatFormatting.RED)));
                                            player.connection.send(new ClientboundSetTitleTextPacket(
                                                    Component.translatable("pwpwarfare.msg.main.enemy_title")
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
                                                            Component.translatable(
                                                                            "pwpwarfare.msg.main.enemy_vehicle",
                                                                            secLeft)
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
}
