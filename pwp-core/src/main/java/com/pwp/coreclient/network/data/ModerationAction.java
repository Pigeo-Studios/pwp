package com.pwp.coreclient.network.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.ModerationInput;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.util.LogOnce;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Вкладка «Модерация» (уровень MODERATOR+): действия над игроками онлайн. Каждый пакет — одно действие;
 * сервер проверяет уровень, ник и поля ({@link ModerationInput}) и сам собирает строку СУЩЕСТВУЮЩЕЙ
 * команды ({@code /kick}, {@code /pwpban}, {@code /pwpwarfare warn|voicemute|teamjoin|cleartenkill}),
 * которую выполняет от имени модератора с правом op 4 (ответы команды приходят модератору в чат). Логика
 * команд не дублируется. Нет команды на сервере (например, warfare на лобби) — ошибка «недоступно».
 */
public final class ModerationAction {
    private static final Logger LOG = LoggerFactory.getLogger(ModerationAction.class);

    /** Тип данных: игроки онлайн для вкладки. */
    public static final String PLAYERS_TYPE = "adminPlayers";

    public static final String KICK = "mod.kick";
    public static final String BAN = "mod.ban";
    public static final String WARN = "mod.warn";
    public static final String VOICE_MUTE = "mod.voiceMute";
    public static final String TEAM_JOIN = "mod.teamJoin";
    public static final String CLEAR_TEAMKILL = "mod.clearTeamkill";

    static final String ERR_NAME = "pwp_core.admin.mod.error.name";
    static final String ERR_OFFLINE = "pwp_core.admin.mod.error.offline";
    static final String ERR_UNAVAILABLE = "pwp_core.admin.mod.error.unavailable";
    static final String ERR_RANK = "pwp_core.admin.mod.error.rank";
    static final String ERR_FAILED = "pwp_core.admin.mod.error.failed";

    private ModerationAction() {}

    public record Kick(String target, String reason) implements Net.C2S {
        public static void encode(Kick m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
            b.writeUtf(m.reason, ModerationInput.TEXT_MAX);
        }

        public static Kick decode(FriendlyByteBuf b) {
            return new Kick(b.readUtf(16), b.readUtf(ModerationInput.TEXT_MAX));
        }

        @Override
        public String validate() {
            return Validate.optionalText("reason", reason, ModerationInput.TEXT_MAX);
        }

        @Override
        public void handle(ServerPlayer sender) {
            run(sender, KICK, target, true, "KICK", reason, null, () -> ModerationInput.kick(target, reason));
        }
    }

    public record Ban(String target, String duration, String reason) implements Net.C2S {
        public static void encode(Ban m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
            b.writeUtf(m.duration, 8);
            b.writeUtf(m.reason, ModerationInput.TEXT_MAX);
        }

        public static Ban decode(FriendlyByteBuf b) {
            return new Ban(b.readUtf(16), b.readUtf(8), b.readUtf(ModerationInput.TEXT_MAX));
        }

        @Override
        public String validate() {
            if (!ModerationInput.validDuration(duration)) return "duration: " + duration;
            return Validate.optionalText("reason", reason, ModerationInput.TEXT_MAX);
        }

        @Override
        public void handle(ServerPlayer sender) {
            // бан пишет историю сам (core /launcher/ban)
            run(sender, BAN, target, true, null, reason, null, () -> ModerationInput.ban(target, duration, reason));
        }
    }

    public record Warn(String target, String message) implements Net.C2S {
        public static void encode(Warn m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
            b.writeUtf(m.message, ModerationInput.TEXT_MAX);
        }

        public static Warn decode(FriendlyByteBuf b) {
            return new Warn(b.readUtf(16), b.readUtf(ModerationInput.TEXT_MAX));
        }

        @Override
        public String validate() {
            return Validate.text("message", message, ModerationInput.TEXT_MAX);
        }

        @Override
        public void handle(ServerPlayer sender) {
            run(sender, WARN, target, false, "WARN", message, null, () -> ModerationInput.warn(target, message));
        }
    }

    public record VoiceMute(String target, int minutes, String reason) implements Net.C2S {
        public static void encode(VoiceMute m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
            b.writeVarInt(m.minutes);
            b.writeUtf(m.reason, ModerationInput.TEXT_MAX);
        }

        public static VoiceMute decode(FriendlyByteBuf b) {
            return new VoiceMute(b.readUtf(16), b.readVarInt(), b.readUtf(ModerationInput.TEXT_MAX));
        }

        @Override
        public String validate() {
            return Validate.first(
                    Validate.range("minutes", minutes, 1, ModerationInput.MUTE_MINUTES_MAX),
                    Validate.optionalText("reason", reason, ModerationInput.TEXT_MAX));
        }

        @Override
        public void handle(ServerPlayer sender) {
            run(
                    sender,
                    VOICE_MUTE,
                    target,
                    true,
                    "MUTE",
                    reason,
                    minutes,
                    () -> ModerationInput.voiceMute(target, minutes, reason));
        }
    }

    public record TeamJoin(String target, String team) implements Net.C2S {
        public static void encode(TeamJoin m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
            b.writeUtf(m.team, 8);
        }

        public static TeamJoin decode(FriendlyByteBuf b) {
            return new TeamJoin(b.readUtf(16), b.readUtf(8));
        }

        @Override
        public String validate() {
            return Validate.oneOf("team", team, "BLUE", "RED");
        }

        @Override
        public void handle(ServerPlayer sender) {
            run(sender, TEAM_JOIN, target, false, null, null, null, () -> ModerationInput.teamJoin(target, team));
        }
    }

    public record ClearTeamkill(String target) implements Net.C2S {
        public static void encode(ClearTeamkill m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
        }

        public static ClearTeamkill decode(FriendlyByteBuf b) {
            return new ClearTeamkill(b.readUtf(16));
        }

        @Override
        public void handle(ServerPlayer sender) {
            run(sender, CLEAR_TEAMKILL, target, false, null, null, null, () -> ModerationInput.clearTeamkill(target));
        }
    }

    /**
     * Общий путь: MODERATOR+ → ник корректен и онлайн → (для наказаний) цель не выше по уровню →
     * команда есть на сервере → выполнение от имени модератора с op 4.
     */
    private static void run(
            ServerPlayer sender,
            String action,
            String target,
            boolean punish,
            String historyType,
            String reason,
            Integer minutes,
            java.util.function.Supplier<String> cmd) {
        AdminAction.runLocal(sender, action, AccessLevel.MODERATOR, target, () -> {
            if (!ModerationInput.validName(target)) return ERR_NAME;
            MinecraftServer server = sender.getServer();
            if (server == null) return ERR_FAILED;
            ServerPlayer victim = server.getPlayerList().getPlayerByName(target);
            if (victim == null) return ERR_OFFLINE;
            // наказывать равного/старшего нельзя (модератор не кикает админа); себя — можно не трогать
            if (punish
                    && !victim.getUUID().equals(sender.getUUID())
                    && !PwpPermissions.level(sender).atLeast(next(PwpPermissions.level(victim)))) {
                return ERR_RANK;
            }
            String command = cmd.get();
            String root = command.substring(0, command.indexOf(' '));
            if (server.getCommands().getDispatcher().getRoot().getChild(root) == null) return ERR_UNAVAILABLE;
            LOG.info("Модерация: {} выполняет /{}", sender.getGameProfile().getName(), command);
            String victimUuid = victim.getStringUUID();
            int result = server.getCommands()
                    .performPrefixedCommand(sender.createCommandSourceStack().withPermission(4), command);
            if (result <= 0) return ERR_FAILED;
            // история наказаний (этап 10a): видна в карточке игрока F6 на любом сервере
            if (historyType != null) {
                com.pwp.api.core.CoreApi.recordPunishment(
                        victimUuid, historyType, reason, sender.getStringUUID(), minutes);
            }
            return null;
        });
    }

    /** Следующий уровень (OWNER — потолок: OWNER может наказать OWNER). */
    private static AccessLevel next(AccessLevel l) {
        AccessLevel[] all = AccessLevel.values();
        return all[Math.min(all.length - 1, l.ordinal() + 1)];
    }

    /** Тип {@code adminPlayers}: игроки онлайн (ник, команда, уровень). Ниже MODERATOR — ошибка. */
    static final DataRequests.Handler PLAYERS = new DataRequests.Handler() {
        @Override
        public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
            return DataRequests.empty();
        }

        @Override
        public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
            JsonObject o = new JsonObject();
            if (!PwpPermissions.has(p, AccessLevel.MODERATOR)) {
                o.addProperty("error", AdminAction.ERR_NO_PERMISSION);
                return o.toString();
            }
            JsonArray arr = new JsonArray();
            for (ServerPlayer pl : p.server.getPlayerList().getPlayers()) {
                JsonObject e = new JsonObject();
                e.addProperty("name", pl.getGameProfile().getName());
                e.addProperty("team", pl.getTeam() == null ? "" : pl.getTeam().getName());
                e.addProperty("level", PwpPermissions.level(pl).name());
                e.addProperty("ping", pl.latency);
                e.addProperty("spectating", SPECTATE_PREV.containsKey(pl.getUUID()));
                for (BiConsumer<ServerPlayer, JsonObject> extra : PLAYER_INFO) {
                    try {
                        extra.accept(pl, e);
                    } catch (RuntimeException ex) {
                        LogOnce.warn(LOG, "admin_player_info", "Расширение списка игроков упало", ex);
                    }
                }
                arr.add(e);
            }
            o.add("players", arr);
            o.add("actions", ACTIONS.deepCopy());
            return o.toString();
        }
    };

    // ---------- расширения модов ----------

    private static final List<BiConsumer<ServerPlayer, JsonObject>> PLAYER_INFO = new CopyOnWriteArrayList<>();
    /** Объявленные модами кнопки вкладки (id, минимальный уровень) — клиент показывает только доступные. */
    private static final JsonArray ACTIONS = new JsonArray();

    /**
     * Мод (warfare) дописывает поля игрока в {@code adminPlayers}: отряд, кит, лидер… Поля — строки/числа;
     * вкладка показывает {@code squad}, {@code kit}, {@code leader} (остальные игнорирует).
     */
    public static void addPlayerInfo(BiConsumer<ServerPlayer, JsonObject> extra) {
        PLAYER_INFO.add(extra);
    }

    /** Сервер объявляет действие вкладки (клиентская кнопка с тем же id регистрирует мод на клиенте). */
    public static synchronized void declareAction(String id) {
        for (var e : ACTIONS) if (e.getAsString().equals(id)) return;
        ACTIONS.add(id);
    }

    // ---------- телепорт и наблюдение (MODERATOR) ----------

    public static final String TELEPORT = "mod.teleport";
    public static final String SPECTATE = "mod.spectate";
    static final String ERR_SELF = "pwp_core.admin.mod.error.self";

    /** Прежний режим игры модератора, ушедшего в наблюдение (до повторного нажатия / выхода). */
    private static final Map<UUID, GameType> SPECTATE_PREV = new ConcurrentHashMap<>();

    /** Телепорт модератора к игроку (в его измерение). */
    public record Teleport(String target) implements Net.C2S {
        public static void encode(Teleport m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
        }

        public static Teleport decode(FriendlyByteBuf b) {
            return new Teleport(b.readUtf(16));
        }

        @Override
        public void handle(ServerPlayer sender) {
            AdminAction.runLocal(sender, TELEPORT, AccessLevel.MODERATOR, target, () -> {
                ServerPlayer t = online(sender, target);
                if (t == null) return ERR_OFFLINE;
                if (t == sender) return ERR_SELF;
                sender.teleportTo(t.serverLevel(), t.getX(), t.getY(), t.getZ(), t.getYRot(), t.getXRot());
                return null;
            });
        }
    }

    /**
     * Наблюдать за игроком: режим наблюдателя + камера игрока (видит всё, как креатив/спектатор в
     * TeamVisibility). Повторное нажатие (цель любая) — вернуть прежний режим и свою камеру.
     */
    public record Spectate(String target) implements Net.C2S {
        public static void encode(Spectate m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
        }

        public static Spectate decode(FriendlyByteBuf b) {
            return new Spectate(b.readUtf(16));
        }

        @Override
        public void handle(ServerPlayer sender) {
            AdminAction.runLocal(sender, SPECTATE, AccessLevel.MODERATOR, target, () -> {
                GameType prev = SPECTATE_PREV.remove(sender.getUUID());
                if (prev != null) {
                    sender.setCamera(sender);
                    sender.setGameMode(prev);
                    return null;
                }
                ServerPlayer t = online(sender, target);
                if (t == null) return ERR_OFFLINE;
                if (t == sender) return ERR_SELF;
                SPECTATE_PREV.put(sender.getUUID(), sender.gameMode.getGameModeForPlayer());
                sender.setGameMode(GameType.SPECTATOR);
                sender.teleportTo(t.serverLevel(), t.getX(), t.getY(), t.getZ(), t.getYRot(), t.getXRot());
                sender.setCamera(t);
                return null;
            });
        }
    }

    /** Выход модератора в режиме наблюдения: вернуть прежний режим до сохранения игрока. */
    public static void onLogout(ServerPlayer player) {
        GameType prev = SPECTATE_PREV.remove(player.getUUID());
        if (prev != null) player.setGameMode(prev);
    }

    private static ServerPlayer online(ServerPlayer sender, String name) {
        if (!ModerationInput.validName(name)) return null;
        return sender.server.getPlayerList().getPlayerByName(name);
    }
}
