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

import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSyncSquads;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PacketSquadMarker {
    private final int x;
    private final int z;
    private final int type;

    public PacketSquadMarker(int x, int z, int type) {
        this.x = x;
        this.z = z;
        this.type = type;
    }

    public static void encode(PacketSquadMarker msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.x);
        buf.writeInt(msg.z);
        buf.writeInt(msg.type);
    }

    public static PacketSquadMarker decode(FriendlyByteBuf buf) {
        return new PacketSquadMarker(buf.readInt(), buf.readInt(), buf.readInt());
    }

    public static void handle(PacketSquadMarker msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();
            AASWorldData data = AASWorldData.get(level);
            long expiry = level.getGameTime() + 6000L;
            String pName = player.getScoreboardName();
            for (AASWorldData.Squad s : data.squads) {
                if (!s.members.contains(pName)) continue;
                if (s.leader.equals(pName)) {
                    if (msg.type == 6) {
                        if (s.rhombusMarkers.size() >= 5) {
                            s.rhombusMarkers.remove(0);
                        }
                        s.rhombusMarkers.add(new AASWorldData.SquadMarker(msg.x, 64, msg.z, 6, expiry, false));
                    } else {
                        s.marker = new AASWorldData.SquadMarker(msg.x, 64, msg.z, msg.type, expiry, false);
                    }
                } else if (s.bravoLeader.equals(pName)) {
                    if (msg.type != 6) {
                        s.bravoMarker = new AASWorldData.SquadMarker(msg.x, 64, msg.z, msg.type, expiry, false);
                    }
                } else {
                    if (!s.charlieLeader.equals(pName)) break;
                    if (msg.type != 6) {
                        s.charlieMarker = new AASWorldData.SquadMarker(msg.x, 64, msg.z, msg.type, expiry, false);
                    }
                }
                data.setDirty();
                PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((ServerLevel)level).dimension()), (Object)new PacketSyncSquads(data.squads));
                break;
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

