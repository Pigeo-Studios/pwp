package com.pigeostudios.pwp.limit;

import com.pigeostudios.pwp.limit.network.LimitsConfigPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** При входе игрок получает лимиты сервера (клиентский кэш). */
@Mod.EventBusSubscriber(modid = "pwpwarfare")
public final class ServerEvents {
    private ServerEvents() {}

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) LimitsConfigPacket.sendTo(player);
    }
}
