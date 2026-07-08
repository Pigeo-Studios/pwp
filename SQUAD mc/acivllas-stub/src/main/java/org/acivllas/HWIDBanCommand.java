package org.acivllas;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class HWIDBanCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hwidban")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("target", StringArgumentType.string())
                        .executes(ctx -> {
                            String targetName = StringArgumentType.getString(ctx, "target");
                            ServerPlayer target = ctx.getSource().getServer().getPlayerList().getPlayerByName(targetName);
                            if (target != null) {
                                String hwid = HWIDManager.getHWID(targetName);
                                if (hwid != null) {
                                    HWIDManager.blacklistPlayer(targetName, hwid);
                                    target.connection.disconnect(Component.literal("You have been banned!"));
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal(targetName + " has been HWID banned. HWID: " + hwid),
                                            true
                                    );
                                } else {
                                    ctx.getSource().sendFailure(Component.literal("HWID not found for " + targetName + ". Use /processlist first."));
                                }
                            } else {
                                ctx.getSource().sendFailure(Component.literal("Player " + targetName + " not found"));
                            }
                            return 1;
                        })
                )
        );
    }
}
