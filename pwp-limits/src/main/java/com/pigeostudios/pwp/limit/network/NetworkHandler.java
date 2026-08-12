package com.pigeostudios.pwp.limit.network;

import com.pigeostudios.pwp.limit.PWPLimit;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

// Регистрация сетевого канала мода: сервер шлёт клиенту реальные значения конфига
// (Forge не синхронизирует SERVER-конфиги по сети, поэтому без пакета клиент
// в мультиплеере всегда работал бы с локальными дефолтами).
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(PWPLimit.MODID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int id = 0;

    public static void register() {
        INSTANCE.registerMessage(id++, LimitsConfigPacket.class,
                LimitsConfigPacket::encode,
                LimitsConfigPacket::decode,
                LimitsConfigPacket::handle);
    }
}
