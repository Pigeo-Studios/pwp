package org.acivllas;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class ScreenCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("screen")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("target", StringArgumentType.string())
                        .executes(ctx -> {
                            String targetName = StringArgumentType.getString(ctx, "target");
                            ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(targetName);
                            if (target != null) {
                                NetworkHandler.CHANNEL.send(
                                        PacketDistributor.PLAYER.with(() -> target),
                                        new NetworkHandler.S2CTriggerScreenshotPacket()
                                );
                                ctx.getSource().sendSuccess(
                                        () -> Component.literal("Requested screenshot from " + targetName), true
                                );
                            } else {
                                ctx.getSource().sendFailure(
                                        Component.literal("Player " + targetName + " not found")
                                );
                            }
                            return 1;
                        })
                )
        );
    }
}
