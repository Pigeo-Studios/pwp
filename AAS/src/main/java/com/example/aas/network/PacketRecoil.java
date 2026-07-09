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

public class PacketRecoil {
    private final float pitch;
    private final float yaw;

    public PacketRecoil(float pitch, float yaw) {
        this.pitch = pitch;
        this.yaw = yaw;
    }

    public static void encode(PacketRecoil msg, FriendlyByteBuf buf) {
        buf.writeFloat(msg.pitch);
        buf.writeFloat(msg.yaw);
    }

    public static PacketRecoil decode(FriendlyByteBuf buf) {
        return new PacketRecoil(buf.readFloat(), buf.readFloat());
    }

    public static void handle(PacketRecoil msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.handleRecoil(msg.pitch, msg.yaw)));
        ctx.get().setPacketHandled(true);
    }
}

