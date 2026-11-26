package com.pwp.coreserver;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pwp.api.admin.AuditTrail;
import com.pwp.api.core.CoreApi;
import com.pwp.api.util.ServerThread;
import com.pwp.coreclient.network.data.AdminAudit;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class BanCommand {
    static final String ACTION = "ban";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pwpban")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("player", StringArgumentType.string())
                        .then(Commands.argument("duration", StringArgumentType.string())
                                .suggests((ctx, b) -> {
                                    b.suggest("30m");
                                    b.suggest("1h");
                                    b.suggest("6h");
                                    b.suggest("12h");
                                    b.suggest("1d");
                                    b.suggest("7d");
                                    b.suggest("30d");
                                    b.suggest("perm");
                                    return b.buildFuture();
                                })
                                .then(Commands.argument("reason", StringArgumentType.greedyString())
                                        .executes(ctx -> ban(
                                                ctx,
                                                StringArgumentType.getString(ctx, "player"),
                                                StringArgumentType.getString(ctx, "duration"),
                                                StringArgumentType.getString(ctx, "reason")))))));
    }

    private static int ban(CommandContext<CommandSourceStack> ctx, String target, String duration, String reason) {
        CommandSourceStack source = ctx.getSource();
        String details = target + " " + duration + " " + reason;
        ServerThread.thenOnServer(CoreApi.banPlayer(target, reason, duration), source.getServer(), json -> {
            if (json == null) {
                source.sendFailure(Component.translatable("pwp_core.ban.failed_core"));
                AdminAudit.recordSource(source, ACTION, details, AuditTrail.Result.FAILED, "core");
            } else if (json.has("error")) {
                String err = json.get("error").getAsString();
                source.sendFailure(Component.translatable("pwp_core.ban.error", err));
                AdminAudit.recordSource(source, ACTION, details, AuditTrail.Result.FAILED, err);
            } else {
                String shown = json.has("duration") ? json.get("duration").getAsString() : duration;
                source.sendSuccess(
                        () -> Component.translatable("pwp_core.ban.done", target, shown)
                                .withStyle(ChatFormatting.GREEN),
                        true);
                AdminAudit.recordSource(source, ACTION, details, AuditTrail.Result.OK, null);
            }
        });
        return 1;
    }
}
