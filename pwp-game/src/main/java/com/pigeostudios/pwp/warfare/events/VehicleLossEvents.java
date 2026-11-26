package com.pigeostudios.pwp.warfare.events;

import static com.pigeostudios.pwp.warfare.events.GameLogicEvents.*;
import static com.pigeostudios.pwp.warfare.events.PlayerItemEvents.*;
import static com.pigeostudios.pwp.warfare.events.PlayerLifecycleEvents.*;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.stats.MatchStatsTracker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Потеря техники: сущность ушла из мира (уничтожена/деспавн) — тикеты, обломки, перезапуск спавнера.
 *
 * <p>Вынесено из {@link GameLogicEvents} без изменения кода (этап 13 ROADMAP).
 */
@EventBusSubscriber(modid = "pwpwarfare", bus = Bus.FORGE)
public final class VehicleLossEvents {
    private VehicleLossEvents() {}

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

    static void processEntityLoss(Entity entity) {
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
                                        level,
                                        Component.translatable("pwpwarfare.msg.vehicle.lost.blue", vType, penalty),
                                        ChatFormatting.BLUE);
                            } else if (vTeam.equalsIgnoreCase("RED")) {
                                data.match.redTickets = Math.max(0, data.match.redTickets - penalty);
                                ticketsChanged = true;
                                broadcastMessage(
                                        level,
                                        Component.translatable("pwpwarfare.msg.vehicle.lost.red", vType, penalty),
                                        ChatFormatting.RED);
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

    static boolean hasCorrectKit(String vType, String kit) {
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

    static float getVehicleHealth(Entity entity) {
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

    static void processWreckLoss(Entity entity, WarfareWorldData data, ServerLevel level) {
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
                    broadcastMessage(
                            level,
                            Component.translatable("pwpwarfare.msg.vehicle.lost.blue", vType, penalty),
                            ChatFormatting.BLUE);
                } else if (vTeam.equalsIgnoreCase("RED")) {
                    data.match.redTickets = Math.max(0, data.match.redTickets - penalty);
                    ticketsChanged = true;
                    broadcastMessage(
                            level,
                            Component.translatable("pwpwarfare.msg.vehicle.lost.red", vType, penalty),
                            ChatFormatting.RED);
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
}
