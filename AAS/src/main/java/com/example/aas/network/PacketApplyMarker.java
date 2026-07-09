/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.config.AASConfig;
import com.example.aas.item.SupplyTruckMarkerItem;
import com.example.aas.item.VehicleMarkerItem;
import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

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

    public static void handle(PacketApplyMarker msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            Entity target = level.getEntity(msg.targetEntityId);
            ItemStack stack = player.getMainHandItem();
            AASWorldData data = AASWorldData.get(level);
            if (target != null && player.distanceTo(target) < 10.0f) {
                String team = "";
                String type = "";
                int penalty = 0;
                int maxMats = 0;
                boolean isSupply = false;
                Item patt1892$temp = stack.getItem();
                if (patt1892$temp instanceof VehicleMarkerItem) {
                    VehicleMarkerItem markerItem = (VehicleMarkerItem)patt1892$temp;
                    team = markerItem.getTeam();
                    type = markerItem.getType();
                    penalty = markerItem.getPenalty();
                    maxMats = markerItem.getMaxMats();
                } else {
                    Item patt2218$temp = stack.getItem();
                    if (patt2218$temp instanceof SupplyTruckMarkerItem) {
                        SupplyTruckMarkerItem supplyItem = (SupplyTruckMarkerItem)patt2218$temp;
                        team = supplyItem.getTeam();
                        type = supplyItem.getVehicleType();
                        penalty = supplyItem.getPenalty();
                        maxMats = supplyItem.getMaxMats();
                        isSupply = true;
                    }
                }
                if (!team.isEmpty()) {
                    target.getPersistentData().putString("AAS_VehicleTeam", team);
                    target.getPersistentData().putString("AAS_VehicleType", type);
                    target.getPersistentData().putInt("AAS_TicketPenalty", penalty);
                    if (isSupply) {
                        target.getPersistentData().putBoolean("AAS_IsSupplyTruck", true);
                        target.getPersistentData().putInt("AAS_SupplyAmmo", ((Integer)AASConfig.SUPPLY_TRUCK_CRATES.get()).intValue());
                    } else {
                        target.getPersistentData().remove("AAS_IsSupplyTruck");
                        target.getPersistentData().remove("AAS_SupplyAmmo");
                    }
                    if (maxMats > 0) {
                        target.getPersistentData().putInt("AAS_VehicleMaxMats", maxMats);
                        target.getPersistentData().putInt("AAS_VehicleMats", maxMats);
                    }
                    data.markedVehicles.removeIf(v -> v.uuid.equals(target.getUUID()));
                    BlockPos spawnerPos = null;
                    if (target.getPersistentData().contains("AAS_SpawnerPos")) {
                        spawnerPos = BlockPos.of((long)target.getPersistentData().getLong("AAS_SpawnerPos"));
                    }
                    data.markedVehicles.add(new AASWorldData.VehicleRecord(target.getUUID(), team, type, target.getX(), target.getY(), target.getZ(), target.getYRot(), spawnerPos));
                    PacketHandler.sendToAllClients(level, data);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

