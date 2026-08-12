package com.pigeostudios.pwp.limit;

import com.pigeostudios.pwp.limit.network.LimitsConfigPacket;
import com.pigeostudios.pwp.limit.network.NetworkHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Главный класс мода PWP: Limits
// Регистрирует серверную конфигурацию, сетевой канал (синк конфига клиенту) и экран настроек на клиенте
@Mod(value = "pwplimit")
public class PWPLimit {
    public static final String MODID = "pwplimit";
    private static final Logger LOGGER = LoggerFactory.getLogger(PWPLimit.class);

    public PWPLimit() {
        // Регистрация серверной конфигурации (кулдаун прыжка и т.д.)
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER, (IConfigSpec) ModConfig.SPEC);
        // Сетевой канал: сервер шлёт клиенту актуальные значения конфига
        NetworkHandler.register();
        // При перезагрузке серверного конфига — переслать новые значения всем игрокам
        FMLJavaModLoadingContext.get().getModEventBus().addListener(PWPLimit::onConfigReload);
        // Загрузка экрана настроек только на клиенте
        DistExecutor.unsafeRunWhenOn((Dist) Dist.CLIENT, () -> ClientRegistration::registerConfigScreen);
        LOGGER.info("PWP: Limits loaded with SERVER config!");
    }

    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == ModConfig.SPEC) {
            LimitsConfigPacket.broadcast();
        }
    }
}
