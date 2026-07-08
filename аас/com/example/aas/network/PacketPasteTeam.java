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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketPasteTeam {
    private final String team;
    private final Map<String, CompoundTag> kits;

    public PacketPasteTeam(String team, Map<String, CompoundTag> kits) {
        this.team = team;
        this.kits = kits;
    }

    public static void encode(PacketPasteTeam msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.team);
        buf.writeInt(msg.kits.size());
        msg.kits.forEach((name, tag) -> {
            buf.m_130070_(name);
            buf.m_130079_(tag);
        });
    }

    public static PacketPasteTeam decode(FriendlyByteBuf buf) {
        String t = buf.m_130277_();
        int size = buf.readInt();
        HashMap<String, CompoundTag> map = new HashMap<String, CompoundTag>();
        for (int i = 0; i < size; ++i) {
            map.put(buf.m_130277_(), buf.m_130260_());
        }
        return new PacketPasteTeam(t, map);
    }

    public static void handle(PacketPasteTeam msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null || !player.m_7500_()) {
                return;
            }
            AASWorldData worldData = AASWorldData.get(player.m_284548_());
            Map<String, AASWorldData.KitInfo> target = msg.team.equals("BLUE") ? worldData.blueKits : worldData.redKits;
            msg.kits.forEach((name, tag) -> {
                if (target.containsKey(name)) {
                    target.put((String)name, AASWorldData.KitInfo.load(tag));
                }
            });
            worldData.m_77762_();
            PacketHandler.sendToAllClients(player.m_284548_(), worldData);
        });
        ctx.get().setPacketHandled(true);
    }
}

