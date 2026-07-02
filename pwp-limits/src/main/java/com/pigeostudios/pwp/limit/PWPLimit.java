package com.pigeostudios.pwp.limit;

import com.pigeostudios.pwp.limit.ClientRegistration;
import com.pigeostudios.pwp.limit.ModConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
// Главный класс мода PWP: Limits
// Регистрирует серверную конфигурацию и экран настроек на клиенте
@Mod(value="pwplimit")
public class PWPLimit {
    public static final String MODID = "pwplimit";

    public PWPLimit() {
        // Регистрация серверной конфигурации (кулдаун прыжка и т.д.)
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, (IConfigSpec)ModConfig.SPEC);
        // Загрузка экрана настроек только на клиенте
        DistExecutor.unsafeRunWhenOn((Dist)Dist.CLIENT, () -> ClientRegistration::registerConfigScreen);
        System.out.println("PWP: Limits loaded with SERVER config!");
    }
}
