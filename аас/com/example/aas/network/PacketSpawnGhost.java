/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSpawnGhost {
    public final BlockPos pos;
    public final int blockId;

    public PacketSpawnGhost(BlockPos pos, int blockId) {
        this.pos = pos;
        this.blockId = blockId;
    }

    public static void encode(PacketSpawnGhost msg, FriendlyByteBuf buf) {
        buf.m_130064_(msg.pos);
        buf.writeInt(msg.blockId);
    }

    public static PacketSpawnGhost decode(FriendlyByteBuf buf) {
        return new PacketSpawnGhost(buf.m_130135_(), buf.readInt());
    }

    public static void handle(PacketSpawnGhost msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.handleSpawnGhost(msg)));
        ctx.get().setPacketHandled(true);
    }
}

