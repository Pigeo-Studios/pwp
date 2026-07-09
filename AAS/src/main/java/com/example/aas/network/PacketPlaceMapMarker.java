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
        buf.writeUtf(msg.type);
    }

    public static PacketPlaceMapMarker decode(FriendlyByteBuf buf) {
        return new PacketPlaceMapMarker(buf.readInt(), buf.readInt(), buf.readUtf());
    }

    public static void handle(PacketPlaceMapMarker msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null || player.getTeam() == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            AASWorldData data = AASWorldData.get(level);
            String team = player.getTeam().getName().toUpperCase();
            String pName = player.getScoreboardName();
            if (msg.type.equals("Artillery Request")) {
                AASWorldData.ArtStrikeRequest activeReq;
                if (!player.getPersistentData().getBoolean("AAS_IsSquadLeader") && !player.isCreative()) {
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
                data.activeMarkers.add(new AASWorldData.MapMarker(strikePos, "Artillery Request", team, level.getGameTime() + 3600L));
                int cmdId = team.equals("BLUE") ? data.blueCMDId : data.redCMDId;
                for (ServerPlayer p : level.players()) {
                    if (p.getTeam() == null || !p.getTeam().getName().toUpperCase().equals(team)) continue;
                    int pSqId = p.getPersistentData().getInt("AAS_SquadID");
                    if (!p.getPersistentData().getBoolean("AAS_IsSquadLeader") && (pSqId != cmdId || cmdId == -1)) continue;
                    p.sendSystemMessage((Component)Component.literal((String)("[STRATEGIC] Artillery requested at " + msg.x + ", " + msg.z)).withStyle(ChatFormatting.GOLD));
                }
                data.setDirty();
                PacketHandler.sendToAllClients(level, data);
            } else {
                data.activeMarkers.add(new AASWorldData.MapMarker(new BlockPos(msg.x, 64, msg.z), msg.type, team, level.getGameTime() + 3600L));
                data.setDirty();
                PacketHandler.sendToAllClients(level, data);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

