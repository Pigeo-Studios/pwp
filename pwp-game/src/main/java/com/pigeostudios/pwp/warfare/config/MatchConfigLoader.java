package com.pigeostudios.pwp.warfare.config;

import com.google.gson.Gson;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.config.MatchConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MatchConfigLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchConfigLoader.class);

    public static void load(ServerLevel level) {
        Path path = Path.of("match_config.json");
        if (!Files.exists(path)) {
            LOGGER.warn("match_config.json not found, using defaults");
            return;
        }

        MatchConfig config;
        try {
            config = new Gson().fromJson(Files.readString(path), MatchConfig.class);
        } catch (Exception e) {
            LOGGER.error("Failed to parse match_config.json: {}", e.getMessage());
            return;
        }

        if (config.rules == null) {
            LOGGER.warn("match_config.json missing 'rules' section, skipping GameRules");
            return;
        }

        GameRules gr = level.getGameRules();

        gr.getRule(GameRules.RULE_KEEPINVENTORY).set(config.rules.keepInventory, level.getServer());
        gr.getRule(GameRules.RULE_WEATHER_CYCLE).set(!config.rules.disableWeatherCycle, level.getServer());
        gr.getRule(GameRules.RULE_DAYLIGHT).set(!config.rules.disableDaylightCycle, level.getServer());
        gr.getRule(GameRules.RULE_NATURAL_REGENERATION).set(!config.rules.disableNaturalRegen, level.getServer());
        gr.getRule(GameRules.RULE_DOBLOCKDROPS).set(!config.rules.disableBlockDrops, level.getServer());
        gr.getRule(GameRules.RULE_DOENTITYDROPS).set(!config.rules.disableEntityDrops, level.getServer());
        gr.getRule(GameRules.RULE_DOMOBLOOT).set(!config.rules.disableEntityDrops, level.getServer());
        gr.getRule(GameRules.RULE_DOFIRETICK).set(!config.rules.disableFireSpread, level.getServer());
        gr.getRule(GameRules.RULE_SHOWDEATHMESSAGES).set(config.rules.showDeathMessages, level.getServer());
        gr.getRule(GameRules.RULE_COMMANDBLOCKOUTPUT).set(!config.rules.commandFeedback, level.getServer());
        gr.getRule(GameRules.RULE_SENDCOMMANDFEEDBACK).set(!config.rules.commandFeedback, level.getServer());
        gr.getRule(GameRules.RULE_SPECTATORSGENERATECHUNKS)
                .set(config.rules.spectatorsGenerateChunks, level.getServer());

        WarfareWorldData data = WarfareWorldData.get(level);
        data.match.hideNametags = config.rules.hideNametags;
        // Своё сообщение о добивании (DownedHandler) — по тому же смыслу, что ванильный gamerule выше:
        // showDeathMessages=true → показываем. Раньше флаг присваивался без инверсии, и сообщений не было никогда.
        data.match.hideDeathMessages = !config.rules.showDeathMessages;
        data.match.disableHunger = config.rules.disableHunger;
        data.match.disableNaturalRegen = config.rules.disableNaturalRegen;
        data.match.disableBlockDrops = config.rules.disableBlockDrops;
        data.match.disableEntityDrops = config.rules.disableEntityDrops;
        data.match.disableFireSpread = config.rules.disableFireSpread;
        data.match.disableWeatherCycle = config.rules.disableWeatherCycle;

        LOGGER.info(
                "Applied match config (hideNametags={}, weather={})",
                config.rules.hideNametags,
                !config.rules.disableWeatherCycle);
    }
}
