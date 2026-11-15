package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.item.SupplyTruckMarkerItem;
import com.pigeostudios.pwp.warfare.item.VehicleMarkerItem;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет применения маркера транспортного средства или грузовика снабжения
// Отправляется клиентом при использовании маркера на сущности
public class PacketApplyMarker {
    private final int targetEntityId;

    public PacketApplyMarker(int entityId) {
        this.targetEntityId = entityId;
    }

    public static void encode(PacketApplyMarker msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.targetEntityId);
    }

    public static PacketApplyMarker decode(FriendlyByteBuf buf) {
        return new PacketApplyMarker(buf.readInt());
    }

    // Обрабатывает применение маркера: проверяет дистанцию, тип предмета,
    // записывает данные о транспортном средстве в мир и синхронизирует с клиентами
    public static void handle(PacketApplyMarker msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                Entity target = level.getEntity(msg.targetEntityId);
                ItemStack stack = player.getMainHandItem();
                WarfareWorldData data = WarfareWorldData.get(level);
                if (target != null && player.distanceTo(target) < 10.0F) {
                    String team = "";
                    String type = "";
                    int penalty = 0;
                    int maxMats = 0;
                    boolean isSupply = false;
                    if (stack.getItem() instanceof VehicleMarkerItem markerItem) {
                        team = markerItem.getTeam();
                        type = markerItem.getType();
                        penalty = markerItem.getPenalty();
                        maxMats = markerItem.getMaxMats();
                    } else if (stack.getItem() instanceof SupplyTruckMarkerItem supplyItem) {
                        team = supplyItem.getTeam();
                        type = supplyItem.getVehicleType();
                        penalty = supplyItem.getPenalty();
                        maxMats = supplyItem.getMaxMats();
                        isSupply = true;
                    }

                    if (!team.isEmpty()) {
                        WarfareData.of(target).putString(WarfareKeys.VEHICLE_TEAM, team);
                        WarfareData.of(target).putString(WarfareKeys.VEHICLE_TYPE, type);
                        WarfareData.of(target).putInt(WarfareKeys.TICKET_PENALTY, penalty);
                        if (isSupply) {
                            WarfareData.of(target).putBoolean(WarfareKeys.IS_SUPPLY_TRUCK, true);
                            WarfareData.of(target)
                                    .putInt(WarfareKeys.SUPPLY_AMMO, (Integer) WarfareConfig.SUPPLY_TRUCK_CRATES.get());
                        } else {
                            WarfareData.of(target).remove(WarfareKeys.IS_SUPPLY_TRUCK);
                            WarfareData.of(target).remove(WarfareKeys.SUPPLY_AMMO);
                        }

                        if (maxMats > 0) {
                            WarfareData.of(target).putInt(WarfareKeys.VEHICLE_MAX_MATS, maxMats);
                            WarfareData.of(target).putInt(WarfareKeys.VEHICLE_MATS, maxMats);
                        }

                        data.markedVehicles.removeIf(v -> v.uuid.equals(target.getUUID()));
                        BlockPos spawnerPos = null;
                        if (WarfareData.of(target).contains(WarfareKeys.SPAWNER_POS)) {
                            spawnerPos = BlockPos.of(WarfareData.of(target).getLong(WarfareKeys.SPAWNER_POS));
                        }

                        int tPenalty = WarfareData.of(target).getInt(WarfareKeys.TICKET_PENALTY);
                        data.markedVehicles.add(new WarfareWorldData.VehicleRecord(
                                target.getUUID(),
                                team,
                                type,
                                target.getX(),
                                target.getY(),
                                target.getZ(),
                                target.getYRot(),
                                spawnerPos,
                                tPenalty));
                        PacketHandler.sendToAllClients(level, data);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
