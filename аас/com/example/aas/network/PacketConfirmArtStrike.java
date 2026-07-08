/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketConfirmArtStrike {
    private final boolean accept;

    public PacketConfirmArtStrike(boolean accept) {
        this.accept = accept;
    }

    public static void encode(PacketConfirmArtStrike msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.accept);
    }

    public static PacketConfirmArtStrike decode(FriendlyByteBuf buf) {
        return new PacketConfirmArtStrike(buf.readBoolean());
    }

    public static void handle(PacketConfirmArtStrike msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            AASWorldData.ArtStrikeRequest request;
            int teamCmdId;
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.m_284548_();
            AASWorldData data = AASWorldData.get(level);
            String team = player.m_5647_() != null ? player.m_5647_().m_5758_().toUpperCase() : "";
            int mySquadId = player.getPersistentData().m_128451_("AAS_SquadID");
            boolean isSL = player.getPersistentData().m_128471_("AAS_IsSquadLeader");
            int n = teamCmdId = team.equals("BLUE") ? data.blueCMDId : data.redCMDId;
            if (mySquadId != teamCmdId || teamCmdId == -1 || !isSL) {
                return;
            }
            AASWorldData.ArtStrikeRequest artStrikeRequest = request = team.equals("BLUE") ? data.blueArtRequest : data.redArtRequest;
            if (request != null) {
                if (msg.accept) {
                    data.activeStrikes.add(new AASWorldData.ActiveStrike(request.pos, team));
                    level.m_7654_().m_6846_().m_240416_((Component)Component.m_237113_((String)"STRATEGIC: Artillery Strike Confirmed by Commander!").m_130944_(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD}), false);
                }
                if (team.equals("BLUE")) {
                    data.blueArtRequest = null;
                } else {
                    data.redArtRequest = null;
                }
                data.activeMarkers.removeIf(m -> m.type.equals("Artillery Request") && m.team.equals(team));
                data.m_77762_();
                PacketHandler.sendToAllClients(level, data);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

