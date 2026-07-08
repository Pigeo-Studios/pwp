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

import com.example.aas.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncPoint {
    public final boolean isInside;
    public final String name;
    public final String owner;
    public final float progress;
    public final boolean isLocked;
    public final String nextObjective;
    public final boolean isContested;
    public final String capturingTeam;
    public final int captureRate;

    public PacketSyncPoint(boolean isInside, String name, String owner, float progress, boolean locked, String nextObj, boolean contested, String capturingTeam, int captureRate) {
        this.isInside = isInside;
        this.name = name;
        this.owner = owner;
        this.progress = progress;
        this.isLocked = locked;
        this.nextObjective = nextObj;
        this.isContested = contested;
        this.capturingTeam = capturingTeam;
        this.captureRate = captureRate;
    }

    public static void encode(PacketSyncPoint msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isInside);
        buf.m_130070_(msg.name);
        buf.m_130070_(msg.owner);
        buf.writeFloat(msg.progress);
        buf.writeBoolean(msg.isLocked);
        buf.m_130070_(msg.nextObjective);
        buf.writeBoolean(msg.isContested);
        buf.m_130070_(msg.capturingTeam);
        buf.writeInt(msg.captureRate);
    }

    public static PacketSyncPoint decode(FriendlyByteBuf buf) {
        return new PacketSyncPoint(buf.readBoolean(), buf.m_130277_(), buf.m_130277_(), buf.readFloat(), buf.readBoolean(), buf.m_130277_(), buf.readBoolean(), buf.m_130277_(), buf.readInt());
    }

    public static void handle(PacketSyncPoint msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.handleSyncPoint(msg)));
        ctx.get().setPacketHandled(true);
    }
}

