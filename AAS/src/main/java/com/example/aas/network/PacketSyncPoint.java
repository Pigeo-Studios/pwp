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
    final public boolean isInside;
    final public String name;
    final public String owner;
    final public float progress;
    final public boolean isLocked;
    final public String nextObjective;
    final public boolean isContested;
    final public String capturingTeam;
    final public int captureRate;

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
        buf.writeUtf(msg.name);
        buf.writeUtf(msg.owner);
        buf.writeFloat(msg.progress);
        buf.writeBoolean(msg.isLocked);
        buf.writeUtf(msg.nextObjective);
        buf.writeBoolean(msg.isContested);
        buf.writeUtf(msg.capturingTeam);
        buf.writeInt(msg.captureRate);
    }

    public static PacketSyncPoint decode(FriendlyByteBuf buf) {
        return new PacketSyncPoint(buf.readBoolean(), buf.readUtf(), buf.readUtf(), buf.readFloat(), buf.readBoolean(), buf.readUtf(), buf.readBoolean(), buf.readUtf(), buf.readInt());
    }

    public static void handle(PacketSyncPoint msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.handleSyncPoint(msg)));
        ctx.get().setPacketHandled(true);
    }
}

