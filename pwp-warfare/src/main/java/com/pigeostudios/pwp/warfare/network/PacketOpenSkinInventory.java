package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketOpenSkinInventory {

    public PacketOpenSkinInventory() {}

    public static void encode(PacketOpenSkinInventory msg, FriendlyByteBuf buf) {}

    public static PacketOpenSkinInventory decode(FriendlyByteBuf buf) {
        return new PacketOpenSkinInventory();
    }

    public static void handle(PacketOpenSkinInventory msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openSkinInventory));
        ctx.get().setPacketHandled(true);
    }
}

