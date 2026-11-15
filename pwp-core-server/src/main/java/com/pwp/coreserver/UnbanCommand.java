package com.pwp.coreserver;

import com.pwp.api.core.CoreApi;
import com.pwp.api.util.ServerThread;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class UnbanCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pwpunban")
            .requires(s -> s.hasPermission(2))
            .then(Commands.argument("player", StringArgumentType.string())
                .executes(ctx -> {
                    String target = StringArgumentType.getString(ctx, "player");
                    CommandSourceStack source = ctx.getSource();
                    ServerThread.thenOnServer(CoreApi.unbanPlayer(target), source.getServer(), json -> {
                        if (json == null) {
                            source.sendFailure(Component.literal("§cUnban failed: core-service недоступен или отклонил запрос"));
                        } else if (json.has("error")) {
                            source.sendFailure(Component.literal("§c" + json.get("error").getAsString()));
                        } else {
                            source.sendSuccess(() -> Component.literal("§a" + target + " unbanned"), true);
                        }
                    });
                    return 1;
                })
            )
        );
    }
}
