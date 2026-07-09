/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.nbt.ListTag
 *  net.minecraft.nbt.Tag
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.client.ClientData;
import com.example.aas.world.AASWorldData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketSyncSquads {
    private final CompoundTag data;

    public PacketSyncSquads(List<AASWorldData.Squad> squads) {
        this.data = new CompoundTag();
        ListTag list = new ListTag();
        for (AASWorldData.Squad s : squads) {
            list.add((Object)s.save());
        }
        this.data.put("List", (Tag)list);
    }

    public PacketSyncSquads(FriendlyByteBuf buf) {
        this.data = buf.readNbt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNbt(this.data);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> {
            ClientData.clientSquads.clear();
            ListTag list = this.data.getList("List", 10);
            for (int i = 0; i < list.size(); ++i) {
                ClientData.clientSquads.add(AASWorldData.Squad.load(list.getCompound(i)));
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}

