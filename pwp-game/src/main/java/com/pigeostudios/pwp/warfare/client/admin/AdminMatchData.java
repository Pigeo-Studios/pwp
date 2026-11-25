package com.pigeostudios.pwp.warfare.client.admin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.match.AdminInput;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Разбор ответа {@code adminMatch} для вкладок «Матч»/«Карта» + фикстура тест-стенда. */
@OnlyIn(Dist.CLIENT)
public final class AdminMatchData {
    public static final String TYPE = "adminMatch";

    public record IssueRow(boolean error, String key, String subject, String detail) {
        /** «Перевод [объект] — подробность». */
        public Component text() {
            MutableComponent c = Component.translatable(key);
            if (!subject.isEmpty()) c = c.append(" [" + subject + "]");
            if (!detail.isEmpty()) c = c.append(" — " + detail);
            return c;
        }
    }

    public final JsonObject raw;

    private AdminMatchData(JsonObject raw) {
        this.raw = raw;
    }

    /** null — битый JSON или ответ-ошибка (нет прав). */
    public static AdminMatchData parse(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            return o.has("error") ? null : new AdminMatchData(o);
        } catch (JsonParseException | IllegalStateException e) {
            return null;
        }
    }

    public String str(String key, String def) {
        return raw.has(key) && raw.get(key).isJsonPrimitive() ? raw.get(key).getAsString() : def;
    }

    public int num(String key) {
        return raw.has(key) && raw.get(key).isJsonPrimitive() ? raw.get(key).getAsInt() : 0;
    }

    public boolean flag(String key) {
        return raw.has(key) && raw.get(key).isJsonPrimitive() && raw.get(key).getAsBoolean();
    }

    public List<String> factions() {
        List<String> out = new ArrayList<>();
        if (raw.has("factions") && raw.get("factions").isJsonArray()) {
            for (JsonElement e : raw.getAsJsonArray("factions")) out.add(e.getAsString());
        }
        if (out.isEmpty()) out.addAll(AdminInput.DEFAULT_FACTIONS);
        for (String cur : List.of(str("blueFaction", "none"), str("redFaction", "none"))) {
            if (!out.contains(cur)) out.add(cur);
        }
        if (!out.contains("none")) out.add("none");
        return out;
    }

    public List<IssueRow> issues() {
        List<IssueRow> out = new ArrayList<>();
        if (!raw.has("issues") || !raw.get("issues").isJsonArray()) return out;
        for (JsonElement e : raw.getAsJsonArray("issues")) {
            JsonObject i = e.getAsJsonObject();
            out.add(new IssueRow(
                    "ERROR".equals(i.get("severity").getAsString()),
                    i.get("key").getAsString(),
                    i.get("subject").getAsString(),
                    i.get("detail").getAsString()));
        }
        return out;
    }

    public List<IssueRow> errors() {
        return issues().stream().filter(IssueRow::error).toList();
    }

    /** Фикстура: ожидание, 2 ошибки + 1 предупреждение (или пустая раскладка без замечаний). */
    public static AdminMatchData fixture(boolean empty) {
        JsonObject o = new JsonObject();
        o.addProperty("phase", empty ? "IDLE" : "WAITING");
        o.addProperty("timer", empty ? 0 : 187);
        o.addProperty("paused", false);
        o.addProperty("waiting", !empty);
        o.addProperty("mode", "aas");
        o.addProperty("defender", "RED");
        o.addProperty("blueFaction", empty ? "none" : "usa");
        o.addProperty("redFaction", empty ? "none" : "russia");
        o.addProperty("blueTickets", 800);
        o.addProperty("redTickets", 800);
        o.addProperty("waitingDefault", 300);
        o.addProperty("blockEnabled", true);
        o.addProperty("layoutLoaded", !empty);
        JsonArray f = new JsonArray();
        AdminInput.DEFAULT_FACTIONS.forEach(f::add);
        o.add("factions", f);
        JsonArray issues = new JsonArray();
        if (!empty) {
            issues.add(issue("ERROR", "pwpwarfare.layout.issue.priority_hole", "C", "bluePriority 5 → expected 3"));
            issues.add(issue("ERROR", "pwpwarfare.layout.issue.no_main", "RED", ""));
            issues.add(issue("WARN", "pwpwarfare.layout.issue.points_overlap", "A/B", ""));
        }
        o.add("issues", issues);
        return new AdminMatchData(o);
    }

    private static JsonObject issue(String sev, String key, String subject, String detail) {
        JsonObject i = new JsonObject();
        i.addProperty("severity", sev);
        i.addProperty("key", key);
        i.addProperty("subject", subject);
        i.addProperty("detail", detail);
        return i;
    }
}
