package com.pigeostudios.pwp.warfare.command;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

@EventBusSubscriber(modid = "pwpwarfare", bus = Bus.FORGE, value = Dist.CLIENT)
// Клиентские команды мода
// Регистрирует команду /compass scale для настройки размера компаса на HUD
public class ClientCommands {
   @SubscribeEvent
   public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
      event.getDispatcher()
         .register(
            (LiteralArgumentBuilder)Commands.literal("compass")
               .then(
                  Commands.literal("scale")
                     .then(
                        Commands.argument("value", IntegerArgumentType.integer(1, 3))
                           .executes(
                              context -> {
                                 int val = IntegerArgumentType.getInteger(context, "value");
                                 WarfareConfig.COMPASS_SCALE.set(val);
                                 WarfareConfig.CLIENT_SPEC.save();
                                 String sizeText = val == 1 ? "Small" : (val == 3 ? "Large" : "Normal");
                                 ((CommandSourceStack)context.getSource())
                                    .sendSuccess(() -> Component.literal("Compass scale saved: " + sizeText).withStyle(ChatFormatting.GREEN), false);
                                 return 1;
                              }
                           )
                     )
               )
         );
   }
}
