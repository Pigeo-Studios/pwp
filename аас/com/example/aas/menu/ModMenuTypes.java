/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraftforge.common.extensions.IForgeMenuType
 *  net.minecraftforge.eventbus.api.IEventBus
 *  net.minecraftforge.registries.DeferredRegister
 *  net.minecraftforge.registries.ForgeRegistries
 *  net.minecraftforge.registries.IForgeRegistry
 *  net.minecraftforge.registries.RegistryObject
 */
package com.example.aas.menu;

import com.example.aas.menu.KitEditorMenu;
import com.example.aas.menu.VehicleSpawnerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create((IForgeRegistry)ForgeRegistries.MENU_TYPES, (String)"aas");
    public static final RegistryObject<MenuType<VehicleSpawnerMenu>> VEHICLE_SPAWNER_MENU = MENUS.register("vehicle_spawner_menu", () -> IForgeMenuType.create(VehicleSpawnerMenu::new));
    public static final RegistryObject<MenuType<KitEditorMenu>> KIT_EDITOR_MENU = MENUS.register("kit_editor_menu", () -> IForgeMenuType.create(KitEditorMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}

