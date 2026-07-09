/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.block.VehicleSpawnerBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

public class PacketUpdateSpawner {
    private final BlockPos pos;
    private final int respawnTime;
    private final int initialTime;
    private final String vehicleId;
    private final float vehicleYaw;

    public PacketUpdateSpawner(BlockPos pos, int respawn, int init, String id, float yaw) {
        this.pos = pos;
        this.respawnTime = respawn;
        this.initialTime = init;
        this.vehicleId = id;
        this.vehicleYaw = yaw;
    }

    public static void encode(PacketUpdateSpawner msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeInt(msg.respawnTime);
        buf.writeInt(msg.initialTime);
        buf.writeUtf(msg.vehicleId);
        buf.writeFloat(msg.vehicleYaw);
    }

    public static PacketUpdateSpawner decode(FriendlyByteBuf buf) {
        return new PacketUpdateSpawner(buf.readBlockPos(), buf.readInt(), buf.readInt(), buf.readUtf(), buf.readFloat());
    }

    public static void handle(PacketUpdateSpawner msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            BlockEntity be;
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null && player.isCreative() && (be = player.level().getBlockEntity(msg.pos)) instanceof VehicleSpawnerBlockEntity) {
                VehicleSpawnerBlockEntity spawner = (VehicleSpawnerBlockEntity)be;
                spawner.respawnTimeSettings = msg.respawnTime;
                spawner.initialTimeSettings = msg.initialTime;
                spawner.vehicleIdString = msg.vehicleId;
                spawner.vehicleYaw = msg.vehicleYaw;
                spawner.setChanged();
                player.level().sendBlockUpdated(msg.pos, spawner.getBlockState(), spawner.getBlockState(), 3);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

