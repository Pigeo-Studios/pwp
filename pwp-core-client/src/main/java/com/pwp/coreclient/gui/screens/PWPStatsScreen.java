package com.pwp.coreclient.gui.screens;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.coreclient.PlayerData;
import com.pwp.coreclient.gui.components.*;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.ClientResponseCache;
import com.pwp.coreclient.network.PacketDataRequest;
import com.pwp.coreclient.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PWPStatsScreen extends Screen {

    private int tab;
    private int lbPage;
    private int lbTotal;
    private int lbTotalPages = 1;
    private String lbOrderBy = "kills";
    private List<LeaderboardEntry> lbEntries = new ArrayList<>();
    private String playerUuid;
    private UUID playerUuidObj;
    private String playerNickname;

    private JsonObject cachedPlayerStats;
    private boolean loading = true;
    private boolean profileRequested;
    private boolean leaderboardRequested;

    private PWPTabs tabs;

    private static final String[] LB_CATEGORIES = {
        "kills", "deaths", "kd", "wins", "winrate",
        "vehicle_kills", "vehicles_destroyed", "air_destroyed",
        "captures", "damage", "healing", "headshots",
        "playtime", "level"
    };
    private static final String[] LB_CATEGORY_NAMES = {
        "Убийства", "Смерти", "K/D", "Победы", "WinRate",
        "Уб.техникой", "Тех.уничт", "В возд.",
        "Захваты", "Урон", "Лечение", "Хэдшоты",
        "Наиграно", "Уровень"
    };

    public PWPStatsScreen() {
        super(Component.literal("СТАТИСТИКА"));
        var p = Minecraft.getInstance().player;
        if (p != null) {
            playerUuid = p.getStringUUID();
            playerUuidObj = p.getUUID();
            playerNickname = p.getScoreboardName();
        }
        requestProfile();
    }

    private void requestProfile() {
        loading = true;
        profileRequested = true;
        PacketHandler.INSTANCE.sendToServer(new PacketDataRequest("profile", ""));
    }

    @Override
    public void tick() {
        if (profileRequested && playerUuidObj != null) {
            PlayerData.CachedProfile profile = PlayerData.get(playerUuidObj);
            if (profile != null && profile.data != null) {
                cachedPlayerStats = profile.data;
                profileRequested = false;
                if (tab == 0) loading = false;
            }
        }
        if (tab == 1 && !leaderboardRequested && ClientResponseCache.leaderboardData != null) {
            JsonObject data = ClientResponseCache.leaderboardData;
            lbTotal = data.has("total") ? data.get("total").getAsInt() : 0;
            int limit = data.has("limit") ? data.get("limit").getAsInt() : 20;
            int page = data.has("page") ? data.get("page").getAsInt() : 1;
            lbTotalPages = Math.max(1, (lbTotal + limit - 1) / limit);
            lbPage = page - 1;
            lbEntries.clear();
            if (data.has("players")) {
                JsonArray players = data.getAsJsonArray("players");
                for (int i = 0; i < players.size(); i++) {
                    lbEntries.add(new LeaderboardEntry(players.get(i).getAsJsonObject()));
                }
            }
            leaderboardRequested = true;
            loading = false;
        }
    }

    @Override
    protected void init() {
        clearWidgets();
        tabs = new PWPTabs(0, 6, width, tab, idx -> {
            tab = idx;
            loading = true;
            if (idx == 0) { requestProfile(); }
            else { leaderboardRequested = false; requestLeaderboard(); }
        });
        tabs.setTabs(List.of("Моя статистика", "Лидеры"));
        tabs.getWidgets().forEach(this::addRenderableWidget);

        addRenderableWidget(new PWPButton(
            width / 2 - 50, height - 30, 100, 22,
            Component.literal("Закрыть"),
            btn -> onClose(),
            PWPButton.Style.GHOST
        ));
    }

    public void requestLeaderboard() {
        leaderboardRequested = true;
        ClientResponseCache.leaderboardData = null;
        String params = "{\"orderBy\":\"" + lbOrderBy + "\",\"page\":" + (lbPage + 1) + "}";
        PacketHandler.INSTANCE.sendToServer(new PacketDataRequest("leaderboard", params));
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);

        PWPLayout.renderHeader(gui, "СТАТИСТИКА", width);
        if (tabs != null) tabs.render(gui, mx, my, pt);

        int panelY = 24;
        int panelH = height - panelY - 36;
        int contentW = PWPLayout.contentWidth(width);
        int cx = PWPLayout.centerX(width, contentW);

        if (loading) {
            gui.drawCenteredString(font, "\u00a77Загрузка...", cx, panelY + panelH / 2, PWPTheme.Colors.TEXT_SECONDARY);
            super.render(gui, mx, my, pt);
            return;
        }

        PWPPanel.render(gui, cx, panelY, contentW, panelH);

        int innerX = cx + 6;
        int innerW = contentW - 12;
        int innerY = panelY + 6;
        int innerH = panelH - 12;

        if (tab == 0) {
            renderMyStats(gui, innerX, innerY, innerW, innerH);
        } else {
            renderLeaderboard(gui, innerX, innerY, innerW, innerH, mx, my);
        }

        super.render(gui, mx, my, pt);
    }

    private void renderMyStats(GuiGraphics gui, int x, int y, int w, int h) {
        if (cachedPlayerStats == null) return;

        String nick = playerNickname;
        int level = 1, prestige = 0;
        if (cachedPlayerStats.has("player")) {
            JsonObject p = cachedPlayerStats.getAsJsonObject("player");
            if (p.has("nickname")) nick = p.get("nickname").getAsString();
        }
        if (cachedPlayerStats.has("level")) level = cachedPlayerStats.get("level").getAsInt();
        if (cachedPlayerStats.has("prestige")) prestige = cachedPlayerStats.get("prestige").getAsInt();

        JsonObject st = cachedPlayerStats.has("stats") ? cachedPlayerStats.getAsJsonObject("stats") : cachedPlayerStats;

        int rowY = y;
        gui.fill(x, rowY, x + w, rowY + 24, PWPTheme.Colors.SURFACE_LIGHT);
        gui.fill(x, rowY + 24, x + w, rowY + 25, PWPTheme.Colors.BORDER);
        gui.drawString(font, Component.literal(nick + "  Lv." + level), x + 8, rowY + 4, PWPTheme.Colors.TEXT_PRIMARY);
        gui.drawString(font, Component.literal("Матчи: " + intVal(st, "matchesPlayed")), x + 8, rowY + 14, PWPTheme.Colors.TEXT_SECONDARY);
        rowY += 28;

        rowY = drawStatSection(gui, x, rowY, w, "БОЙ", new String[][]{
            {"Убийства", intVal(st, "kills")}, {"Смерти", intVal(st, "deaths")},
            {"K/D", formatKd(st)}, {"Спасения", intVal(st, "revives")},
            {"Уб.техникой", intVal(st, "vehicleKills")}, {"Захваты", intVal(st, "captures")},
        });

        PWPLayout.renderDivider(gui, x + 10, rowY, w - 20);
        rowY += 4;

        rowY = drawStatSection(gui, x, rowY, w, "ТЕХНИКА", new String[][]{
            {"Уничтожено", intVal(st, "vehiclesDestroyed")}, {"Воздух", intVal(st, "airVehiclesDestroyed")},
        });

        PWPLayout.renderDivider(gui, x + 10, rowY, w - 20);
        rowY += 4;

        rowY = drawStatSection(gui, x, rowY, w, "МАТЧИ", new String[][]{
            {"Сыграно", intVal(st, "matchesPlayed")}, {"Победы", intVal(st, "wins")},
            {"Поражения", intVal(st, "losses")}, {"WinRate", formatWins(st)},
            {"Время", formatPlaytime(st)}, {"Уб./матч", formatKpg(st)},
        });
    }

    private int drawStatSection(GuiGraphics gui, int x, int y, int w, String sectionTitle, String[][] rows) {
        gui.drawString(font, Component.literal(sectionTitle), x + 8, y + 2, PWPTheme.Colors.TEXT_ACCENT);
        y += 14;

        int colW = w / 2 - 12;
        for (String[] row : rows) {
            gui.drawString(font, Component.literal(row[0] + ":"), x + 8, y, PWPTheme.Colors.TEXT_SECONDARY);
            gui.drawString(font, Component.literal(row[1]), x + 8 + colW, y, PWPTheme.Colors.TEXT_PRIMARY);
            y += 11;
        }
        return y + 2;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        return super.mouseClicked(mx, my, btn);
    }

    private void renderLeaderboard(GuiGraphics gui, int x, int y, int w, int h, int mx, int my) {
        int catY = y;
        int catX = x;

        for (int i = 0; i < LB_CATEGORIES.length; i++) {
            int bw = font.width(LB_CATEGORY_NAMES[i]) + 14;
            boolean isSel = lbOrderBy.equals(LB_CATEGORIES[i]);
            boolean hover = mx >= catX && mx <= catX + bw && my >= catY && my <= catY + 18;
            int bg = isSel ? PWPTheme.Colors.ACCENT_DIM : (hover ? PWPTheme.Colors.SURFACE_LIGHT : PWPTheme.Colors.SURFACE);
            gui.fill(catX, catY, catX + bw, catY + 18, bg);

            int borderCol = isSel ? PWPTheme.Colors.ACCENT : (hover ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Colors.BORDER);
            gui.fill(catX, catY, catX + bw, catY + 1, borderCol);
            gui.fill(catX, catY + 17, catX + bw, catY + 18, borderCol);
            gui.fill(catX, catY, catX + 1, catY + 18, borderCol);
            gui.fill(catX + bw - 1, catY, catX + bw, catY + 18, borderCol);

            gui.drawString(font, Component.literal(LB_CATEGORY_NAMES[i]), catX + 6, catY + 5,
                isSel ? PWPTheme.Colors.TEXT_ACCENT : (hover ? PWPTheme.Colors.TEXT_PRIMARY : PWPTheme.Colors.TEXT_SECONDARY));

            catX += bw + 4;
        }

        int rowY = catY + 22;
        int rowH = 14;

        gui.fill(x, rowY, x + w, rowY + 1, PWPTheme.Colors.BORDER);
        rowY += 2;

        int rankOffset = lbPage * 10;
        for (int i = 0; i < lbEntries.size(); i++) {
            LeaderboardEntry e = lbEntries.get(i);
            boolean isMe = e.uuid.equals(playerUuid);
            int rowBg = isMe ? 0xFF2A2010 : (i % 2 == 0 ? PWPTheme.Colors.SURFACE : 0xFF0E1117);
            gui.fill(x, rowY, x + w, rowY + rowH, rowBg);

            if (isMe) {
                gui.fill(x, rowY, x + w, rowY + 1, PWPTheme.Colors.ACCENT);
                gui.fill(x, rowY + rowH - 1, x + w, rowY + rowH, PWPTheme.Colors.ACCENT);
            }

            int rn = rankOffset + i + 1;
            gui.drawString(font, Component.literal("#" + rn), x + 4, rowY + 3, PWPTheme.Colors.TEXT_PRIMARY);
            gui.drawString(font, Component.literal(e.nickname), x + 28, rowY + 3,
                isMe ? PWPTheme.Colors.TEXT_ACCENT : PWPTheme.Colors.TEXT_PRIMARY);

            String val = getColValue(e, lbOrderBy);
            gui.drawString(font, Component.literal(val), x + w - 60, rowY + 3, PWPTheme.Colors.TEXT_PRIMARY);

            rowY += rowH;
        }
    }

    private String getColValue(LeaderboardEntry e, String col) {
        return switch (col) {
            case "kills" -> String.valueOf(e.kills);
            case "deaths" -> String.valueOf(e.deaths);
            case "kd" -> String.format("%.2f", e.kd);
            case "wins" -> String.valueOf(e.wins);
            case "winrate" -> String.format("%.1f%%", e.winRate);
            default -> String.valueOf(e.kills);
        };
    }

    private String intVal(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return "0";
        try { return String.format("%,d", obj.get(key).getAsInt()); } catch (Exception e) { return "0"; }
    }

    private String formatKd(JsonObject obj) {
        if (obj == null) return "0.00";
        int k = obj.has("kills") ? obj.get("kills").getAsInt() : 0;
        int d = obj.has("deaths") ? obj.get("deaths").getAsInt() : 0;
        if (d == 0) return String.format("%.2f", (double) k);
        return String.format("%.2f", (double) k / d);
    }

    private String formatWins(JsonObject obj) {
        if (obj == null) return "0%";
        int w = obj.has("wins") ? obj.get("wins").getAsInt() : 0;
        int l = obj.has("losses") ? obj.get("losses").getAsInt() : 0;
        int t = w + l;
        if (t == 0) return "0%";
        return String.format("%.1f%%", (double) w / t * 100);
    }

    private String formatKpg(JsonObject obj) {
        if (obj == null) return "0.00";
        int k = obj.has("kills") ? obj.get("kills").getAsInt() : 0;
        int m = obj.has("matchesPlayed") ? obj.get("matchesPlayed").getAsInt() : 0;
        if (m == 0) return "0.00";
        return String.format("%.2f", (double) k / m);
    }

    private String formatPlaytime(JsonObject obj) {
        if (obj == null || !obj.has("playtimeSeconds")) return "0h";
        long secs = obj.get("playtimeSeconds").getAsLong();
        if (secs < 60) return secs + "s";
        long min = secs / 60;
        if (min < 60) return min + "m";
        long hours = min / 60;
        return hours + "h";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static class LeaderboardEntry {
        final String uuid, nickname;
        int kills, deaths, wins, vehicleKills, captures, headshots;
        int vehiclesDestroyed, airVehiclesDestroyed;
        int level, matchesPlayed;
        long playtime;
        double damage, healing, kd, winRate;

        LeaderboardEntry(JsonObject obj) {
            JsonObject p = obj.has("player") ? obj.getAsJsonObject("player") : obj;
            JsonObject st = obj.has("stats") ? obj.getAsJsonObject("stats") : obj;
            uuid = getStr(p, "uuid", "");
            nickname = getStr(p, "nickname", "");
            kills = getInt(st, "kills", 0);
            deaths = getInt(st, "deaths", 0);
            wins = getInt(st, "wins", 0);
            vehicleKills = getInt(st, "vehicleKills", 0);
            captures = getInt(st, "captures", 0);
            headshots = getInt(st, "headshots", 0);
            vehiclesDestroyed = getInt(st, "vehiclesDestroyed", 0);
            airVehiclesDestroyed = getInt(st, "airVehiclesDestroyed", 0);
            level = getInt(obj, "level", 0);
            matchesPlayed = getInt(st, "matchesPlayed", 0);
            playtime = getLong(st, "playtimeSeconds");
            damage = getDouble(st, "damageDealt");
            healing = getDouble(st, "healingDone");
            int totalGames = wins + getInt(st, "losses", 0);
            kd = deaths == 0 ? kills : Math.round((double) kills / deaths * 100.0) / 100.0;
            winRate = totalGames == 0 ? 0 : Math.round((double) wins / totalGames * 1000.0) / 10.0;
        }

        String getStr(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }
        int getInt(JsonObject o, String k, int d) { return o.has(k) ? o.get(k).getAsInt() : d; }
        long getLong(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsLong() : 0; }
        double getDouble(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsDouble() : 0; }
    }
}
