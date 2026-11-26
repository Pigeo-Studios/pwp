package com.pwp.coreserver.community;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.pwp.api.core.CoreApi;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.util.RequestGuard;
import com.pwp.api.util.ServerThread;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Команды публичного сервера (этап 10). Основной путь — GUI (пауза → «Пожаловаться», «Привязать Discord»;
 * F6), команды — фолбэк и для консоли:
 *
 * <ul>
 *   <li>{@code /report <ник> <причина> [подробности]} — жалоба
 *   <li>{@code /a <текст>} — админ-чат: видят только модераторы и выше на этом сервере
 *   <li>{@code /link} — код привязки Discord (в Discord: {@code /link <код>})
 *   <li>{@code /pwpchat reload} — перечитать фильтр чата (op 2+)
 * </ul>
 */
public final class CommunityCommands {
    private static final RequestGuard LINK_GUARD = new RequestGuard(10_000);

    private CommunityCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("report")
                .then(Commands.argument("player", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(
                                ctx.getSource().getServer().getPlayerNames(), b))
                        .then(Commands.argument("reason", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(ReportService.REASONS, b))
                                .executes(ctx -> report(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "reason"),
                                        ""))
                                .then(Commands.argument("details", StringArgumentType.greedyString())
                                        .executes(ctx -> report(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                StringArgumentType.getString(ctx, "reason"),
                                                StringArgumentType.getString(ctx, "details")))))));

        d.register(Commands.literal("a")
                .requires(s -> s.getPlayer() == null || PwpPermissions.has(s.getPlayer(), AccessLevel.MODERATOR))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                        .executes(ctx -> adminChat(ctx.getSource(), StringArgumentType.getString(ctx, "message")))));

        d.register(Commands.literal("link").executes(ctx -> link(ctx.getSource())));

        d.register(Commands.literal("pwpchat")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("reload").executes(ctx -> {
                    int n = ChatGuard.reload();
                    ctx.getSource().sendSuccess(() -> Component.translatable("pwp_core.chat.reloaded", n), true);
                    return n;
                })));
    }

    private static int report(CommandSourceStack src, String target, String reason, String details) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.translatable("pwp_core.report.error.console"));
            return 0;
        }
        String err = ReportService.report(p, target, reason.toLowerCase(java.util.Locale.ROOT), clip(details));
        if (err != null) {
            src.sendFailure(Component.translatable(err));
            return 0;
        }
        return 1;
    }

    private static int adminChat(CommandSourceStack src, String message) {
        String name = src.getTextName();
        String text = clip(message);
        MutableComponent line = Component.translatable("pwp_core.chat.admin_prefix")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal(" " + name + ": ").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(text).withStyle(ChatFormatting.GOLD));
        for (ServerPlayer p : src.getServer().getPlayerList().getPlayers()) {
            if (PwpPermissions.has(p, AccessLevel.MODERATOR)) p.sendSystemMessage(line);
        }
        src.getServer().sendSystemMessage(line);
        return 1;
    }

    private static int link(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        boolean started = ServerThread.guardedForPlayer(
                LINK_GUARD, "discord.link", p, () -> CoreApi.discordCode(p.getStringUUID()), (online, resp) -> {
                    if (!CoreApi.isSuccess(resp) || !resp.has("data")) {
                        online.sendSystemMessage(
                                Component.translatable("pwp_core.link.error").withStyle(ChatFormatting.RED));
                        return;
                    }
                    String code = resp.getAsJsonObject("data").get("code").getAsString();
                    online.sendSystemMessage(Component.translatable(
                                    "pwp_core.link.code",
                                    Component.literal(code).withStyle(s -> s.withColor(ChatFormatting.AQUA)
                                            .withBold(true)
                                            .withClickEvent(new ClickEvent(
                                                    ClickEvent.Action.COPY_TO_CLIPBOARD, "/link " + code))
                                            .withHoverEvent(new HoverEvent(
                                                    HoverEvent.Action.SHOW_TEXT,
                                                    Component.translatable("pwp_core.link.copy")))))
                            .withStyle(ChatFormatting.GREEN));
                });
        if (!started)
            p.sendSystemMessage(Component.translatable("pwp_core.link.wait").withStyle(ChatFormatting.YELLOW));
        return 1;
    }

    private static String clip(String s) {
        String t = s == null ? "" : s.replaceAll("\\p{Cntrl}", " ").trim();
        return t.length() > 200 ? t.substring(0, 200) : t;
    }
}
