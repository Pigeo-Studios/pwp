package com.pigeostudios.pwp.warfare.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Кэш текстур pwpwarfare для путей, собираемых в кадре (иконки маркеров/флагов по типу).
 * Раньше {@code new ResourceLocation(...)} создавался на каждый маркер в каждом кадре карты/HUD.
 * Константные текстуры по-прежнему — {@code static final}.
 */
@OnlyIn(Dist.CLIENT)
public final class Tex {
    private static final Map<String, ResourceLocation> CACHE = new ConcurrentHashMap<>();

    private Tex() {}

    public static ResourceLocation of(String path) {
        return CACHE.computeIfAbsent(path, p -> new ResourceLocation("pwpwarfare", p));
    }
}
