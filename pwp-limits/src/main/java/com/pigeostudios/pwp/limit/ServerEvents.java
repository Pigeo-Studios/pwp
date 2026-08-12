package com.pigeostudios.pwp.limit;

import com.pigeostudios.pwp.limit.network.LimitsConfigPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// Серверные события: при входе игрока сразу шлём ему актуальные значения конфига лимитов
@Mod.EventBusSubscriber(modid = PWPLimit.MODID)
public class ServerEvents {

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer) {
            LimitsConfigPacket.sendTo(serverPlayer);
        }
    }
}
