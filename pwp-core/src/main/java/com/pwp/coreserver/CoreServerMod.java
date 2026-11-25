package com.pwp.coreserver;

import com.pwp.api.core.CoreApi;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Серверная часть pwp-core: ключ CoreApi, баны (вызывается из {@link com.pwp.api.PwpCoreMod}). */
public class CoreServerMod {
    public static final String MODID = "pwpcore";
    public static final Logger log = LoggerFactory.getLogger(CoreServerMod.class);
    // 127.0.0.1, не localhost (localhost -> ::1 в Java на Windows; инцидент 18.11.2026 — лобби не видело core)
    public static final String API_BASE = "http://127.0.0.1:8080";

    public static boolean isMatchServer = false;

    public CoreServerMod() {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
            MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
            new BanEnforcer();
            log.info("PWP Core Server initialized");
        } else {
            log.info("PWP Core Server loaded on client — skipping server setup");
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Ключ читается ТОЛЬКО на выделенном сервере (commonSetup регистрируется только там):
        // у клиента ключа нет и быть не должно — весь HTTP к core-service идёт через сервер
        String apiKey = loadApiKey();
        CoreApi.configure(API_BASE, apiKey);
        log.info("CoreApi configured with base: {}", API_BASE);

        isMatchServer = com.pwp.api.ServerRole.isMatchServer();
        log.info(isMatchServer ? "Match server mode" : "Lobby server mode");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        BanCommand.register(event.getDispatcher());
        UnbanCommand.register(event.getDispatcher());
        log.info("Ban/Unban commands registered");
    }

    private static String loadApiKey() {
        String envKey = System.getenv("PWP_API_KEY");
        if (envKey != null && !envKey.isEmpty()) {
            log.info("Using PWP_API_KEY from environment");
            return envKey;
        }
        try {
            var configPath = Path.of("config", "pwpcore.txt");
            if (Files.exists(configPath)) {
                String key = Files.readString(configPath).trim();
                if (!key.isEmpty()) {
                    log.info("Using API key from config/pwpcore.txt");
                    return key;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read config/pwpcore.txt: {}", e.getMessage());
        }
        throw new IllegalStateException("PWP Core Server cannot start: No API key configured. "
                + "Set the PWP_API_KEY environment variable or create config/pwpcore.txt "
                + "with the API key from core-service's config.json api.keys[0].");
    }
}
