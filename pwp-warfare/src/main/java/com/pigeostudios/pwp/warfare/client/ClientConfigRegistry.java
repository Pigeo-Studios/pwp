package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.gui.WarfareConfigScreen;
import net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory;
import net.minecraftforge.fml.ModLoadingContext;

// Регистрация экрана конфигурации мода в главном меню Forge
public class ClientConfigRegistry {
   public static void registerConfigScreen() {
      ModLoadingContext.get().registerExtensionPoint(ConfigScreenFactory.class, () -> new ConfigScreenFactory((mc, parent) -> new WarfareConfigScreen(parent)));
   }
}
