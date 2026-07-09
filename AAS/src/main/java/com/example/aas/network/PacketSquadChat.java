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
        buf.writeUtf(msg.message);
        buf.writeInt(msg.mode);
    }

    public static PacketSquadChat decode(FriendlyByteBuf buf) {
        return new PacketSquadChat(buf.readUtf(), buf.readInt());
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
                    senderName = sender.getScoreboardName();
                    String string = senderTeam = sender.getTeam() != null ? sender.getTeam().getName() : "NEUTRAL";
                    if (mode != 0) break block5;
                    MutableComponent formattedMessage = Component.literal((String)"[ALL] ").withStyle(ChatFormatting.LIGHT_PURPLE).append((Component)Component.literal((String)(senderName + ": ")).withStyle(ChatFormatting.WHITE)).append((Component)Component.literal((String)message).withStyle(ChatFormatting.LIGHT_PURPLE));
                    sender.server.getPlayerList().broadcastSystemMessage((Component)formattedMessage, false);
                    break block6;
                }
                if (mode != 1) break block7;
                if (senderTeam.equals("NEUTRAL")) {
                    sender.sendSystemMessage((Component)Component.literal((String)"You are not in a team!").withStyle(ChatFormatting.RED));
                    return;
                }
                MutableComponent formattedMessage = Component.literal((String)"[TEAM] ").withStyle(ChatFormatting.BLUE).append((Component)Component.literal((String)(senderName + ": ")).withStyle(ChatFormatting.WHITE)).append((Component)Component.literal((String)message).withStyle(ChatFormatting.BLUE));
                for (ServerPlayer p : sender.server.getPlayerList().getPlayers()) {
                    if (p.getTeam() == null || !p.getTeam().getName().equalsIgnoreCase(senderTeam)) continue;
                    p.sendSystemMessage((Component)formattedMessage);
                }
                break block6;
            }
            if (mode != 2) break block6;
            AASWorldData data = AASWorldData.get(sender.serverLevel().getServer().overworld());
            AASWorldData.Squad playerSquad = null;
            for (AASWorldData.Squad s : data.squads) {
                if (!s.members.contains(senderName)) continue;
                playerSquad = s;
                break;
            }
            if (playerSquad == null) {
                sender.sendSystemMessage((Component)Component.literal((String)"You are not in a squad!").withStyle(ChatFormatting.RED));
                return;
            }
            MutableComponent formattedMessage = Component.literal((String)"[SQUAD] ").withStyle(ChatFormatting.GREEN).append((Component)Component.literal((String)(senderName + ": ")).withStyle(ChatFormatting.WHITE)).append((Component)Component.literal((String)message).withStyle(ChatFormatting.GREEN));
            for (String memberName : playerSquad.members) {
                ServerPlayer member = sender.server.getPlayerList().getPlayerByName(memberName);
                if (member == null) continue;
                member.sendSystemMessage((Component)formattedMessage);
            }
        }
    }
}

