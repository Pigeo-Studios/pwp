package com.pigeostudios.pwp.limit;

import com.pigeostudios.pwp.limit.client.ConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

// Регистрация экрана конфигурации мода в главном меню Forge
public class ClientRegistration {
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, lastScreen) -> new ConfigScreen((Screen)lastScreen)));
    }
}
