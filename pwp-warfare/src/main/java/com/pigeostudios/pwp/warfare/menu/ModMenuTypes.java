package com.pigeostudios.pwp.warfare.menu;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

// Реестр типов меню мода
// Регистрация контейнеров для GUI через DeferredRegister
public class ModMenuTypes {
   public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, "pwpwarfare");
   public static final RegistryObject<MenuType<VehicleSpawnerMenu>> VEHICLE_SPAWNER_MENU = MENUS.register(
      "vehicle_spawner_menu", () -> IForgeMenuType.create(VehicleSpawnerMenu::new)
   );
   public static final RegistryObject<MenuType<KitEditorMenu>> KIT_EDITOR_MENU = MENUS.register(
      "kit_editor_menu", () -> IForgeMenuType.create(KitEditorMenu::new)
   );

   public static void register(IEventBus eventBus) {
      MENUS.register(eventBus);
   }
}
