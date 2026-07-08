/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.blaze3d.platform.InputConstants$Type
 *  net.minecraft.client.KeyMapping
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RegisterKeyMappingsEvent
 *  net.minecraftforge.client.settings.IKeyConflictContext
 *  net.minecraftforge.client.settings.KeyConflictContext
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.example.aas.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="aas", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.MOD)
public class ModKeyBindings {
    public static final KeyMapping DROP_SUPPLY_KEY = new KeyMapping("key.aas.drop_supply", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 88, "key.categories.aas");
    public static final KeyMapping OPEN_SQUAD_MENU_KEY = new KeyMapping("key.aas.open_squad_menu", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 75, "key.categories.aas");
    public static final KeyMapping SHOW_MAP_KEY = new KeyMapping("key.aas.show_map", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, 258, "key.categories.aas");
    public static final KeyMapping SHOW_NICKNAMES_KEY = new KeyMapping("key.aas.show_nicknames", (IKeyConflictContext)KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, 340, "key.categories.aas");
    public static final KeyMapping PLACE_PING_KEY = new KeyMapping("key.aas.place_ping", (IKeyConflictContext)KeyConflictContext.IN_GAME, InputConstants.Type.MOUSE, 2, "key.categories.aas");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(DROP_SUPPLY_KEY);
        event.register(OPEN_SQUAD_MENU_KEY);
        event.register(SHOW_MAP_KEY);
        event.register(SHOW_NICKNAMES_KEY);
        event.register(PLACE_PING_KEY);
    }
}

