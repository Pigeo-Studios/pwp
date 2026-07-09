/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketPasteKit {
    private final String team;
    private final String kitName;
    private final CompoundTag tag;

    public PacketPasteKit(String team, String kitName, CompoundTag tag) {
        this.team = team;
        this.kitName = kitName;
        this.tag = tag;
    }

    public static void encode(PacketPasteKit msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.team);
        buf.writeUtf(msg.kitName);
        buf.writeNbt(msg.tag);
    }

    public static PacketPasteKit decode(FriendlyByteBuf buf) {
        return new PacketPasteKit(buf.readUtf(), buf.readUtf(), buf.readNbt());
    }

    public static void handle(PacketPasteKit msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null || !player.isCreative()) {
                return;
            }
            AASWorldData worldData = AASWorldData.get(player.serverLevel());
            AASWorldData.KitInfo newKit = AASWorldData.KitInfo.load(msg.tag);
            newKit.name = msg.kitName;
            if (msg.team.equals("BLUE")) {
                worldData.blueKits.put(msg.kitName, newKit);
            } else {
                worldData.redKits.put(msg.kitName, newKit);
            }
            worldData.setDirty();
            PacketHandler.sendToAllClients(player.serverLevel(), worldData);
        });
        ctx.get().setPacketHandled(true);
    }
}

