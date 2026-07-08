/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.core.BlockPos
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketPlaceMapMarker {
    private final int x;
    private final int z;
    private final String type;

    public PacketPlaceMapMarker(int x, int z, String type) {
        this.x = x;
        this.z = z;
        this.type = type;
    }

    public static void encode(PacketPlaceMapMarker msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.x);
        buf.writeInt(msg.z);
        buf.m_130070_(msg.type);
    }

    public static PacketPlaceMapMarker decode(FriendlyByteBuf buf) {
        return new PacketPlaceMapMarker(buf.readInt(), buf.readInt(), buf.m_130277_());
    }

    public static void handle(PacketPlaceMapMarker msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null || player.m_5647_() == null) {
                return;
            }
            ServerLevel level = player.m_284548_();
            AASWorldData data = AASWorldData.get(level);
            String team = player.m_5647_().m_5758_().toUpperCase();
            String pName = player.m_6302_();
            if (msg.type.equals("Artillery Request")) {
                AASWorldData.ArtStrikeRequest activeReq;
                if (!player.getPersistentData().m_128471_("AAS_IsSquadLeader") && !player.m_7500_()) {
                    return;
                }
                AASWorldData.ArtStrikeRequest artStrikeRequest = activeReq = team.equals("BLUE") ? data.blueArtRequest : data.redArtRequest;
                if (activeReq != null) {
                    return;
                }
                BlockPos strikePos = new BlockPos(msg.x, 64, msg.z);
                if (team.equals("BLUE")) {
                    data.blueArtRequest = new AASWorldData.ArtStrikeRequest(pName, strikePos);
                } else {
                    data.redArtRequest = new AASWorldData.ArtStrikeRequest(pName, strikePos);
                }
                data.activeMarkers.add(new AASWorldData.MapMarker(strikePos, "Artillery Request", team, level.m_46467_() + 3600L));
                int cmdId = team.equals("BLUE") ? data.blueCMDId : data.redCMDId;
                for (ServerPlayer p : level.m_6907_()) {
                    if (p.m_5647_() == null || !p.m_5647_().m_5758_().toUpperCase().equals(team)) continue;
                    int pSqId = p.getPersistentData().m_128451_("AAS_SquadID");
                    if (!p.getPersistentData().m_128471_("AAS_IsSquadLeader") && (pSqId != cmdId || cmdId == -1)) continue;
                    p.m_213846_((Component)Component.m_237113_((String)("[STRATEGIC] Artillery requested at " + msg.x + ", " + msg.z)).m_130940_(ChatFormatting.GOLD));
                }
                data.m_77762_();
                PacketHandler.sendToAllClients(level, data);
            } else {
                data.activeMarkers.add(new AASWorldData.MapMarker(new BlockPos(msg.x, 64, msg.z), msg.type, team, level.m_46467_() + 3600L));
                data.m_77762_();
                PacketHandler.sendToAllClients(level, data);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

