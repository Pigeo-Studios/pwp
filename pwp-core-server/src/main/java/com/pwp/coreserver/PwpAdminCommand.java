package com.pwp.coreserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Единый контур модерации /pwp: бан, мут текста, мут войса, warn, kick, история.
 * Все запросы к core-service уходят в отдельном потоке (никогда не блокируем серверный поток).
 * TAB-подсказки для цели берутся из локального кэша (онлайн + забаненные/замученные),
 * который в фоне обновляет демон-поток — бригадир не делает сетевых вызовов.
 */
public class PwpAdminCommand {

    private static final List<String> DURATIONS = List.of("30m", "1h", "6h", "12h", "1d", "7d", "30d", "perm");
    private static final long CACHE_TTL_MS = 15_000L;

    private static volatile List<String> bannedNames = List.of();
    private static volatile List<String> chatMutedNames = List.of();
    private static volatile List<String> voiceMutedNames = List.of();
    private static volatile long bannedAt = 0;
    private static volatile long chatAt = 0;
    private static volatile long voiceAt = 0;
    private static final AtomicBoolean REFRESHER_STARTED = new AtomicBoolean(false);

    private enum ArgKind { ONLINE, BANNED, CHAT_MUTED, VOICE_MUTED }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("pwp")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("ban")
                    .then(targetArg(ArgKind.ONLINE)
                        .then(durationArg()
                            .executes(ctx -> ban(ctx, arg(ctx, "player"), arg(ctx, "duration"), null))
                            .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> ban(ctx, arg(ctx, "player"), arg(ctx, "duration"), arg(ctx, "reason")))))))
                .then(Commands.literal("unban")
                    .then(targetArg(ArgKind.BANNED)
                        .executes(ctx -> unban(ctx, arg(ctx, "player")))))
                .then(Commands.literal("mute")
                    .then(targetArg(ArgKind.ONLINE)
                        .then(durationArg()
                            .executes(ctx -> mute(ctx, arg(ctx, "player"), arg(ctx, "duration"), null))
                            .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> mute(ctx, arg(ctx, "player"), arg(ctx, "duration"), arg(ctx, "reason")))))))
                .then(Commands.literal("unmute")
                    .then(targetArg(ArgKind.CHAT_MUTED)
                        .executes(ctx -> unmute(ctx, arg(ctx, "player")))))
                .then(Commands.literal("mutevoice")
                    .then(targetArg(ArgKind.ONLINE)
                        .then(durationArg()
                            .executes(ctx -> mutevoice(ctx, arg(ctx, "player"), arg(ctx, "duration"), null))
                            .then(Commands.argument("reason", StringArgumentType.greedyString())
                                .executes(ctx -> mutevoice(ctx, arg(ctx, "player"), arg(ctx, "duration"), arg(ctx, "reason")))))))
                .then(Commands.literal("unmutevoice")
                    .then(targetArg(ArgKind.VOICE_MUTED)
                        .executes(ctx -> unmutevoice(ctx, arg(ctx, "player")))))
                .then(Commands.literal("warn")
                    .then(targetArg(ArgKind.ONLINE)
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> warn(ctx, arg(ctx, "player"), arg(ctx, "reason"))))))
                .then(Commands.literal("kick")
                    .then(targetArg(ArgKind.ONLINE)
                        .executes(ctx -> kick(ctx, arg(ctx, "player"), null))
                        .then(Commands.argument("reason", StringArgumentType.greedyString())
                            .executes(ctx -> kick(ctx, arg(ctx, "player"), arg(ctx, "reason"))))))
                .then(Commands.literal("history")
                    .then(targetArg(ArgKind.ONLINE)
                        .executes(ctx -> history(ctx, arg(ctx, "player")))))
                .then(Commands.<CommandSourceStack>literal("mutes").executes(ctx -> mutes(ctx)))
        );
        startCacheRefresher();
    }

    // ====== БАН ======

    private static int ban(CommandContext<CommandSourceStack> ctx, String target, String duration, String reason) {
        CommandSourceStack src = ctx.getSource();
        String adminUuid = adminUuid(src);
        runApi(src, "pwp-ban", () -> CoreServerApi.ban(target, reason, duration, adminUuid), r -> {
            if (isSuccess(r)) {
                String nick = r.has("nickname") ? r.get("nickname").getAsString() : target;
                String dur = r.has("duration") ? r.get("duration").getAsString() : displayDuration(duration);
                ok(src, "\u00A7a" + nick + " забанен (" + dur + ")" + appendReason(reason));
                kickOnline(src, target);
                bannedNames = List.of();
            } else {
                fail(src, r, "Бан не выполнен");
            }
        });
        return 1;
    }

    private static int unban(CommandContext<CommandSourceStack> ctx, String target) {
        CommandSourceStack src = ctx.getSource();
        runApi(src, "pwp-unban", () -> CoreServerApi.unban(target, adminUuid(src)), r -> {
            if (isSuccess(r)) {
                ok(src, "\u00A7a" + target + " разбанен");
                bannedNames = List.of();
            } else {
                fail(src, r, "Разбан не выполнен");
            }
        });
        return 1;
    }

    // ====== МУТ (текст) ======

    private static int mute(CommandContext<CommandSourceStack> ctx, String target, String duration, String reason) {
        CommandSourceStack src = ctx.getSource();
        int minutes = parseMinutes(duration);
        runApi(src, "pwp-mute", () -> CoreServerApi.chatMute(target, adminUuid(src), adminName(src), reason, minutes), r -> {
            if (isSuccess(r)) {
                notifyTarget(src, target, "Ваш текстовый чат отключён администратором " + adminName(src) + " (" + displayDuration(duration) + ")");
                ok(src, "\u00A7aЧат игрока " + target + " отключён" + appendReason(reason) + " " + displayDuration(duration));
                chatMutedNames = List.of();
            } else {
                fail(src, r, "Мут не выполнен");
            }
        });
        return 1;
    }

    private static int unmute(CommandContext<CommandSourceStack> ctx, String target) {
        CommandSourceStack src = ctx.getSource();
        runApi(src, "pwp-unmute", () -> CoreServerApi.chatUnmute(target, adminUuid(src)), r -> {
            if (isSuccess(r)) {
                notifyTarget(src, target, "Ваш текстовый чат снова включён");
                ok(src, "\u00A7aЧат игрока " + target + " включён");
                chatMutedNames = List.of();
            } else {
                fail(src, r, "Снятие мута не выполнено");
            }
        });
        return 1;
    }

    // ====== МУТ (войс) ======

    private static int mutevoice(CommandContext<CommandSourceStack> ctx, String target, String duration, String reason) {
        CommandSourceStack src = ctx.getSource();
        int minutes = parseMinutes(duration);
        runApi(src, "pwp-mutevoice", () -> CoreServerApi.voiceMute(target, adminUuid(src), adminName(src), reason, minutes), r -> {
            if (isSuccess(r)) {
                notifyTarget(src, target, "Ваш голосовой чат отключён администратором " + adminName(src) + " (" + displayDuration(duration) + ")");
                ok(src, "\u00A7aГолосовой чат игрока " + target + " отключён" + appendReason(reason) + " " + displayDuration(duration));
                voiceMutedNames = List.of();
            } else {
                fail(src, r, "Мут не выполнен");
            }
        });
        return 1;
    }

    private static int unmutevoice(CommandContext<CommandSourceStack> ctx, String target) {
        CommandSourceStack src = ctx.getSource();
        runApi(src, "pwp-unmutevoice", () -> CoreServerApi.voiceUnmute(target), r -> {
            if (isSuccess(r)) {
                notifyTarget(src, target, "Ваш голосовой чат снова включён");
                ok(src, "\u00A7aГолосовой чат игрока " + target + " включён");
                voiceMutedNames = List.of();
            } else {
                fail(src, r, "Снятие мута не выполнено");
            }
        });
        return 1;
    }

    // ====== WARN ======

    private static int warn(CommandContext<CommandSourceStack> ctx, String target, String reason) {
        CommandSourceStack src = ctx.getSource();
        runApi(src, "pwp-warn", () -> CoreServerApi.warn(target, reason, adminUuid(src)), r -> {
            if (isSuccess(r)) {
                ServerPlayer p = findPlayer(src, target);
                if (p != null) onScreenWarning(p, reason);
                ok(src, "\u00A7aПредупреждение игроку " + target + " записано" + appendReason(reason));
            } else {
                fail(src, r, "Предупреждение не записано");
            }
        });
        return 1;
    }

    // ====== KICK ======

    private static int kick(CommandContext<CommandSourceStack> ctx, String target, String reason) {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer p = findPlayer(src, target);
        if (p == null) {
            src.sendFailure(Component.literal("\u00A7cИгрок " + target + " не в сети"));
            return 0;
        }
        ServerPlayer kickTarget = p;
        String msg = reason != null && !reason.isEmpty() ? reason : "Вы были кикнуты администратором";
        kickTarget.connection.disconnect(Component.literal("\u00A7c" + msg));
        ok(src, "\u00A7aИгрок " + target + " кикнут" + appendReason(reason));
        runApi(src, "pwp-kick", () -> CoreServerApi.kick(target, reason, adminUuid(src)), r -> {
            if (!isSuccess(r)) {
                fail(src, r, "Запись кика в историю не выполнена");
            }
        });
        return 1;
    }

    // ====== HISTORY / MUTES ======

    private static int history(CommandContext<CommandSourceStack> ctx, String target) {
        CommandSourceStack src = ctx.getSource();
        runApi(src, "pwp-history", () -> CoreServerApi.getPunishments(target), r -> printHistory(src, r));
        return 1;
    }

    private static int mutes(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        runApi(src, "pwp-mutes", () -> {
            JsonObject chat = CoreServerApi.getChatMutes();
            JsonObject voice = CoreServerApi.getVoiceMutes();
            JsonObject out = new JsonObject();
            out.add("chat", chat);
            out.add("voice", voice);
            return out;
        }, r -> printMutes(src, r));
        return 1;
    }

    private static void printHistory(CommandSourceStack src, JsonObject r) {
        if (!isSuccess(r)) {
            fail(src, r, "Не удалось получить историю");
            return;
        }
        JsonArray arr = r.has("data") && !r.get("data").isJsonNull() ? r.getAsJsonArray("data") : new JsonArray();
        if (arr.size() == 0) {
            ok(src, "\u00A7eНаказаний не найдено");
            return;
        }
        src.sendSuccess(() -> Component.literal("=== История наказаний ===").withStyle(ChatFormatting.GOLD), false);
        int shown = 0;
        for (JsonElement e : arr) {
            if (shown++ >= 20) break;
            JsonObject row = e.getAsJsonObject();
            String created = row.has("created_at") && !row.get("created_at").isJsonNull() ? row.get("created_at").getAsString() : "?";
            String type = row.has("type") ? row.get("type").getAsString() : "?";
            String reason = row.has("reason") && !row.get("reason").isJsonNull() && !row.get("reason").getAsString().isEmpty()
                    ? row.get("reason").getAsString() : "—";
            Integer dm = row.has("duration_minutes") && !row.get("duration_minutes").isJsonNull()
                    ? row.get("duration_minutes").getAsInt() : null;
            String dur = dm != null ? formatDuration(dm) : "";
            String line = created.substring(0, Math.min(created.length(), 19)) + " " + type + " " + dur + " — " + reason;
            src.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.WHITE), false);
        }
    }

    private static void printMutes(CommandSourceStack src, JsonObject r) {
        JsonObject chat = r.has("chat") && !r.get("chat").isJsonNull() ? r.getAsJsonObject("chat") : null;
        JsonObject voice = r.has("voice") && !r.get("voice").isJsonNull() ? r.getAsJsonObject("voice") : null;
        src.sendSuccess(() -> Component.literal("=== Текстовые муты (" + count(chat) + ") ===").withStyle(ChatFormatting.GOLD), false);
        printMuteList(src, chat);
        src.sendSuccess(() -> Component.literal("=== Войс-муты (" + count(voice) + ") ===").withStyle(ChatFormatting.GOLD), false);
        printMuteList(src, voice);
    }

    private static void printMuteList(CommandSourceStack src, JsonObject r) {
        JsonArray arr = r != null && r.has("data") && !r.get("data").isJsonNull() ? r.getAsJsonArray("data") : new JsonArray();
        if (arr.size() == 0) {
            src.sendSuccess(() -> Component.literal("Нет активных").withStyle(ChatFormatting.YELLOW), false);
            return;
        }
        for (JsonElement e : arr) {
            JsonObject m = e.getAsJsonObject();
            String name = m.has("nickname") && m.get("nickname") != null ? m.get("nickname").getAsString()
                    : (m.has("uuid") ? m.get("uuid").getAsString().substring(0, 8) : "?");
            String reason = m.has("reason") && !m.get("reason").getAsString().isEmpty() ? m.get("reason").getAsString() : "—";
            boolean perm = m.has("expiresAt") && m.get("expiresAt").getAsLong() == 0;
            String by = m.has("mutedByNickname") ? m.get("mutedByNickname").getAsString() : "?";
            String line = name + " | мут от: " + by + " | " + (perm ? "навсегда" : "временный") + " | " + reason;
            src.sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.WHITE), false);
        }
    }

    private static int count(JsonObject r) {
        if (r == null || !r.has("data") || r.get("data").isJsonNull()) return 0;
        return r.getAsJsonArray("data").size();
    }

    // ====== ХЕЛПЕРЫ ======

    private static String adminUuid(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        return p != null ? p.getStringUUID() : "console";
    }

    private static String adminName(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        return p != null ? p.getScoreboardName() : "Console";
    }

    private static boolean isSuccess(JsonObject r) {
        return r != null && r.has("success") && r.get("success").getAsBoolean();
    }

    private static void runApi(CommandSourceStack src, String threadName, ApiCall call, java.util.function.Consumer<JsonObject> onResult) {
        src.getServer().execute(() -> new Thread(() -> {
            try {
                JsonObject r = call.call();
                src.getServer().execute(() -> onResult.accept(r));
            } catch (Exception e) {
                src.getServer().execute(() -> src.sendFailure(Component.literal("\u00A7cОшибка API: " + e.getMessage())));
            }
        }, threadName).start());
    }

    private interface ApiCall {
        JsonObject call() throws Exception;
    }

    private static void ok(CommandSourceStack src, String text) {
        src.getServer().execute(() -> src.sendSuccess(() -> Component.literal(text), true));
    }

    private static void fail(CommandSourceStack src, JsonObject r, String what) {
        String err = r != null && r.has("error") ? r.get("error").getAsString() : "нет ответа core-service";
        src.sendFailure(Component.literal("\u00A7c" + what + ": " + err));
    }

    private static String appendReason(String reason) {
        return reason != null && !reason.isEmpty() ? " — " + reason : "";
    }

    private static void notifyTarget(CommandSourceStack src, String target, String msg) {
        ServerPlayer p = findPlayer(src, target);
        if (p != null) {
            p.sendSystemMessage(Component.literal("\u00A7c" + msg));
        }
    }

    private static void kickOnline(CommandSourceStack src, String target) {
        ServerPlayer p = findPlayer(src, target);
        if (p != null) {
            p.connection.disconnect(Component.literal("\u00A7cВы забанены администратором"));
        }
    }

    private static ServerPlayer findPlayer(CommandSourceStack src, String nameOrUuid) {
        for (ServerPlayer sp : src.getServer().getPlayerList().getPlayers()) {
            if (sp.getScoreboardName().equalsIgnoreCase(nameOrUuid) || sp.getStringUUID().equals(nameOrUuid)) {
                return sp;
            }
        }
        return null;
    }

    private static void onScreenWarning(ServerPlayer t, String message) {
        t.connection.send(new ClientboundSetTitlesAnimationPacket(10, 140, 20));
        t.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(message).withStyle(ChatFormatting.YELLOW)));
        t.connection.send(new ClientboundSetTitleTextPacket(
            Component.literal("!ВНИМАНИЕ!").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)));
        t.playNotifySound(SoundEvents.ANVIL_LAND, SoundSource.MASTER, 1.0F, 0.8F);
        t.sendSystemMessage(Component.literal("[АДМИН ПРЕДУПРЕЖДЕНИЕ] " + message)
            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
    }

    // ====== АРГУМЕНТЫ / ПОДСКАЗКИ ======

    private static String arg(CommandContext<CommandSourceStack> ctx, String name) {
        return StringArgumentType.getString(ctx, name);
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> targetArg(ArgKind kind) {
        return Commands.argument("player", StringArgumentType.string())
            .suggests((ctx, builder) -> {
                if (kind == ArgKind.ONLINE) {
                    return SharedSuggestionProvider.suggest(ctx.getSource().getOnlinePlayerNames(), builder);
                }
                List<String> names = switch (kind) {
                    case BANNED -> bannedNames;
                    case CHAT_MUTED -> chatMutedNames;
                    case VOICE_MUTED -> voiceMutedNames;
                    default -> List.of();
                };
                return SharedSuggestionProvider.suggest(names, builder);
            });
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> durationArg() {
        return Commands.argument("duration", StringArgumentType.string())
            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(DURATIONS, builder));
    }

    // ====== ДЛИТЕЛЬНОСТЬ ======

    /** "30m", "2h", "3d", "perm", "0", число(часы) → минуты. 0 = навсегда. */
    private static int parseMinutes(String s) {
        if (s == null) return 0;
        String t = s.trim().toLowerCase();
        if (t.isEmpty() || t.equals("perm") || t.equals("0")) return 0;
        try {
            if (t.endsWith("m")) return Math.max(1, Integer.parseInt(t.substring(0, t.length() - 1)));
            if (t.endsWith("h")) return Math.max(1, Integer.parseInt(t.substring(0, t.length() - 1)) * 60);
            if (t.endsWith("d")) return Math.max(1, Integer.parseInt(t.substring(0, t.length() - 1)) * 1440);
            return Math.max(1, Integer.parseInt(t) * 60);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static String displayDuration(String s) {
        int minutes = parseMinutes(s);
        if (minutes <= 0) return "навсегда";
        return formatDuration(minutes);
    }

    private static String formatDuration(int minutes) {
        if (minutes < 60) return minutes + " мин";
        if (minutes < 1440) return (minutes / 60) + " ч";
        return (minutes / 1440) + " дн";
    }

    // ====== КЭШ ДЛЯ TAB-ПОДСКАЗОК ======

    private static void startCacheRefresher() {
        if (REFRESHER_STARTED.compareAndSet(false, true)) {
            Thread t = new Thread(() -> {
                while (true) {
                    try {
                        refreshCaches();
                    } catch (Throwable ignored) {}
                    try {
                        Thread.sleep(10_000);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }, "pwp-modcache");
            t.setDaemon(true);
            t.start();
        }
    }

    private static void refreshCaches() {
        long now = System.currentTimeMillis();
        if (now - bannedAt > CACHE_TTL_MS) {
            JsonObject r = CoreServerApi.getBannedPlayers();
            if (r != null && r.has("data") && !r.get("data").isJsonNull()) {
                bannedNames = extractNames(r.getAsJsonArray("data"));
                bannedAt = now;
            }
        }
        if (now - chatAt > CACHE_TTL_MS) {
            JsonObject r = CoreServerApi.getChatMutes();
            if (r != null && r.has("data") && !r.get("data").isJsonNull()) {
                chatMutedNames = extractNames(r.getAsJsonArray("data"));
                chatAt = now;
            }
        }
        if (now - voiceAt > CACHE_TTL_MS) {
            JsonObject r = CoreServerApi.getVoiceMutes();
            if (r != null && r.has("data") && !r.get("data").isJsonNull()) {
                voiceMutedNames = extractNames(r.getAsJsonArray("data"));
                voiceAt = now;
            }
        }
    }

    private static List<String> extractNames(JsonArray arr) {
        List<String> names = new ArrayList<>();
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            if (o.has("nickname") && o.get("nickname") != null && !o.get("nickname").getAsString().isEmpty()) {
                names.add(o.get("nickname").getAsString());
            } else if (o.has("uuid") && o.get("uuid") != null) {
                names.add(o.get("uuid").getAsString().substring(0, Math.min(8, o.get("uuid").getAsString().length())));
            }
        }
        return names;
    }
}
