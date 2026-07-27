package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.MarkerClientCache;
import com.pigeostudios.pwp.warfare.server.MarkerManager;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketRemoveMarker {
    private final UUID id;
    public PacketRemoveMarker(UUID id) { this.id = id; }

    public static void encode(PacketRemoveMarker msg, FriendlyByteBuf buf) { buf.writeUUID(msg.id); }
    public static PacketRemoveMarker decode(FriendlyByteBuf buf) { return new PacketRemoveMarker(buf.readUUID()); }

    public static void handle(PacketRemoveMarker msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                ServerPlayer p = ctx.get().getSender();
                if (p == null) return;
                MapMarker marker = MarkerManager.findById(msg.id);
                if (marker == null) return;
                boolean isOwner = p.getUUID().equals(marker.ownerUUID);
                boolean isCMD = false;
                WarfareWorldData data = WarfareWorldData.get(p.serverLevel());
                int cmdSquadId = -1;
                String teamName = p.getTeam() != null ? p.getTeam().getName() : "";
                if (teamName.toUpperCase().contains("BLUE")) cmdSquadId = data.blueCMDId;
                else if (teamName.toUpperCase().contains("RED")) cmdSquadId = data.redCMDId;
                if (cmdSquadId != -1) {
                    String pName = p.getScoreboardName();
                    for (var s : data.squads) {
                        if (s.id == cmdSquadId && s.leader.equals(pName)) { isCMD = true; break; }
                    }
                }
                if (!isOwner && !isCMD) return;
                MarkerManager.removeById(msg.id);
                MarkerManager.broadcastRemoval(msg.id);
            } else {
                MarkerClientCache.remove(msg.id);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
