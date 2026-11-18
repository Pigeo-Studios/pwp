package com.pigeostudios.pwp.limit;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
// Главный класс мода PWP: Limits
// Регистрирует серверную конфигурацию и экран настроек на клиенте

@Mod(value = "pwplimit")
public class PWPLimit {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(PWPLimit.class);
    public static final String MODID = "pwplimit";

    public PWPLimit() {
        // Регистрация серверной конфигурации (кулдаун прыжка и т.д.)
        ModLoadingContext.get()
                .registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, (IConfigSpec) ModConfig.SPEC);
        // Экрана настроек в меню модов больше нет (18.11.2026): это SERVER-конфиг — до входа на сервер
        // он не загружен (открытие из меню падало), а на клиенте правила сервера всё равно не меняются.
        LOG.info("PWP Limits загружен с серверным конфигом");
    }
}
