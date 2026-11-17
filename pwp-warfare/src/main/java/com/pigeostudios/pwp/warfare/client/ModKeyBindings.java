package com.pigeostudios.pwp.warfare.client;

import com.mojang.blaze3d.platform.InputConstants.Type;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.MOD)
// Регистрация клавиш мода: карта, меню отряда, пинг, сброс припасов
public class ModKeyBindings {
    public static final KeyMapping DROP_SUPPLY_KEY = new KeyMapping(
            "key.pwpwarfare.drop_supply", KeyConflictContext.IN_GAME, Type.KEYSYM, 88, "key.categories.pwpwarfare");
    public static final KeyMapping OPEN_SQUAD_MENU_KEY = new KeyMapping(
            "key.pwpwarfare.open_squad_menu", KeyConflictContext.IN_GAME, Type.KEYSYM, 75, "key.categories.pwpwarfare");
    public static final KeyMapping SHOW_MAP_KEY = new KeyMapping(
            "key.pwpwarfare.show_map", KeyConflictContext.IN_GAME, Type.KEYSYM, 258, "key.categories.pwpwarfare");
    public static final KeyMapping SHOW_NICKNAMES_KEY = new KeyMapping(
            "key.pwpwarfare.show_nicknames",
            KeyConflictContext.UNIVERSAL,
            Type.KEYSYM,
            340,
            "key.categories.pwpwarfare");
    public static final KeyMapping PLACE_PING_KEY = new KeyMapping(
            "key.pwpwarfare.place_ping", KeyConflictContext.IN_GAME, Type.MOUSE, 2, "key.categories.pwpwarfare");
    public static final KeyMapping SQUAD_PTT_KEY = new KeyMapping(
            "key.pwpwarfare.squad_ptt", KeyConflictContext.IN_GAME, Type.KEYSYM, 66, "key.categories.pwpwarfare");
    public static final KeyMapping COMMAND_PTT_KEY = new KeyMapping(
            "key.pwpwarfare.command_ptt", KeyConflictContext.IN_GAME, Type.KEYSYM, 71, "key.categories.pwpwarfare");
    // Голосования и запрос арты: раньше — сырые коды GLFW в ClientEvents, подписи на HUD — текстом.
    // Дефолты прежние (F9/F10, F7/F8, PgUp/PgDn), теперь переназначаются и подписываются из KeyMapping.
    public static final KeyMapping VOTE_START_YES_KEY = new KeyMapping(
            "key.pwpwarfare.vote_start_yes", KeyConflictContext.IN_GAME, Type.KEYSYM, 298, "key.categories.pwpwarfare");
    public static final KeyMapping VOTE_START_NO_KEY = new KeyMapping(
            "key.pwpwarfare.vote_start_no", KeyConflictContext.IN_GAME, Type.KEYSYM, 299, "key.categories.pwpwarfare");
    public static final KeyMapping VOTE_CMD_YES_KEY = new KeyMapping(
            "key.pwpwarfare.vote_cmd_yes", KeyConflictContext.IN_GAME, Type.KEYSYM, 296, "key.categories.pwpwarfare");
    public static final KeyMapping VOTE_CMD_NO_KEY = new KeyMapping(
            "key.pwpwarfare.vote_cmd_no", KeyConflictContext.IN_GAME, Type.KEYSYM, 297, "key.categories.pwpwarfare");
    public static final KeyMapping ART_CONFIRM_KEY = new KeyMapping(
            "key.pwpwarfare.art_confirm", KeyConflictContext.IN_GAME, Type.KEYSYM, 266, "key.categories.pwpwarfare");
    public static final KeyMapping ART_DENY_KEY = new KeyMapping(
            "key.pwpwarfare.art_deny", KeyConflictContext.IN_GAME, Type.KEYSYM, 267, "key.categories.pwpwarfare");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(DROP_SUPPLY_KEY);
        event.register(OPEN_SQUAD_MENU_KEY);
        event.register(SHOW_MAP_KEY);
        event.register(SHOW_NICKNAMES_KEY);
        event.register(PLACE_PING_KEY);
        event.register(SQUAD_PTT_KEY);
        event.register(COMMAND_PTT_KEY);
        event.register(VOTE_START_YES_KEY);
        event.register(VOTE_START_NO_KEY);
        event.register(VOTE_CMD_YES_KEY);
        event.register(VOTE_CMD_NO_KEY);
        event.register(ART_CONFIRM_KEY);
        event.register(ART_DENY_KEY);
    }
}
