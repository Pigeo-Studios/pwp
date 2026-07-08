/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.network.PacketHandler;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketSelectKit {
    public final String kitName;

    public PacketSelectKit(String kitName) {
        this.kitName = kitName;
    }

    public static void encode(PacketSelectKit msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.kitName);
    }

    public static PacketSelectKit decode(FriendlyByteBuf buf) {
        return new PacketSelectKit(buf.m_130277_());
    }

    public static void handle(PacketSelectKit msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player != null) {
                AASWorldData data = AASWorldData.get(player.m_284548_());
                player.getPersistentData().m_128359_("AAS_PendingKit", msg.kitName);
                PacketHandler.sendToAllClients(player.m_284548_(), data);
                if (!data.isGameStarted) {
                    player.m_213846_((Component)Component.m_237113_((String)("\u041a\u043b\u0430\u0441\u0441 " + msg.kitName + " \u0437\u0430\u0431\u0440\u043e\u043d\u0438\u0440\u043e\u0432\u0430\u043d. \u0412\u044b \u043f\u043e\u043b\u0443\u0447\u0438\u0442\u0435 \u0435\u0433\u043e \u043f\u0440\u0438 \u0441\u0442\u0430\u0440\u0442\u0435 \u0438\u0433\u0440\u044b.")).m_130940_(ChatFormatting.YELLOW));
                } else {
                    player.m_213846_((Component)Component.m_237113_((String)("\u041a\u043b\u0430\u0441\u0441 " + msg.kitName + " \u0432\u044b\u0431\u0440\u0430\u043d. \u0412\u043e\u0437\u044c\u043c\u0438\u0442\u0435 \u043a\u0438\u0442 \u0441 \u044f\u0449\u0438\u043a\u0430 \u043d\u0430 \u0431\u0430\u0437\u0435(\u041f\u041a\u041c).")).m_130940_(ChatFormatting.YELLOW));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

