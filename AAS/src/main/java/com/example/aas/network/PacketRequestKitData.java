/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.network.PacketDistributor
 */
package com.example.aas.network;

import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSendKitData;
import com.example.aas.world.AASWorldData;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PacketRequestKitData {
    private final String team;
    private final String kitName;

    public PacketRequestKitData(String team, String kitName) {
        this.team = team;
        this.kitName = kitName;
    }

    public static void encode(PacketRequestKitData msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.team);
        buf.writeUtf(msg.kitName);
    }

    public static PacketRequestKitData decode(FriendlyByteBuf buf) {
        return new PacketRequestKitData(buf.readUtf(), buf.readUtf());
    }

    public static void handle(PacketRequestKitData msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null || !player.isCreative()) {
                return;
            }
            AASWorldData data = AASWorldData.get(player.serverLevel());
            HashMap<String, CompoundTag> toSend = new HashMap<String, CompoundTag>();
            if (msg.kitName.equals("ALL")) {
                Map<String, AASWorldData.KitInfo> source = msg.team.equals("BLUE") ? data.blueKits : data.redKits;
                source.forEach((name, kit) -> toSend.put((String)name, kit.save()));
            } else {
                AASWorldData.KitInfo kit2;
                AASWorldData.KitInfo kitInfo = kit2 = msg.team.equals("BLUE") ? data.blueKits.get(msg.kitName) : data.redKits.get(msg.kitName);
                if (kit2 != null) {
                    toSend.put(msg.kitName, kit2.save());
                }
            }
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), (Object)new PacketSendKitData(toSend));
        });
        ctx.get().setPacketHandled(true);
    }
}

