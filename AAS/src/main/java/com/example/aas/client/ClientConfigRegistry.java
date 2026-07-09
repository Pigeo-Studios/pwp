/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.screens.Screen
 *  net.minecraftforge.client.ConfigScreenHandler$ConfigScreenFactory
 *  net.minecraftforge.fml.ModLoadingContext
 */
package com.example.aas.client;

import com.example.aas.client.gui.AASConfigScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;

public class ClientConfigRegistry {
    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> new AASConfigScreen((Screen)parent)));
    }
}

