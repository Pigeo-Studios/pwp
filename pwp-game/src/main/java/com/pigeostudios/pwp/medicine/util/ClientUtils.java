package com.pigeostudios.pwp.medicine.util;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

// Утилиты для клиентской стороны.
// Позволяет безопасно получить игрока из клиентского потока.
public class ClientUtils {
    // Возвращает текущего клиентского игрока (только на клиенте)
    public static Player getClientPlayer() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            return Minecraft.getInstance().player;
        }
        return null;
    }
}
