package com.pwp.coreclient.gui.hud;

import com.mojang.blaze3d.platform.InputConstants;
import com.pwp.coreclient.CoreClientMod;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

/**
 * Регистрация клавиши [TAB] — «Открыть лобби-меню».
 * Само открытие экрана выполняется в LobbyHudEvents.onClientTick
 * (FORGE-шина), здесь — только регистрация KeyMapping (MOD-шина).
 */
@EventBusSubscriber(modid = CoreClientMod.MODID, value = Dist.CLIENT, bus = Bus.MOD)
public class LobbyKeyMappings {

    public static final KeyMapping OPEN_LOBBY_MENU_KEY = new KeyMapping(
            "key.pwp_lobby.open_menu",
            KeyConflictContext.UNIVERSAL,
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_TAB,
            "key.categories.pwp");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_LOBBY_MENU_KEY);
    }
}
