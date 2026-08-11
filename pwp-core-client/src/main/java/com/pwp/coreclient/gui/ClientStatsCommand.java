package com.pwp.coreclient.gui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.pwp.coreclient.gui.screens.PWPLobbyScreen;

@Mod.EventBusSubscriber(modid = "pwp_core_client", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientStatsCommand {

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("stats")
                .executes(ctx -> {
                    Minecraft.getInstance().setScreen(new StatsScreen());
                    return 1;
                })
        );
        dispatcher.register(Commands.literal("pwp")
                .executes(ctx -> {
                    // Серверный root /pwp конфликтует с модерацией pwp-core-server (оригинальная
                    // команда лобби затирается), поэтому экран открываем локально на клиенте
                    if (!PWPLobbyScreen.isOpen()) {
                        Minecraft.getInstance().setScreen(new PWPLobbyScreen());
                    }
                    return 1;
                })
                .then(Commands.literal("stats")
                        .executes(ctx -> {
                            Minecraft.getInstance().setScreen(new StatsScreen());
                            return 1;
                        }))
        );
    }
}