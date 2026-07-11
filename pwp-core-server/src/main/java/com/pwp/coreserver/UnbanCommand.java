package com.pwp.coreserver;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
                    ctx.getSource().getServer().execute(() -> {
                        new Thread(() -> {
                            try {
                                JsonObject body = new JsonObject();
                                body.addProperty("target", target);
                                String result = BanCommand.postJson("/api/v1/launcher/unban", body.toString());
                                JsonObject json = JsonParser.parseString(result).getAsJsonObject();
                                ctx.getSource().sendSuccess(() ->
                                    Component.literal(json.has("error") ? "\u00A7c" + json.get("error").getAsString()
                                        : "\u00A7a" + target + " unbanned"),
                                    true);
                            } catch (Exception e) {
                                ctx.getSource().sendFailure(Component.literal("\u00A7cUnban failed: " + e.getMessage()));
                            }
                        }, "PWP-Unban").start();
                    });
                    return 1;
                })
            )
        );
    }
}
