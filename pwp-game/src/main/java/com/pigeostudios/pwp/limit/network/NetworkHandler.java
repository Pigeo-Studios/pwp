package com.pigeostudios.pwp.limit.network;

import net.minecraftforge.network.simple.SimpleChannel;

/**
 * Синхронизация SERVER-конфига лимитов клиенту. С 25.11.2026 (pwp-game) — пакет в конце канала
 * {@code pwpwarfare:main} (бывший канал {@code pwplimit:network} удалён).
 */
public final class NetworkHandler {
    public static final SimpleChannel INSTANCE = com.pigeostudios.pwp.warfare.network.PacketHandler.INSTANCE;

    private NetworkHandler() {}

    /** Регистрирует пакеты лимитов начиная с {@code id}; возвращает следующий свободный id. */
    public static int register(SimpleChannel ch, int id) {
        ch.registerMessage(
                id++,
                LimitsConfigPacket.class,
                LimitsConfigPacket::encode,
                LimitsConfigPacket::decode,
                LimitsConfigPacket::handle);
        return id;
    }
}
