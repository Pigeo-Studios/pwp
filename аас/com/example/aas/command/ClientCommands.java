/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.arguments.ArgumentType
 *  com.mojang.brigadier.arguments.IntegerArgumentType
 *  com.mojang.brigadier.builder.LiteralArgumentBuilder
 *  com.mojang.brigadier.context.CommandContext
 *  net.minecraft.ChatFormatting
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraft.commands.Commands
 *  net.minecraft.network.chat.Component
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.client.event.RegisterClientCommandsEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 */
package com.example.aas.command;

import com.example.aas.config.AASConfig;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid="aas", bus=Mod.EventBusSubscriber.Bus.FORGE, value={Dist.CLIENT})
public class ClientCommands {
    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register((LiteralArgumentBuilder)Commands.m_82127_((String)"compass").then(Commands.m_82127_((String)"scale").then(Commands.m_82129_((String)"value", (ArgumentType)IntegerArgumentType.integer((int)1, (int)3)).executes(context -> {
            int val = IntegerArgumentType.getInteger((CommandContext)context, (String)"value");
            AASConfig.COMPASS_SCALE.set((Object)val);
            AASConfig.CLIENT_SPEC.save();
            String sizeText = val == 1 ? "Small" : (val == 3 ? "Large" : "Normal");
            ((CommandSourceStack)context.getSource()).m_288197_(() -> Component.m_237113_((String)("Compass scale saved: " + sizeText)).m_130940_(ChatFormatting.GREEN), false);
            return 1;
        }))));
    }
}

