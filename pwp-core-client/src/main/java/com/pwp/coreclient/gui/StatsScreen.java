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
import java.util.UUID;

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

    private static final int TAB_MY_STATS = 0;
    private static final int TAB_LEADERBOARD = 1;

    private static final String[] LB_CATEGORIES = {
        "kills", "deaths", "assists", "kd", "wins", "winrate",
        "vehicle_kills", "vehicles_destroyed", "air_destroyed",
        "captures", "damage", "healing", "headshots",
        "hub_destructions", "playtime", "level"
    };
    private static final String[] LB_CATEGORY_NAMES = {
        "Kills", "Deaths", "Assists", "K/D", "Wins", "WinRate",
        "Vehicle Kills", "Vehicles Destr.", "Air Destr.",
        "Captures", "Damage", "Healing", "Headshots",
        "Hub Destr.", "Playtime", "Level"
    };

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
                    JsonObject p = players.get(i).getAsJsonObject();
                    lbEntries.add(new LeaderboardEntry(p));
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
                b -> { tab = TAB_MY_STATS; init(); })
                .bounds(cx - 160, 8, 80, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("Leaderboard"),
                b -> { tab = TAB_LEADERBOARD; fetchData(); init(); })
                .bounds(cx - 80, 8, 80, 20).build());

        addRenderableWidget(Button.builder(
                Component.literal("\u2715 Close"),
                b -> onClose())
                .bounds(cx - 50, height - 28, 100, 20).build());

        if (tab == TAB_LEADERBOARD) {
            addRenderableWidget(Button.builder(
                    Component.literal("\u25C0"),
                    b -> { if (lbPage > 0) { lbPage--; fetchLeaderboardPage(); init(); }})
                    .bounds(cx + 80, 32, 20, 20).build());
            addRenderableWidget(Button.builder(
                    Component.literal("\u25B6"),
                    b -> { if (lbPage < lbTotalPages - 1) { lbPage++; fetchLeaderboardPage(); init(); }})
                    .bounds(cx + 104, 32, 20, 20).build());
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

        if (tab == TAB_MY_STATS) {
            renderMyStats(gui, mx, my);
        } else {
            renderLeaderboard(gui, mx, my);
        }
    }

    private void renderMyStats(GuiGraphics gui, int mx, int my) {
        if (cachedPlayerStats == null) return;
        int cx = width / 2;
        int leftX = Math.max(10, cx - 180);
        int rightX = cx + 10;

        int playerData = cachedPlayerStats.has("player") ? 1 : 0;
        if (!cachedPlayerStats.has("stats")) playerData = 0;

        String nick = playerNickname;
        int level = 1;
        int prestige = 0;
        long coins = 0;
        long xp = 0;
        if (cachedPlayerStats.has("player") && cachedPlayerStats.getAsJsonObject("player").has("nickname")) {
            nick = cachedPlayerStats.getAsJsonObject("player").get("nickname").getAsString();
        }
        if (cachedPlayerStats.has("level")) level = cachedPlayerStats.get("level").getAsInt();
        if (cachedPlayerStats.has("prestige")) prestige = cachedPlayerStats.get("prestige").getAsInt();
        if (cachedPlayerStats.has("coins")) coins = cachedPlayerStats.get("coins").getAsLong();
        if (cachedPlayerStats.has("xp")) xp = cachedPlayerStats.get("xp").getAsLong();

        JsonObject st = cachedPlayerStats.has("stats") ? cachedPlayerStats.getAsJsonObject("stats") : cachedPlayerStats;

        int rank = -1;
        int total = 0;
        if (cachedRank != null) {
            rank = cachedRank.get("rank").getAsInt();
            total = cachedRank.get("total").getAsInt();
        }

        String rankStr = rank > 0 ? "\u00a7e#" + rank + "\u00a77 of " + total : "\u00a77--";
        gui.drawString(font, "\u00a7f" + nick + "  \u00a77Lv." + level + " \u00a7e" + "\u2726" + prestige, leftX, 58, 0xFFFFFF);
        gui.drawString(font, "\u00a77Rank: " + rankStr, leftX, 70, 0x7A7D84);

        int y = 90;

        // Panel 1: Combat
        y = drawPanel(gui, leftX, y, 350, "\u2694 BATTLE STATS", new String[][]{
            {"Kills", intVal(st, "kills"), "Deaths", intVal(st, "deaths")},
            {"K/D", formatKd(st), "Assists", intVal(st, "assists")},
            {"Vehicle Kills", intVal(st, "vehicle_kills"), "Captures", intVal(st, "captures")},
            {"Headshots", intVal(st, "headshots"), "Best KillStreak", intVal(st, "best_kill_streak")},
            {"TeamKills", intVal(st, "team_kills"), "Hub Destructions", intVal(st, "hub_destructions")},
            {"Base Defends", intVal(st, "base_defends"), "", ""},
        }) + 16;

        // Panel 2: Vehicles
        y = drawPanel(gui, leftX, y, 350, PWPTheme.Icons.STAR + " VEHICLES", new String[][]{
            {"Vehicles Destroyed", intVal(st, "vehicles_destroyed"), "Air Destroyed", intVal(st, "air_vehicles_destroyed")},
        }) + 16;

        // Panel 3: Accuracy / Damage
        y = drawPanel(gui, leftX, y, 350, PWPTheme.Icons.CROSSHAIR + " WEAPONS", new String[][]{
            {"Shots Fired", intVal(st, "shots_fired"), "Shots Hit", intVal(st, "shots_hit")},
            {"Accuracy", formatAccuracy(st), "Damage Dealt", doubleVal(st, "damage_dealt")},
            {"Healing Done", doubleVal(st, "healing_done"), "Supplies Delivered", intVal(st, "supplies_delivered")},
            {"Longest Kill", doubleVal(st, "longest_kill") + "m", "Distance", doubleVal(st, "distance_traveled") + "m"},
        }) + 16;

        // Panel 4: Matches
        y = drawPanel(gui, leftX, y, 350, PWPTheme.Icons.FLAG + " MATCHES", new String[][]{
            {"Matches Played", intVal(st, "matches_played"), "Wins", intVal(st, "wins")},
            {"Losses", intVal(st, "losses"), "WinRate", formatWins(st)},
            {"Playtime", formatPlaytime(st), "Kills/Match", formatKpg(st)},
            {"Best WinStreak", intVal(st, "best_win_streak"), "Current WinStreak", intVal(st, "current_win_streak")},
            {"MVP Count", intVal(st, "match_mvp_count"), "", ""},
        });
    }

    private int drawPanel(GuiGraphics gui, int x, int y, int w, String title, String[][] rows) {
        int titleH = 14;
        int rowH = 11;
        int h = titleH + rows.length * rowH + 8;

        int bgColor = PWPTheme.Colors.SURFACE;
        int borderColor = PWPTheme.Colors.BORDER;

        gui.fill(x, y, x + w, y + h, bgColor);
        gui.fill(x, y, x + w, y + 1, borderColor);
        gui.fill(x, y + h - 1, x + w, y + h, borderColor);
        gui.fill(x, y, x + 1, y + h, borderColor);
        gui.fill(x + w - 1, y, x + w, y + h, borderColor);

        gui.fill(x + 1, y + 1, x + w - 1, y + titleH + 1, PWPTheme.Colors.SURFACE_LIGHT);
        gui.drawString(font, "\u00a7e" + title, x + 6, y + 3, 0xFFFFFF);

        int ry = y + titleH + 4;
        for (String[] row : rows) {
            String leftLabel = row[0];
            String leftVal = row[1];
            String rightLabel = row[2];
            String rightVal = row[3];
            if (!leftLabel.isEmpty()) {
                gui.drawString(font, "\u00a77" + leftLabel + ":", x + 6, ry, 0x7A7D84);
                gui.drawString(font, "\u00a7f" + leftVal, x + 90, ry, 0xFFFFFF);
            }
            if (!rightLabel.isEmpty()) {
                int col2X = x + w / 2;
                gui.drawString(font, "\u00a77" + rightLabel + ":", col2X + 4, ry, 0x7A7D84);
                gui.drawString(font, "\u00a7f" + rightVal, col2X + 88, ry, 0xFFFFFF);
            }
            ry += rowH;
        }

        return y + h;
    }

    private void renderLeaderboard(GuiGraphics gui, int mx, int my) {
        int cx = width / 2;
        int leftX = Math.max(10, cx - 180);
        int w = 360;

        // Category selector
        int catX = leftX;
        int catY = 56;
        for (int i = 0; i < LB_CATEGORIES.length; i++) {
            int bw = font.width(LB_CATEGORY_NAMES[i]) + 8;
            boolean isSel = lbOrderBy.equals(LB_CATEGORIES[i]);
            int bg = isSel ? PWPTheme.Colors.ACCENT_DIM : PWPTheme.Colors.SURFACE;
            gui.fill(catX, catY, catX + bw, catY + 14, bg);
            if (isSel) {
                gui.fill(catX, catY, catX + bw, catY + 1, PWPTheme.Colors.ACCENT);
                gui.fill(catX, catY + 13, catX + bw, catY + 14, PWPTheme.Colors.ACCENT);
            }
            gui.drawString(font, (isSel ? "\u00a7e" : "\u00a77") + LB_CATEGORY_NAMES[i], catX + 4, catY + 3, 0xFFFFFF);
            catX += bw + 2;
            if (catX + 60 > leftX + w) { catX = leftX; catY += 16; }
        }

        catY += 4;
        String pageInfo = "\u00a77Page " + (lbPage + 1) + "/" + lbTotalPages + "  Total: \u00a7e" + lbTotal;
        gui.drawString(font, pageInfo, leftX, catY, 0x7A7D84);
        catY += 12;

        int headerY = catY;

        // Column headers
        gui.fill(leftX, headerY, leftX + w, headerY + 1, PWPTheme.Colors.BORDER);
        gui.fill(leftX, headerY + 1, leftX + w, headerY + 13, PWPTheme.Colors.SURFACE_LIGHT);
        gui.drawString(font, "\u00a77#", leftX + 4, headerY + 3, 0x7A7D84);
        gui.drawString(font, "\u00a77Player", leftX + 26, headerY + 3, 0x7A7D84);
        gui.drawString(font, "\u00a77" + getColLabel(lbOrderBy), leftX + 190, headerY + 3, 0x7A7D84);
        gui.drawString(font, "\u00a77K/D", leftX + 258, headerY + 3, 0x7A7D84);
        gui.drawString(font, "\u00a77W/R", leftX + 300, headerY + 3, 0x7A7D84);

        int rowY = headerY + 15;
        int rankOffset = lbPage * 10;

        for (int i = 0; i < lbEntries.size(); i++) {
            LeaderboardEntry e = lbEntries.get(i);
            boolean isMe = e.uuid.equals(playerUuid);
            int rowBg = isMe ? 0xFF2A2010 : (i % 2 == 0 ? PWPTheme.Colors.SURFACE : 0xFF0E1117);
            gui.fill(leftX, rowY, leftX + w, rowY + 12, rowBg);

            if (isMe) {
                gui.fill(leftX, rowY, leftX + w, rowY + 1, PWPTheme.Colors.ACCENT);
                gui.fill(leftX, rowY + 11, leftX + w, rowY + 12, PWPTheme.Colors.ACCENT);
            }

            String rankStr = "\u00a77#" + (rankOffset + i + 1);
            if (rankOffset + i + 1 == 1) rankStr = "\u00a76\u265B #1";
            else if (rankOffset + i + 1 == 2) rankStr = "\u00a77\u265B #2";
            else if (rankOffset + i + 1 == 3) rankStr = "\u00a76\u265B #3";

            gui.drawString(font, rankStr, leftX + 4, rowY + 2, 0xFFFFFF);
            String nameStr = e.nickname;
            if (font.width(nameStr) > 100) nameStr = font.plainSubstrByWidth(nameStr, 98) + "...";
            gui.drawString(font, (isMe ? "\u00a7e" : "\u00a7f") + nameStr, leftX + 26, rowY + 2, 0xFFFFFF);

            String colVal = getColValue(e, lbOrderBy);
            gui.drawString(font, "\u00a7f" + colVal, leftX + 190, rowY + 2, 0xFFFFFF);
            gui.drawString(font, "\u00a77" + e.kd, leftX + 258, rowY + 2, 0x7A7D84);
            gui.drawString(font, "\u00a77" + e.winRate + "%", leftX + 300, rowY + 2, 0x7A7D84);

            if (isMe) {
                String meLabel = "\u00a7e\u25C0 you";
                gui.drawString(font, meLabel, leftX + w - font.width(meLabel) - 4, rowY + 2, PWPTheme.Colors.TEXT_ACCENT);
            }

            rowY += 12;
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

    private String intStr(int v) { return String.format("%,d", v); }

    // Utility methods
    private String intVal(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return "0";
        try { return String.format("%,d", obj.get(key).getAsInt()); } catch (Exception e) { return "0"; }
    }

    private String doubleVal(JsonObject obj, String key) {
        if (obj == null || !obj.has(key)) return "0";
        try {
            double v = obj.get(key).getAsDouble();
            if (v == (long) v) return String.format("%,d", (long) v);
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
        long secs = obj.get("playtime_seconds").getAsLong();
        return formatPlaytimeRaw(secs);
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

    // Data holder
    private static class LeaderboardEntry {
        String uuid, nickname;
        int kills, deaths, assists, wins, vehicleKills, captures, headshots;
        int vehiclesDestroyed, airVehiclesDestroyed, hubDestructions;
        int level, matchesPlayed;
        long playtime;
        double damage, healing, kd, winRate;

        LeaderboardEntry(JsonObject obj) {
            uuid = getStr(obj, "uuid");
            nickname = getStr(obj, "nickname");
            kills = getInt(obj, "kills");
            deaths = getInt(obj, "deaths");
            assists = getInt(obj, "assists");
            wins = getInt(obj, "wins");
            vehicleKills = getInt(obj, "vehicle_kills");
            captures = getInt(obj, "captures");
            headshots = getInt(obj, "headshots");
            vehiclesDestroyed = getInt(obj, "vehicles_destroyed");
            airVehiclesDestroyed = getInt(obj, "air_vehicles_destroyed");
            hubDestructions = getInt(obj, "hub_destructions");
            level = getInt(obj, "level");
            matchesPlayed = getInt(obj, "matches_played");
            playtime = getLong(obj, "playtime_seconds");
            damage = getDouble(obj, "damage_dealt");
            healing = getDouble(obj, "healing_done");
            kd = deaths == 0 ? kills : Math.round((double) kills / deaths * 100.0) / 100.0;
            int totalGames = wins + getInt(obj, "losses");
            winRate = totalGames == 0 ? 0 : Math.round((double) wins / totalGames * 1000.0) / 10.0;
        }

        private String getStr(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsString() : ""; }
        private int getInt(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsInt() : 0; }
        private long getLong(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsLong() : 0; }
        private double getDouble(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsDouble() : 0; }
    }
}