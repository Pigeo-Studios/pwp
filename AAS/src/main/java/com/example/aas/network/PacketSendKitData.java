/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.client.AASClipboard;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketSendKitData {
    private final Map<String, CompoundTag> data;

    public PacketSendKitData(Map<String, CompoundTag> data) {
        this.data = data;
    }

    public static void encode(PacketSendKitData msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.data.size());
        msg.data.forEach((name, tag) -> {
            buf.writeUtf(name);
            buf.writeNbt(tag);
        });
    }

    public static PacketSendKitData decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        HashMap<String, CompoundTag> map = new HashMap<String, CompoundTag>();
        for (int i = 0; i < size; ++i) {
            map.put(buf.readUtf(), buf.readNbt());
        }
        return new PacketSendKitData(map);
    }

    public static void handle(PacketSendKitData msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (msg.data.size() > 1) {
                AASClipboard.teamKitsData = msg.data;
            } else {
                msg.data.values().stream().findFirst().ifPresent(tag -> {
                    AASClipboard.kitData = tag;
                });
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

