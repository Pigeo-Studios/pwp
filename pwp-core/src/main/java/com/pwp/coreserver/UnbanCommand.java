package com.pwp.coreserver;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pwp.api.admin.AuditTrail;
import com.pwp.api.core.CoreApi;
import com.pwp.api.util.ServerThread;
import com.pwp.coreclient.network.data.AdminAudit;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

public class UnbanCommand {
    static final String ACTION = "unban";

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("pwpunban")
                .requires(s -> s.hasPermission(2))
                .then(Commands.argument("player", StringArgumentType.string()).executes(ctx -> {
                    String target = StringArgumentType.getString(ctx, "player");
                    CommandSourceStack source = ctx.getSource();
                    ServerThread.thenOnServer(CoreApi.unbanPlayer(target), source.getServer(), json -> {
                        if (json == null) {
                            source.sendFailure(Component.translatable("pwp_core.unban.failed_core"));
                            AdminAudit.recordSource(source, ACTION, target, AuditTrail.Result.FAILED, "core");
                        } else if (json.has("error")) {
                            String err = json.get("error").getAsString();
                            source.sendFailure(Component.translatable("pwp_core.unban.error", err));
                            AdminAudit.recordSource(source, ACTION, target, AuditTrail.Result.FAILED, err);
                        } else {
                            source.sendSuccess(
                                    () -> Component.translatable("pwp_core.unban.done", target)
                                            .withStyle(ChatFormatting.GREEN),
                                    true);
                            AdminAudit.recordSource(source, ACTION, target, AuditTrail.Result.OK, null);
                        }
                    });
                    return 1;
                })));
    }
}
