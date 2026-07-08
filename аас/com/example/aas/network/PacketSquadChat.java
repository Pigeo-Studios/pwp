/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketSquadChat {
    private final String message;
    private final int mode;

    public PacketSquadChat(String message, int mode) {
        this.message = message;
        this.mode = mode;
    }

    public static void encode(PacketSquadChat msg, FriendlyByteBuf buf) {
        buf.m_130070_(msg.message);
        buf.writeInt(msg.mode);
    }

    public static PacketSquadChat decode(FriendlyByteBuf buf) {
        return new PacketSquadChat(buf.m_130277_(), buf.readInt());
    }

    public static void handle(PacketSquadChat msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ((NetworkEvent.Context)ctx.get()).getSender();
            if (sender != null) {
                PacketSquadChat.processChat(sender, msg.message, msg.mode);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void processChat(ServerPlayer sender, String message, int mode) {
        block6: {
            String senderName;
            block7: {
                String senderTeam;
                block5: {
                    senderName = sender.m_6302_();
                    String string = senderTeam = sender.m_5647_() != null ? sender.m_5647_().m_5758_() : "NEUTRAL";
                    if (mode != 0) break block5;
                    MutableComponent formattedMessage = Component.m_237113_((String)"[ALL] ").m_130940_(ChatFormatting.LIGHT_PURPLE).m_7220_((Component)Component.m_237113_((String)(senderName + ": ")).m_130940_(ChatFormatting.WHITE)).m_7220_((Component)Component.m_237113_((String)message).m_130940_(ChatFormatting.LIGHT_PURPLE));
                    sender.f_8924_.m_6846_().m_240416_((Component)formattedMessage, false);
                    break block6;
                }
                if (mode != 1) break block7;
                if (senderTeam.equals("NEUTRAL")) {
                    sender.m_213846_((Component)Component.m_237113_((String)"You are not in a team!").m_130940_(ChatFormatting.RED));
                    return;
                }
                MutableComponent formattedMessage = Component.m_237113_((String)"[TEAM] ").m_130940_(ChatFormatting.BLUE).m_7220_((Component)Component.m_237113_((String)(senderName + ": ")).m_130940_(ChatFormatting.WHITE)).m_7220_((Component)Component.m_237113_((String)message).m_130940_(ChatFormatting.BLUE));
                for (ServerPlayer p : sender.f_8924_.m_6846_().m_11314_()) {
                    if (p.m_5647_() == null || !p.m_5647_().m_5758_().equalsIgnoreCase(senderTeam)) continue;
                    p.m_213846_((Component)formattedMessage);
                }
                break block6;
            }
            if (mode != 2) break block6;
            AASWorldData data = AASWorldData.get(sender.m_284548_().m_7654_().m_129783_());
            AASWorldData.Squad playerSquad = null;
            for (AASWorldData.Squad s : data.squads) {
                if (!s.members.contains(senderName)) continue;
                playerSquad = s;
                break;
            }
            if (playerSquad == null) {
                sender.m_213846_((Component)Component.m_237113_((String)"You are not in a squad!").m_130940_(ChatFormatting.RED));
                return;
            }
            MutableComponent formattedMessage = Component.m_237113_((String)"[SQUAD] ").m_130940_(ChatFormatting.GREEN).m_7220_((Component)Component.m_237113_((String)(senderName + ": ")).m_130940_(ChatFormatting.WHITE)).m_7220_((Component)Component.m_237113_((String)message).m_130940_(ChatFormatting.GREEN));
            for (String memberName : playerSquad.members) {
                ServerPlayer member = sender.f_8924_.m_6846_().m_11255_(memberName);
                if (member == null) continue;
                member.m_213846_((Component)formattedMessage);
            }
        }
    }
}

