package com.pwp.coreclient.network.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.ModerationInput;
import com.pwp.api.security.PwpPermissions;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
            run(sender, KICK, target, true, () -> ModerationInput.kick(target, reason));
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
            run(sender, BAN, target, true, () -> ModerationInput.ban(target, duration, reason));
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
            run(sender, WARN, target, false, () -> ModerationInput.warn(target, message));
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
            run(sender, VOICE_MUTE, target, true, () -> ModerationInput.voiceMute(target, minutes, reason));
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
            run(sender, TEAM_JOIN, target, false, () -> ModerationInput.teamJoin(target, team));
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
            run(sender, CLEAR_TEAMKILL, target, false, () -> ModerationInput.clearTeamkill(target));
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
            java.util.function.Supplier<String> cmd) {
        AdminAction.runLocal(sender, action, AccessLevel.MODERATOR, () -> {
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
            int result = server.getCommands()
                    .performPrefixedCommand(sender.createCommandSourceStack().withPermission(4), command);
            return result > 0 ? null : ERR_FAILED;
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
                arr.add(e);
            }
            o.add("players", arr);
            return o.toString();
        }
    };
}
