package com.pwp.coreserver;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

@Mod(CoreServerMod.MODID)
public class CoreServerMod {
    public static final String MODID = "pwpcore";
    public static final Logger log = LoggerFactory.getLogger(CoreServerMod.class);
    public static final String API_BASE = "http://localhost:8080";
    public static final String API_KEY = loadApiKey();

    public static boolean isMatchServer = false;

    public CoreServerMod() {
        if (FMLEnvironment.dist.isDedicatedServer()) {
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::commonSetup);
            MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
            log.info("PWP Core Server initialized");
        } else {
            log.info("PWP Core Server loaded on client — skipping server setup");
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        if (API_KEY.isEmpty()) {
            log.error("PWP Core Server cannot start: No API key configured.");
            log.error("Set PWP_API_KEY environment variable or create config/pwpcore.txt");
            return;
        }
        CoreServerApi.configure(API_BASE, API_KEY);
        log.info("CoreServerApi configured with base: {}", API_BASE);

        // Register dummy channels for client-only mods to satisfy Forge handshake
        NetworkRegistry.newSimpleChannel(
            new ResourceLocation("pwp_core_client", "network"),
            () -> "1", "1"::equals, "1"::equals
        );
        NetworkRegistry.newSimpleChannel(
            new ResourceLocation("pwp_cosmetics", "main"),
            () -> "1", "1"::equals, "1"::equals
        );
        log.info("Registered dummy channels for client-only mods");

        String dirName = Path.of("").toAbsolutePath().getFileName().toString();
        isMatchServer = dirName.startsWith("match_");
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
        if (FMLEnvironment.dist.isDedicatedServer()) {
            throw new RuntimeException(
                "PWP Core Server cannot start: No API key configured.\n" +
                "Set the PWP_API_KEY environment variable or create config/pwpcore.txt\n" +
                "with the API key from core-service's config.json api.keys[0]."
            );
        }
        return "";
    }
}
