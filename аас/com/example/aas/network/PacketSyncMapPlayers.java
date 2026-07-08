/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.network.ClientPacketHandler;
import com.example.aas.network.MapPlayerInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncMapPlayers {
    private final List<MapPlayerInfo> players;

    public PacketSyncMapPlayers(List<MapPlayerInfo> players) {
        this.players = players;
    }

    public List<MapPlayerInfo> getPlayers() {
        return this.players;
    }

    public static void encode(PacketSyncMapPlayers msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.players.size());
        for (MapPlayerInfo p : msg.players) {
            buf.m_130070_(p.name);
            buf.m_130077_(p.uuid);
            buf.writeDouble(p.x);
            buf.writeDouble(p.z);
            buf.writeFloat(p.rot);
            buf.writeInt(p.squadId);
            buf.writeBoolean(p.isLeader);
            buf.writeBoolean(p.isDowned);
            buf.writeLong(p.lastShoutTime);
            buf.writeBoolean(p.inVehicle);
            buf.writeInt(p.vehicleId);
            buf.writeInt(p.seatIndex);
            buf.m_130070_(p.team);
        }
    }

    public static PacketSyncMapPlayers decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        ArrayList<MapPlayerInfo> list = new ArrayList<MapPlayerInfo>(size);
        for (int i = 0; i < size; ++i) {
            list.add(new MapPlayerInfo(buf.m_130277_(), buf.m_130259_(), buf.readDouble(), buf.readDouble(), buf.readFloat(), buf.readInt(), buf.readBoolean(), buf.readBoolean(), buf.readLong(), buf.readBoolean(), buf.readInt(), buf.readInt(), buf.m_130277_()));
        }
        return new PacketSyncMapPlayers(list);
    }

    public static void handle(PacketSyncMapPlayers msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientPacketHandler.handleSyncMap(msg)));
        ctx.get().setPacketHandled(true);
    }
}

