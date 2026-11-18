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

    // этап 4: бывшие захардкоженные клавиши — теперь переназначаемые (дефолты прежние)
    public static final KeyMapping OPEN_MAP_KEY = new KeyMapping(
            "key.pwpwarfare.open_map", KeyConflictContext.IN_GAME, Type.KEYSYM, 77, "key.categories.pwpwarfare");
    public static final KeyMapping CMD_VOTE_YES_KEY = new KeyMapping(
            "key.pwpwarfare.cmd_vote_yes", KeyConflictContext.IN_GAME, Type.KEYSYM, 296, "key.categories.pwpwarfare");
    public static final KeyMapping CMD_VOTE_NO_KEY = new KeyMapping(
            "key.pwpwarfare.cmd_vote_no", KeyConflictContext.IN_GAME, Type.KEYSYM, 297, "key.categories.pwpwarfare");
    public static final KeyMapping READY_YES_KEY = new KeyMapping(
            "key.pwpwarfare.ready_yes", KeyConflictContext.IN_GAME, Type.KEYSYM, 298, "key.categories.pwpwarfare");
    public static final KeyMapping READY_NO_KEY = new KeyMapping(
            "key.pwpwarfare.ready_no", KeyConflictContext.IN_GAME, Type.KEYSYM, 299, "key.categories.pwpwarfare");
    public static final KeyMapping ART_CONFIRM_KEY = new KeyMapping(
            "key.pwpwarfare.art_confirm", KeyConflictContext.IN_GAME, Type.KEYSYM, 266, "key.categories.pwpwarfare");
    public static final KeyMapping ART_DENY_KEY = new KeyMapping(
            "key.pwpwarfare.art_deny", KeyConflictContext.IN_GAME, Type.KEYSYM, 267, "key.categories.pwpwarfare");
    /**
     * Отладка «заполнить» (креатив, PacketDebugFill). Раньше висела на F9 вместе с «готов» — конфликт;
     * теперь по умолчанию НЕ назначена (назначить в «Управление», право проверяет сервер).
     */
    public static final KeyMapping DEBUG_FILL_KEY = new KeyMapping(
            "key.pwpwarfare.debug_fill",
            KeyConflictContext.IN_GAME,
            Type.KEYSYM,
            com.mojang.blaze3d.platform.InputConstants.UNKNOWN.getValue(),
            "key.categories.pwpwarfare");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MAP_KEY);
        event.register(CMD_VOTE_YES_KEY);
        event.register(CMD_VOTE_NO_KEY);
        event.register(READY_YES_KEY);
        event.register(READY_NO_KEY);
        event.register(ART_CONFIRM_KEY);
        event.register(ART_DENY_KEY);
        event.register(DEBUG_FILL_KEY);
        event.register(DROP_SUPPLY_KEY);
        event.register(OPEN_SQUAD_MENU_KEY);
        event.register(SHOW_MAP_KEY);
        event.register(SHOW_NICKNAMES_KEY);
        event.register(PLACE_PING_KEY);
        event.register(SQUAD_PTT_KEY);
        event.register(COMMAND_PTT_KEY);
    }
}
