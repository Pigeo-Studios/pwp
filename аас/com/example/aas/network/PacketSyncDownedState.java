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

public class PacketSyncDownedState {
    private final int entityId;
    private final boolean isDowned;
    private final boolean died;

    public PacketSyncDownedState(int id, boolean downed) {
        this(id, downed, false);
    }

    public PacketSyncDownedState(int id, boolean downed, boolean died) {
        this.entityId = id;
        this.isDowned = downed;
        this.died = died;
    }

    public static void encode(PacketSyncDownedState msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeBoolean(msg.isDowned);
        buf.writeBoolean(msg.died);
    }

    public static PacketSyncDownedState decode(FriendlyByteBuf buf) {
        return new PacketSyncDownedState(buf.readInt(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(PacketSyncDownedState msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.handleDownedState(msg.entityId, msg.isDowned, msg.died)));
        ctx.get().setPacketHandled(true);
    }
}

