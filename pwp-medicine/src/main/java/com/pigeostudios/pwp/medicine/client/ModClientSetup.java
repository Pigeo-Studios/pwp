package com.pigeostudios.pwp.medicine.client;

import com.pigeostudios.pwp.medicine.client.gui.PWPConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

// Настройка клиентской части мода.
// Регистрирует экран конфигурации мода в меню Forge.
public class ModClientSetup {
    public static void registerConfigScreen() {
        // Добавляет фабрику экрана настроек, чтобы он был доступен из главного меню Forge
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parentScreen) -> new PWPConfigScreen((Screen)parentScreen)));
    }
}
