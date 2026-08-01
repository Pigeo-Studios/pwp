package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.PathCache;
import com.pigeostudios.pwp.warfare.server.PathManager;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PacketRemovePath {
    public final UUID pathId;

    public PacketRemovePath(UUID pathId) { this.pathId = pathId; }

    public static void encode(PacketRemovePath msg, FriendlyByteBuf buf) { buf.writeUUID(msg.pathId); }
    public static PacketRemovePath decode(FriendlyByteBuf buf) { return new PacketRemovePath(buf.readUUID()); }

    public static void handle(PacketRemovePath msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            if (ctx.get().getDirection().getReceptionSide().isServer()) {
                ServerPlayer p = ctx.get().getSender();
                if (p == null) return;
                var stored = PathManager.findById(msg.pathId);
                if (stored == null) return;
                String pName = p.getScoreboardName();
                boolean isCMD = PathManager.isTeamCMD(p, stored.team());
                boolean isOwner = stored.owner().equals(pName);
                if (!isOwner && !isCMD) return;
                PathManager.removeById(msg.pathId);
                PathManager.broadcastRemovalToTeam(p, msg.pathId);
            } else {
                PathCache.serverPaths.remove(msg.pathId);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
