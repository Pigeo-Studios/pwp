/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.NetworkEvent$Context
 *  net.minecraftforge.network.PacketDistributor
 */
package com.example.aas.network;

import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSyncSquads;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PacketPlacePing {
    private final boolean isMoveMarker;

    public PacketPlacePing(boolean isMoveMarker) {
        this.isMoveMarker = isMoveMarker;
    }

    public static void encode(PacketPlacePing msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.isMoveMarker);
    }

    public static PacketPlacePing decode(FriendlyByteBuf buf) {
        return new PacketPlacePing(buf.readBoolean());
    }

    public static void handle(PacketPlacePing msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            AASWorldData data = AASWorldData.get(player.m_284548_());
            String pName = player.m_6302_();
            Vec3 eyePos = player.m_146892_();
            Vec3 reachVec = eyePos.m_82549_(player.m_20154_().m_82490_(300.0));
            BlockHitResult hit = player.m_9236_().m_45547_(new ClipContext(eyePos, reachVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)player));
            if (hit.m_6662_() == HitResult.Type.BLOCK) {
                BlockPos target = hit.m_82425_().m_121945_(hit.m_82434_());
                for (AASWorldData.Squad s : data.squads) {
                    if (!s.members.contains(pName)) continue;
                    long time = player.m_9236_().m_46467_();
                    if (s.leader.equals(pName)) {
                        if (msg.isMoveMarker) {
                            s.marker = new AASWorldData.SquadMarker(target.m_123341_(), target.m_123342_(), target.m_123343_(), 0, time + 12000L, true);
                        } else {
                            s.pingPos = target;
                            s.pingExpiry = time + 400L;
                        }
                    } else if (s.bravoLeader.equals(pName)) {
                        if (msg.isMoveMarker) {
                            s.bravoMarker = new AASWorldData.SquadMarker(target.m_123341_(), target.m_123342_(), target.m_123343_(), 0, time + 12000L, true);
                        } else {
                            s.bravoPingPos = target;
                            s.bravoPingExpiry = time + 400L;
                        }
                    } else {
                        if (!s.charlieLeader.equals(pName)) break;
                        if (msg.isMoveMarker) {
                            s.charlieMarker = new AASWorldData.SquadMarker(target.m_123341_(), target.m_123342_(), target.m_123343_(), 0, time + 12000L, true);
                        } else {
                            s.charliePingPos = target;
                            s.charliePingExpiry = time + 400L;
                        }
                    }
                    data.m_77762_();
                    PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(() -> ((Level)player.m_9236_()).m_46472_()), (Object)new PacketSyncSquads(data.squads));
                    break;
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

