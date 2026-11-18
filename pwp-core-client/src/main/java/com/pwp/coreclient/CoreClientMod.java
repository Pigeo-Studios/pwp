package com.pwp.coreclient;

import com.pwp.coreclient.network.PacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CoreClientMod.MODID)
public class CoreClientMod {

    public static final String MODID = "pwp_core_client";
    private static final Logger log = LoggerFactory.getLogger(CoreClientMod.class);

    public CoreClientMod() {
        log.error("CORE CLIENT MOD CONSTRUCTOR CALLED");
        log.error("MODID=" + MODID);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientScreenHandler.init();
            // лобби: клавиша «Меню лобби», подсказка в HUD, тост при старте голосования
            com.pwp.coreclient.lobby.LobbyHud.init(
                    FMLJavaModLoadingContext.get().getModEventBus());
            // админ-панель: клавиша F6, состояние «я админ» (adminInfo), реестр вкладок
            com.pwp.coreclient.admin.AdminPanel.init(
                    FMLJavaModLoadingContext.get().getModEventBus());
            // настройки интерфейса: свои регистрируем сразу, файл читаем до открытия экранов
            // (настройки модов, зарегистрированные позже, подхватят значения из файла)
            com.pwp.coreclient.settings.CoreUiSettings.init();
            // подсказки «что дальше» (этап 4): реестр + выключатель в «Интерфейсе», до загрузки файла настроек
            com.pwp.coreclient.hints.Hints.init(FMLJavaModLoadingContext.get().getModEventBus());
            com.pwp.coreclient.hints.CoreHints.register();
            com.pwp.coreclient.settings.UiSettingsFile.load();
            // тест-стенд UI (/pwpui): только -Dpwp.uitest=true или dev-окружение
            com.pwp.coreclient.gui.devtest.UiTestCommand.init();
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        log.error("CORE CLIENT MOD commonSetup CALLED");
        event.enqueueWork(() -> {
            log.error("PACKET HANDLER REGISTRATION START");
            PacketHandler.register();
            log.error("PACKET HANDLER REGISTRATION COMPLETE");
        });
    }
}
