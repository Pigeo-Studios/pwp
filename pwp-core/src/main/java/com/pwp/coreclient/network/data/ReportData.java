package com.pwp.coreclient.network.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pwp.api.core.CoreApi;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.util.ServerThread;
import com.pwp.coreserver.community.ReportService;
import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Жалобы и история наказаний для GUI (этап 10a).
 *
 * <ul>
 *   <li>C2S {@link Send} — жалоба игрока (экран «Пожаловаться» в паузе): ник, причина из списка, подробности
 *   <li>тип {@code adminReports} (MODERATOR+) — открытые жалобы из core; core недоступен — с этого сервера
 *   <li>C2S {@link Close} (MODERATOR+) — закрыть жалобу
 *   <li>тип {@code adminPlayerHistory} (MODERATOR+, q {@code name}) — история наказаний игрока онлайн
 * </ul>
 */
public final class ReportData {
    public static final String REPORTS_TYPE = "adminReports";
    public static final String HISTORY_TYPE = "adminPlayerHistory";
    public static final String ACTION_CLOSE = "report.close";

    private ReportData() {}

    public static void registerData() {
        DataRequests.register(REPORTS_TYPE, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                if (!PwpPermissions.has(p, AccessLevel.MODERATOR)) return DataRequests.empty();
                return CoreApi.listReports("open", 0, 50);
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                JsonObject o = new JsonObject();
                if (!PwpPermissions.has(p, AccessLevel.MODERATOR)) {
                    o.addProperty("error", AdminAction.ERR_NO_PERMISSION);
                    return o.toString();
                }
                if (CoreApi.isSuccess(r) && r.has("data") && r.get("data").isJsonArray()) {
                    o.addProperty("source", "core");
                    o.add("reports", r.getAsJsonArray("data"));
                } else {
                    o.addProperty("source", "local");
                    JsonArray local = new JsonArray();
                    for (JsonElement e : ReportService.local()) {
                        JsonObject l = e.getAsJsonObject();
                        JsonObject x = new JsonObject();
                        x.addProperty("id", 0);
                        x.add("server", l.get("server"));
                        x.add("reporter", l.get("reporterName"));
                        x.add("target", l.get("targetName"));
                        x.add("reason", l.get("reason"));
                        x.add("details", l.get("details"));
                        x.add("createdAt", l.get("createdAt"));
                        local.add(x);
                    }
                    o.add("reports", local);
                }
                return o.toString();
            }
        });

        DataRequests.register(HISTORY_TYPE, new DataRequests.Handler() {
            @Override
            public CompletableFuture<JsonObject> fetch(ServerPlayer p, JsonObject q) {
                if (!PwpPermissions.has(p, AccessLevel.MODERATOR)) return DataRequests.empty();
                ServerPlayer target = p.server.getPlayerList().getPlayerByName(DataRequests.str(q, "name", ""));
                return target == null ? DataRequests.empty() : CoreApi.getPunishments(target.getStringUUID());
            }

            @Override
            public String toJson(ServerPlayer p, JsonObject q, JsonObject r) {
                JsonObject o = new JsonObject();
                o.addProperty("name", DataRequests.str(q, "name", ""));
                if (!PwpPermissions.has(p, AccessLevel.MODERATOR)) {
                    o.addProperty("error", AdminAction.ERR_NO_PERMISSION);
                } else if (CoreApi.isSuccess(r)
                        && r.has("data")
                        && r.get("data").isJsonArray()) {
                    o.add("items", r.getAsJsonArray("data"));
                } else {
                    o.addProperty("error", AdminAction.ERR_CORE_DOWN);
                }
                return o.toString();
            }
        });
    }

    /** Жалоба: ник цели, причина (из {@link ReportService#REASONS}), подробности (необязательно). */
    public record Send(String target, String reason, String details) implements Net.C2S {
        public static void encode(Send m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
            b.writeUtf(m.reason, 24);
            b.writeUtf(m.details, 200);
        }

        public static Send decode(FriendlyByteBuf b) {
            return new Send(b.readUtf(16), b.readUtf(24), b.readUtf(200));
        }

        @Override
        public String validate() {
            return Validate.first(
                    Validate.id("target", target, 16),
                    Validate.oneOf("reason", reason, ReportService.REASONS.toArray(String[]::new)),
                    Validate.optionalText("details", details, 200));
        }

        @Override
        public void handle(ServerPlayer sender) {
            String err = ReportService.report(sender, target, reason, details);
            if (err != null)
                sender.sendSystemMessage(Component.translatable(err).withStyle(ChatFormatting.RED));
        }
    }

    /** Закрыть жалобу (MODERATOR+). */
    public record Close(long id) implements Net.C2S {
        public static void encode(Close m, FriendlyByteBuf b) {
            b.writeVarLong(m.id);
        }

        public static Close decode(FriendlyByteBuf b) {
            return new Close(b.readVarLong());
        }

        @Override
        public String validate() {
            return id > 0 ? null : "id: " + id;
        }

        @Override
        public void handle(ServerPlayer sender) {
            AdminAction.runLocal(sender, ACTION_CLOSE, AccessLevel.MODERATOR, "#" + id, () -> {
                ServerThread.thenForPlayer(
                        CoreApi.closeReport(id, sender.getGameProfile().getName()),
                        sender,
                        (online, resp) -> DataRequests.refresh(online, REPORTS_TYPE));
                return null;
            });
        }
    }
}
