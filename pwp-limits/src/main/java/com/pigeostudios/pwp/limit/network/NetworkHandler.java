package com.pigeostudios.pwp.limit.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** Канал pwplimit:network — синхронизация SERVER-конфига лимитов клиенту (восстановлено из 1.0.3). */
public final class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("pwplimit", "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);
    private static int id = 0;

    private NetworkHandler() {}

    public static void register() {
        INSTANCE.registerMessage(
                id++,
                LimitsConfigPacket.class,
                LimitsConfigPacket::encode,
                LimitsConfigPacket::decode,
                LimitsConfigPacket::handle);
    }
}
