/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.DistExecutor
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.client.ClientHooks;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PacketOpenPlayerKitMenu {
    public final List<KitDTO> kits;

    public PacketOpenPlayerKitMenu(List<KitDTO> kits) {
        this.kits = kits;
    }

    public static void encode(PacketOpenPlayerKitMenu msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.kits.size());
        for (KitDTO k : msg.kits) {
            buf.m_130070_(k.name);
            buf.writeBoolean(k.available);
            buf.m_130070_(k.reason);
            buf.writeInt(k.items.size());
            for (ItemStack stack : k.items) {
                buf.m_130055_(stack);
            }
        }
    }

    public static PacketOpenPlayerKitMenu decode(FriendlyByteBuf buf) {
        int size = buf.readInt();
        ArrayList<KitDTO> list = new ArrayList<KitDTO>();
        for (int i = 0; i < size; ++i) {
            String name = buf.m_130277_();
            boolean avail = buf.readBoolean();
            String reason = buf.m_130277_();
            int itemCount = buf.readInt();
            ArrayList<ItemStack> items = new ArrayList<ItemStack>();
            for (int j = 0; j < itemCount; ++j) {
                items.add(buf.m_130267_());
            }
            list.add(new KitDTO(name, avail, reason, items));
        }
        return new PacketOpenPlayerKitMenu(list);
    }

    public static void handle(PacketOpenPlayerKitMenu msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> () -> ClientHooks.openPlayerKitMenu(msg.kits)));
        ctx.get().setPacketHandled(true);
    }

    public static class KitDTO {
        public String name;
        public boolean available;
        public String reason;
        public List<ItemStack> items;

        public KitDTO(String n, boolean a, String r, List<ItemStack> items) {
            this.name = n;
            this.available = a;
            this.reason = r;
            this.items = items;
        }
    }
}

