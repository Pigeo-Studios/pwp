package com.pwp.coreclient.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.coreclient.CoreAPI;
import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class StatsScreen extends Screen {

    private int tab = 0;
    private int lbPage = 0;
    private int lbTotalPages = 1;
    private String lbOrderBy = "kills";
    private List<LeaderboardEntry> lbEntries = new ArrayList<>();
    private int lbTotal = 0;
    private String playerUuid;
    private String playerNickname;

    private JsonObject cachedPlayerStats;
    private JsonObject cachedRank;
    private boolean loading = true;
    private String errorMsg = null;

    private double scrollOffset = 0;
    private double scrollMax = 0;

    private static final int TAB_MY_STATS = 0;
    private static final int TAB_LEADERBOARD = 1;

    private static final int CONTENT_TOP = 52;
    private static final int CONTENT_BOTTOM_OFFSET = 34;

    private static final String[] LB_CATEGORIES = {
        "kills", "deaths", "assists", "kd", "wins", "winrate",
        "vehicle_kills", "vehicles_destroyed", "air_destroyed",
        "captures", "damage", "healing", "headshots",
        "hub_destructions", "playtime", "level"
    };
    private static final String[] LB_CATEGORY_NAMES = {
        "Kills", "Deaths", "Assists", "K/D", "Wins", "WinRate",
        "V.Kills", "V.Destr.", "Air Dstr.",
        "Captures", "Damage", "Healing", "Headshots",
        "Hub Destr", "Playtime", "Level"
    };

    private List<int[]> catBounds = new ArrayList<>();

    public StatsScreen() {
        super(Component.literal("STATISTICS"));
        Player p = Minecraft.getInstance().player;
        if (p != null) {
            playerUuid = p.getStringUUID();
            playerNickname = p.getScoreboardName();
        }
        fetchData();
    }

    private void fetchData() {
        loading = true;
        errorMsg = null;
        scrollOffset = 0;
        new Thread(() -> {
            try {
                JsonObject profileResp = CoreAPI.getPlayerProfile(playerUuid);
                if (profileResp != null && profileResp.has("data")) {
                    cachedPlayerStats = profileResp.getAsJsonObject("data");
                }
                JsonObject rankResp = CoreAPI.getPlayerRank(playerUuid, lbOrderBy);
                if (rankResp != null && rankResp.has("data")) {
                    cachedRank = rankResp.getAsJsonObject("data");
                }
                fetchLeaderboardPage();
            } catch (Exception e) {
                errorMsg = "Failed to load stats";
            }
            loading = false;
        }).start();
    }

    private void fetchLeaderboardPage() {
        JsonObject lbResp = CoreAPI.getLeaderboard(lbOrderBy, lbPage + 1, 10);
        if (lbResp != null && lbResp.has("data")) {
            JsonObject data = lbResp.getAsJsonObject("data");
            lbTotal = data.get("total").getAsInt();
            int limit = data.get("limit").getAsInt();
            int page = data.get("page").getAsInt();
            lbTotalPages = Math.max(1, (lbTotal + limit - 1) / limit);

            lbEntries.clear();
            JsonArray players = data.getAsJsonArray("players");
            if (players != null) {
                for (int i = 0; i < players.size(); i++) {
                    lbEntries.add(new LeaderboardEntry(players.get(i).getAsJsonObject()));
                }
            }
        }
    }

    @Override
    protected void init() {
        clearWidgets();
        int cx = width / 2;

        addRenderableWidget(Button.builder(
                Component.literal("My Stats"),
                b -> { tab = TAB_MY_STATS; scrollOffset = 0; init(); })
                .bounds(cx - 160, 8, 80, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Leaderboard"),
                b -> { tab = TAB_LEADERBOARD; scrollOffset = 0; fetchLeaderboardPage(); init(); })
                .bounds(cx - 80, 8, 80, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("\u2715 Close"),
                b -> onClose())
                .bounds(cx - 50, height - 28, 100, 20).build());

        if (tab == TAB_LEADERBOARD) {
            addRenderableWidget(Button.builder(
                    Component.literal("\u25C0"),
                    b -> { if (lbPage > 0) { lbPage--; scrollOffset = 0; fetchLeaderboardPage(); init(); }})
                    .bounds(cx + 80, 8, 20, 20).build());
            addRenderableWidget(Button.builder(
                    Component.literal("\u25B6"),
                    b -> { if (lbPage < lbTotalPages - 1) { lbPage++; scrollOffset = 0; fetchLeaderboardPage(); init(); }})
                    .bounds(cx + 104, 8, 20, 20).build());
            addRenderableWidget(Button.builder(
                    Component.literal("Page " + (lbPage + 1) + "/" + lbTotalPages),
                    b -> {})
                    .bounds(cx - 60, 8, 90, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);

        int cx = width / 2;
        gui.drawCenteredString(font, "\u00a76\u2694 STATISTICS", cx, 34, 0xFFFFFF);

        if (loading) {
            gui.drawCenteredString(font, "\u00a77Loading...", cx, height / 2, 0x7A7D84);
            return;
        }
        if (errorMsg != null) {
            gui.drawCenteredString(font, "\u00a7c" + errorMsg, cx, height / 2, 0xFF5555);
            return;
        }

        int clipY = CONTENT_TOP;
        int clipH = height - CONTENT_TOP - CONTENT_BOTTOM_OFFSET;
        gui.enableScissor(0, clipY, width, clipH);

        if (tab == TAB_MY_STATS) {
            renderMyStats(gui, mx, my, clipY, clipH);
        } else {
            renderLeaderboard(gui, mx, my, clipY, clipH);
        }

        gui.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta != 0) {
            scrollOffset -= delta * 10;
            if (scrollOffset < 0) scrollOffset = 0;
            if (scrollOffset > scrollMax) scrollOffset = scrollMax;
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (btn == 0 && tab == TAB_LEADERBOARD) {
            for (int[] b : catBounds) {
                if (mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3]) {
                    String cat = LB_CATEGORIES[b[4]];
                    if (!lbOrderBy.equals(cat)) {
                        lbOrderBy = cat;
                        lbPage = 0;
                        scrollOffset = 0;
                        fetchLeaderboardPage();
                        init();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    // ====== MY STATS TAB ======

    private void renderMyStats(GuiGraphics gui, int mx, int my, int clipY, int clipH) {
        if (cachedPlayerStats == null) return;
        int leftX = Math.max(10, width / 2 - 180);
        int panelW = 360;

        String nick = playerNickname;
        int level = 1, prestige = 0;
        if (cachedPlayerStats.has("player")) {
            JsonObject p = cachedPlayerStats.getAsJsonObject("player");
            if (p.has("nickname")) nick = p.get("nickname").getAsString();
        }
        if (cachedPlayerStats.has("level")) level = cachedPlayerStats.get("level").getAsInt();
        if (cachedPlayerStats.has("prestige")) prestige = cachedPlayerStats.get("prestige").getAsInt();

        JsonObject st = cachedPlayerStats.has("stats") ? cachedPlayerStats.getAsJsonObject("stats") : cachedPlayerStats;

        int rank = -1, total = 0;
        if (cachedRank != null) {
            rank = cachedRank.get("rank").getAsInt();
            total = cachedRank.get("total").getAsInt();
        }

        int y = clipY + 4 - (int) scrollOffset;
        String rankStr = rank > 0 ? "\u00a7e#" + rank + "\u00a77 of " + total : "\u00a77--";
        gui.drawString(font, "\u00a7f" + nick + "  \u00a77Lv." + level + " \u00a7e\u2726" + prestige, leftX + 2, y, 0xFFFFFF);
        gui.drawString(font, "\u00a77Rank: " + rankStr + "  \u00a77Matches: " + intVal(st, "matches_played"), leftX + 2, y + 12, 0x7A7D84);
        y += 28;

        y = drawPanel(gui, leftX, y, panelW, "\u2694 BATTLE", new String[][]{
            {"Kills", intVal(st, "kills"), "Deaths", intVal(st, "deaths")},
            {"K/D", formatKd(st), "Assists", intVal(st, "assists")},
            {"Vehicle Kills", intVal(st, "vehicle_kills"), "Captures", intVal(st, "captures")},
            {"Headshots", intVal(st, "headshots"), "Best Streak", intVal(st, "best_kill_streak")},
            {"TeamKills", intVal(st, "team_kills"), "Hub Destr.", intVal(st, "hub_destructions")},
            {"Base Defends", intVal(st, "base_defends"), "", ""},
        });
        y += 5;

        y = drawPanel(gui, leftX, y, panelW, "\u2605 VEHICLES", new String[][]{
            {"Destroyed", intVal(st, "vehicles_destroyed"), "Air Destroyed", intVal(st, "air_vehicles_destroyed")},
        });
        y += 5;

        y = drawPanel(gui, leftX, y, panelW, "\u2316 WEAPONS", new String[][]{
            {"Shots Fired", intVal(st, "shots_fired"), "Shots Hit", intVal(st, "shots_hit")},
            {"Accuracy", formatAccuracy(st), "Damage", doubleVal(st, "damage_dealt")},
            {"Healing", doubleVal(st, "healing_done"), "Supplies", intVal(st, "supplies_delivered")},
            {"Longest Kill", doubleVal(st, "longest_kill") + "m", "", ""},
        });
        y += 5;

        y = drawPanel(gui, leftX, y, panelW, "\u2691 MATCHES", new String[][]{
            {"Played", intVal(st, "matches_played"), "Wins", intVal(st, "wins")},
            {"Losses", intVal(st, "losses"), "WinRate", formatWins(st)},
            {"Playtime", formatPlaytime(st), "Kills/Match", formatKpg(st)},
            {"Best WinStreak", intVal(st, "best_win_streak"), "Curr. Streak", intVal(st, "current_win_streak")},
            {"MVP", intVal(st, "match_mvp_count"), "", ""},
        });

        scrollMax = Math.max(0, y + 20 - clipY - clipH);
    }

    private int drawPanel(GuiGraphics gui, int x, int y, int w, String title, String[][] rows) {
        int titleH = 14;
        int rowH = 11;
        int pad = 4;
        int h = titleH + rows.length * rowH + pad;

        gui.fill(x, y, x + w, y + h, PWPTheme.Colors.SURFACE);
        gui.fill(x, y, x + w, y + 1, PWPTheme.Colors.BORDER);
        gui.fill(x, y + h - 1, x + w, y + h, PWPTheme.Colors.BORDER);
        gui.fill(x, y, x + 1, y + h, PWPTheme.Colors.BORDER);
        gui.fill(x + w - 1, y, x + w, y + h, PWPTheme.Colors.BORDER);

        gui.fill(x + 1, y + 1, x + w - 1, y + titleH + 1, PWPTheme.Colors.SURFACE_LIGHT);
        gui.drawString(font, "\u00a7e" + title, x + 6, y + 3, 0xFFFFFF);

        int ry = y + titleH + pad / 2;
        int col2X = x + w / 2;
        int valX1 = x + 85;
        int valX2 = col2X + 85;
        int maxTextW = col2X - x - 90;

        for (String[] row : rows) {
            if (!row[0].isEmpty()) {
                String label = "\u00a77" + row[0] + ":";
                gui.drawString(font, label, x + 6, ry, 0x7A7D84);
                String val = "\u00a7f" + truncateText(row[1], maxTextW);
                gui.drawString(font, val, valX1, ry, 0xFFFFFF);
            }
            if (!row[2].isEmpty()) {
                String label = "\u00a77" + row[2] + ":";
                gui.drawString(font, label, col2X + 4, ry, 0x7A7D84);
                String val = "\u00a7f" + truncateText(row[3], maxTextW);
                gui.drawString(font, val, valX2, ry, 0xFFFFFF);
            }
            ry += rowH;
        }
        return y + h;
    }

    private String truncateText(String text, int maxW) {
        if (font.width(text) > maxW) {
            return font.plainSubstrByWidth(text, maxW - 4) + "...";
        }
        return text;
    }

    // ====== LEADERBOARD TAB ======

    private void renderLeaderboard(GuiGraphics gui, int mx, int my, int clipY, int clipH) {
        int leftX = Math.max(10, width / 2 - 180);
        int w = 360;
        int y = clipY + 4 - (int) scrollOffset;

        catBounds.clear();

        catBounds.add(new int[]{leftX - 10, 0, 0, 0, -1});
        int catX = leftX;
        int catY = y;
        for (int i = 0; i < LB_CATEGORIES.length; i++) {
            int bw = font.width(LB_CATEGORY_NAMES[i]) + 10;
            boolean isSel = lbOrderBy.equals(LB_CATEGORIES[i]);
            boolean hover = mx >= catX && mx <= catX + bw && my >= catY && my <= catY + 16;
            int bg = isSel ? PWPTheme.Colors.ACCENT_DIM : (hover ? PWPTheme.Colors.SURFACE_LIGHT : PWPTheme.Colors.SURFACE);
            gui.fill(catX, catY, catX + bw, catY + 16, bg);
            if (isSel) {
                gui.fill(catX, catY, catX + bw, catY + 1, PWPTheme.Colors.ACCENT);
                gui.fill(catX, catY + 15, catX + bw, catY + 16, PWPTheme.Colors.ACCENT);
            }
            gui.drawString(font, (isSel ? "\u00a7e" : "\u00a77") + LB_CATEGORY_NAMES[i], catX + 5, catY + 4, 0xFFFFFF);
            catBounds.add(new int[]{catX, catY, bw, 16, i});
            catX += bw + 2;
            if (catX + 50 > leftX + w) { catX = leftX; catY += 18; }
        }

        y = catY + 18;

        int headerY = y;
        gui.fill(leftX, headerY, leftX + w, headerY + 1, PWPTheme.Colors.BORDER);
        gui.fill(leftX, headerY + 1, leftX + w, headerY + 13, PWPTheme.Colors.SURFACE_LIGHT);
        gui.drawString(font, "\u00a77#", leftX + 4, headerY + 3, 0x7A7D84);
        gui.drawString(font, "\u00a77Player", leftX + 26, headerY + 3, 0x7A7D84);
        gui.drawString(font, "\u00a77" + getColLabel(lbOrderBy), leftX + 190, headerY + 3, 0x7A7D84);
        gui.drawString(font, "\u00a77K/D", leftX + 270, headerY + 3, 0x7A7D84);
        gui.drawString(font, "\u00a77W/R", leftX + 310, headerY + 3, 0x7A7D84);

        int rowY = headerY + 15;
        int rankOffset = lbPage * 10;
        int rowH = 13;

        int totalH = rowY + lbEntries.size() * rowH + 20;
        scrollMax = Math.max(0, totalH - clipY - clipH);

        for (int i = 0; i < lbEntries.size(); i++) {
            LeaderboardEntry e = lbEntries.get(i);
            boolean isMe = e.uuid.equals(playerUuid);
            int rowBg = isMe ? 0xFF2A2010 : (i % 2 == 0 ? PWPTheme.Colors.SURFACE : 0xFF0E1117);
            gui.fill(leftX, rowY, leftX + w, rowY + rowH, rowBg);

            if (isMe) {
                gui.fill(leftX, rowY, leftX + w, rowY + 1, PWPTheme.Colors.ACCENT);
                gui.fill(leftX, rowY + rowH - 1, leftX + w, rowY + rowH, PWPTheme.Colors.ACCENT);
            }

            int rn = rankOffset + i + 1;
            String rankStr = rn == 1 ? "\u00a76#1" : rn == 2 ? "\u00a77#2" : rn == 3 ? "\u00a76#3" : "\u00a77#" + rn;
            gui.drawString(font, rankStr, leftX + 4, rowY + 3, 0xFFFFFF);

            String nameStr = e.nickname;
            if (font.width(nameStr) > 120) nameStr = font.plainSubstrByWidth(nameStr, 118) + "...";
            gui.drawString(font, (isMe ? "\u00a7e" : "\u00a7f") + nameStr, leftX + 26, rowY + 3, 0xFFFFFF);

            String colVal = truncateText(getColValue(e, lbOrderBy), 70);
            gui.drawString(font, "\u00a7f" + colVal, leftX + 190, rowY + 3, 0xFFFFFF);

            String kdStr = String.format("%.2f", e.kd);
            gui.drawString(font, "\u00a77" + kdStr, leftX + 270, rowY + 3, 0x7A7D84);

            String wrStr = String.format("%.1f%%", e.winRate);
            gui.drawString(font, "\u00a77" + wrStr, leftX + 310, rowY + 3, 0x7A7D84);

            if (isMe) {
                String meLabel = "\u00a7e\u25C0 you";
                int meW = font.width(meLabel);
                gui.drawString(font, meLabel, leftX + w - meW - 4, rowY + 3, PWPTheme.Colors.TEXT_ACCENT);
            }

            rowY += rowH;
        }
    }

    private String getColLabel(String orderBy) {
        for (int i = 0; i < LB_CATEGORIES.length; i++) {
            if (LB_CATEGORIES[i].equals(orderBy)) return LB_CATEGORY_NAMES[i];
        }
        return "Kills";
    }

    private String getColValue(LeaderboardEntry e, String col) {
        return switch (col) {
            case "kills" -> intStr(e.kills);
            case "deaths" -> intStr(e.deaths);
            case "assists" -> intStr(e.assists);
            case "kd" -> String.format("%.2f", e.kd);
            case "wins" -> intStr(e.wins);
            case "winrate" -> String.format("%.1f", e.winRate);
            case "vehicle_kills" -> intStr(e.vehicleKills);
            case "vehicles_destroyed" -> intStr(e.vehiclesDestroyed);
            case "air_destroyed" -> intStr(e.airVehiclesDestroyed);
            case "captures" -> intStr(e.captures);
            case "damage" -> intStr((int) e.damage);
            case "healing" -> intStr((int) e.healing);
            case "headshots" -> intStr(e.headshots);
            case "hub_destructions" -> intStr(e.hubDestructions);
            case "playtime" -> formatPlaytimeRaw(e.playtime);
            case "level" -> intStr(e.level);
            default -> intStr(e.kills);
        };
    }

    // ====== UTILITY ======

    private String intStr(int v) { return String.format("%,d", v); }

    private String intVal(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return "0";
        try { return String.format("%,d", obj.get(key).getAsInt()); } catch (Exception e) { return "0"; }
    }

    private String doubleVal(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return "0";
        try {
            double v = obj.get(key).getAsDouble();
            if (v >= 1000) return String.format("%,.0f", v);
            if (v == (long) v) return String.format("%,.0f", v);
            return String.format("%,.1f", v);
        } catch (Exception e) { return "0"; }
    }

    private String formatKd(JsonObject obj) {
        if (obj == null) return "0.00";
        int k = obj.has("kills") ? obj.get("kills").getAsInt() : 0;
        int d = obj.has("deaths") ? obj.get("deaths").getAsInt() : 0;
        if (d == 0) return String.format("%.2f", (double) k);
        return String.format("%.2f", (double) k / d);
    }

    private String formatAccuracy(JsonObject obj) {
        if (obj == null) return "0%";
        int f = obj.has("shots_fired") ? obj.get("shots_fired").getAsInt() : 0;
        int h = obj.has("shots_hit") ? obj.get("shots_hit").getAsInt() : 0;
        if (f == 0) return "0%";
        return String.format("%.1f%%", (double) h / f * 100);
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
        int m = obj.has("matches_played") ? obj.get("matches_played").getAsInt() : 0;
        if (m == 0) return "0.00";
        return String.format("%.2f", (double) k / m);
    }

    private String formatPlaytime(JsonObject obj) {
        if (obj == null || !obj.has("playtime_seconds")) return "0h";
        return formatPlaytimeRaw(obj.get("playtime_seconds").getAsLong());
    }

    private String formatPlaytimeRaw(long secs) {
        if (secs < 60) return secs + "s";
        long min = secs / 60;
        if (min < 60) return min + "m " + (secs % 60) + "s";
        long hours = min / 60;
        long mins = min % 60;
        if (hours < 24) return hours + "h " + mins + "m";
        long days = hours / 24;
        return days + "d " + (hours % 24) + "h";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ====== DATA ======

    private static class LeaderboardEntry {
        String uuid, nickname;
        int kills, deaths, assists, wins, vehicleKills, captures, headshots;
        int vehiclesDestroyed, airVehiclesDestroyed, hubDestructions;
        int level, matchesPlayed;
        long playtime;
        double damage, healing, kd, winRate;

        LeaderboardEntry(JsonObject obj) {
            uuid = get(obj, "uuid", "");
            nickname = get(obj, "nickname", "");
            kills = get(obj, "kills", 0);
            deaths = get(obj, "deaths", 0);
            assists = get(obj, "assists", 0);
            wins = get(obj, "wins", 0);
            vehicleKills = get(obj, "vehicle_kills", 0);
            captures = get(obj, "captures", 0);
            headshots = get(obj, "headshots", 0);
            vehiclesDestroyed = get(obj, "vehicles_destroyed", 0);
            airVehiclesDestroyed = get(obj, "air_vehicles_destroyed", 0);
            hubDestructions = get(obj, "hub_destructions", 0);
            level = get(obj, "level", 0);
            matchesPlayed = get(obj, "matches_played", 0);
            playtime = getLong(obj, "playtime_seconds");
            damage = getDouble(obj, "damage_dealt");
            healing = getDouble(obj, "healing_done");
            kd = deaths == 0 ? kills : Math.round((double) kills / deaths * 100.0) / 100.0;
            int totalGames = wins + get(obj, "losses", 0);
            winRate = totalGames == 0 ? 0 : Math.round((double) wins / totalGames * 1000.0) / 10.0;
        }

        private String get(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }
        private int get(JsonObject o, String k, int d) { return o.has(k) ? o.get(k).getAsInt() : d; }
        private long getLong(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsLong() : 0; }
        private double getDouble(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsDouble() : 0; }
    }
}