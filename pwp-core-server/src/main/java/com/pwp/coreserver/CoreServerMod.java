package com.pwp.coreserver;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod(CoreServerMod.MODID)
public class CoreServerMod {
    public static final String MODID = "pwpcore";
    public static final Logger log = LoggerFactory.getLogger(CoreServerMod.class);
    public static final String API_BASE = "http://pigeo.asuscomm.com:8080";
    public static final String API_KEY = loadApiKey();

    public static boolean isMatchServer = false;

    public CoreServerMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(new PlayerConnectHandler());
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        log.info("PWP Core Server initialized");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        CoreServerApi.configure(API_BASE, API_KEY);
        log.info("CoreServerApi configured with base: {}", API_BASE);

        // Detect match server by working directory name
        String dirName = Path.of("").toAbsolutePath().getFileName().toString();
        isMatchServer = dirName.startsWith("match_");
        log.info(isMatchServer ? "Match server mode — auth via Core verify" : "Lobby server mode");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        BanCommand.register(event.getDispatcher());
        UnbanCommand.register(event.getDispatcher());
        log.info("Ban/Unban commands registered");
    }

    private static String loadApiKey() {
        String envKey = System.getenv("PWP_API_KEY");
        if (envKey != null && !envKey.isEmpty() && !envKey.equals("pwp_server_key_change_me")) {
            log.info("Using PWP_API_KEY from environment");
            return envKey;
        }
        try {
            var configPath = Path.of("config", "pwpcore.txt");
            if (Files.exists(configPath)) {
                String key = Files.readString(configPath).trim();
                if (!key.isEmpty() && !key.equals("pwp_server_key_change_me")) {
                    log.info("Using API key from config/pwpcore.txt");
                    return key;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to read config/pwpcore.txt: {}", e.getMessage());
        }
        log.error("========================================================");
        log.error("HARDCODED API KEY IN USE! Server -> CoreService calls");
        log.error("will FAIL with 401 Unauthorized.");
        log.error("Set env var PWP_API_KEY to the key from core-service's");
        log.error("config.json api.keys[0] and restart the server.");
        log.error("========================================================");
        return "pwp_server_key_change_me";
    }
}
