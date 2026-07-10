package com.pigeostudios.pwp.warfare.network;

import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketSendData {
    private final String dataType;
    private final String jsonData;

    public PacketSendData(String dataType, String jsonData) {
        this.dataType = dataType;
        this.jsonData = jsonData;
    }

    public String getDataType() { return dataType; }
    public String getJsonData() { return jsonData; }

    public static void encode(PacketSendData msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.dataType);
        buf.writeUtf(msg.jsonData);
    }

    public static PacketSendData decode(FriendlyByteBuf buf) {
        return new PacketSendData(buf.readUtf(), buf.readUtf(32767));
    }

    public static void handle(PacketSendData msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientDataHandler.handleData(msg.dataType, msg.jsonData);
            });
        });
        ctx.get().setPacketHandled(true);
    }
}
