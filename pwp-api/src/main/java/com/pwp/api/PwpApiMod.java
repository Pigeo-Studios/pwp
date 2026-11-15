package com.pwp.api;

import net.minecraftforge.fml.common.Mod;

/**
 * PWP API — общий модуль для клиентских и серверных pwp-модов.
 *
 * <p>Содержит асинхронный HTTP-клиент core-service ({@link com.pwp.api.core.CoreApi}),
 * политику запуска матчей, проверку прав и утилиты главного потока сервера.
 * Сам по себе ничего не регистрирует: моды-потребители конфигурируют и используют его.
 */
@Mod(PwpApiMod.MODID)
public class PwpApiMod {
    public static final String MODID = "pwpapi";
}
