/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.network.PacketDistributor
 */
package com.example.aas.network;

import com.example.aas.network.PacketCaptureNotification;
import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PacketDebugFill {
    public static void encode(PacketDebugFill msg, FriendlyByteBuf buf) {
    }

    public static PacketDebugFill decode(FriendlyByteBuf buf) {
        return new PacketDebugFill();
    }

    public static void handle(PacketDebugFill msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null && player.m_7500_()) {
                ServerLevel level = player.m_284548_();
                AASWorldData data = AASWorldData.get(level);
                String pTeam = player.m_5647_() != null ? player.m_5647_().m_5758_().toUpperCase() : "BLUE";
                for (AASWorldData.CapturePoint point : data.capturePoints) {
                    if (!point.isInside(player.m_20182_())) continue;
                    String oldOwner = point.owner;
                    point.progress += 0.255f;
                    if (point.progress >= 1.0f) {
                        point.progress = 1.0f;
                        if (!point.owner.equals(pTeam)) {
                            point.owner = pTeam;
                            point.capturingTeam = "NONE";
                            PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketCaptureNotification(point.name, pTeam, false));
                        }
                    }
                    data.m_77762_();
                    PacketHandler.sendToAllClients(level, data);
                    break;
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

