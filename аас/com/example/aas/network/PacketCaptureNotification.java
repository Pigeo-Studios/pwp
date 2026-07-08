/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.client.ClientData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class PacketCaptureNotification {
    private final String pointName;
    private final String team;
    private final boolean neutralized;

    public PacketCaptureNotification(String name, String team, boolean neut) {
        this.pointName = name;
        this.team = team;
        this.neutralized = neut;
    }

    public static void encode(PacketCaptureNotification msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.pointName);
        buf.m_130070_(msg.team);
        buf.writeBoolean(msg.neutralized);
    }

    public static PacketCaptureNotification decode(FriendlyByteBuf buf) {
        return new PacketCaptureNotification(buf.m_130277_(), buf.m_130277_(), buf.readBoolean());
    }

    public static void handle(PacketCaptureNotification msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> ClientData.captureNotifications.add(new ClientData.CaptureNotification(msg.pointName, msg.team, msg.neutralized)));
        ctx.get().setPacketHandled(true);
    }
}

