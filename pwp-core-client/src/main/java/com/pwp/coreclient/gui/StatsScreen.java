package com.pwp.coreclient.gui;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.coreclient.PlayerData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.ClientResponseCache;
import com.pwp.coreclient.network.PacketDataRequest;
import com.pwp.coreclient.network.PacketHandler;
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
    private UUID playerUuidObj;
    private String playerNickname;

    private JsonObject cachedPlayerStats;
    private boolean loading = true;
    private String errorMsg = null;
    private boolean profileRequested = false;
    private boolean leaderboardRequested = false;

    private double scrollOffset = 0;
    private double scrollMax = 0;

    private static final int TAB_MY_STATS = 0;
    private static final int TAB_LEADERBOARD = 1;

    private static final int CONTENT_TOP = 54;
    private static final int CONTENT_BOTTOM_OFFSET = 36;

    private static final String[] LB_CATEGORIES = {
        "kills", "deaths", "kd", "wins", "winrate",
        "vehicle_kills", "vehicles_destroyed", "air_destroyed",
        "captures", "damage", "healing", "headshots",
        "playtime", "level"
    };
    private static final String[] LB_CATEGORY_NAMES = {
        "Убийства", "Смерти", "K/D", "Победы", "WinRate",
        "Техника", "Уничтожено", "Авиация",
        "Захваты", "Урон", "Лечение", "Голова",
        "Наиграно", "Уровень"
    };

    private List<int[]> catBounds = new ArrayList<>();

    public StatsScreen() {
        super(Component.literal("СТАТИСТИКА"));
        Player p = Minecraft.getInstance().player;
        if (p != null) {
            playerUuid = p.getStringUUID();
            playerUuidObj = p.getUUID();
            playerNickname = p.getScoreboardName();
        }
        requestProfile();
    }

    private void requestProfile() {
        loading = true;
        errorMsg = null;
        scrollOffset = 0;
        profileRequested = true;
        PacketHandler.INSTANCE.sendToServer(new PacketDataRequest("profile", ""));
    }

    public void requestLeaderboard() {
        leaderboardRequested = false;
        ClientResponseCache.leaderboardData = null;
        String params = "{\"orderBy\":\"" + lbOrderBy + "\",\"page\":" + (lbPage + 1) + "}";
        PacketHandler.INSTANCE.sendToServer(new PacketDataRequest("leaderboard", params));
    }

    @Override
    public void tick() {
        if (profileRequested && playerUuidObj != null) {
            PlayerData.CachedProfile profile = PlayerData.get(playerUuidObj);
            if (profile != null && profile.data != null) {
                cachedPlayerStats = profile.data;
                profileRequested = false;
                if (tab == TAB_MY_STATS) loading = false;
            }
        }
        if (tab == TAB_LEADERBOARD && !leaderboardRequested && ClientResponseCache.leaderboardData != null) {
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
        int cx = width / 2;

        addRenderableWidget(Button.builder(
                Component.literal("Моя статистика"),
                b -> { tab = TAB_MY_STATS; scrollOffset = 0; init(); })
                .bounds(cx - 160, 6, 80, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("Лидеры"),
                b -> { tab = TAB_LEADERBOARD; scrollOffset = 0; loading = true; requestLeaderboard(); init(); })
                .bounds(cx - 76, 6, 90, 22).build());

        addRenderableWidget(Button.builder(
                Component.literal("\u2715 Закрыть"),
                b -> onClose())
                .bounds(cx - 50, height - 28, 100, 22).build());

        if (tab == TAB_LEADERBOARD && lbTotalPages > 1) {
            int pageY = 30;
            addRenderableWidget(Button.builder(
                    Component.literal("\u25C0"),
                    b -> { if (lbPage > 0) { lbPage--; scrollOffset = 0; loading = true; requestLeaderboard(); init(); }})
                    .bounds(cx + 80, pageY, 20, 18).build());
            addRenderableWidget(Button.builder(
                    Component.literal("\u25B6"),
                    b -> { if (lbPage < lbTotalPages - 1) { lbPage++; scrollOffset = 0; loading = true; requestLeaderboard(); init(); }})
                    .bounds(cx + 104, pageY, 20, 18).build());
            addRenderableWidget(Button.builder(
                    Component.literal((lbPage + 1) + "/" + lbTotalPages),
                    b -> {})
                    .bounds(cx + 52, pageY, 26, 18).build());
        }
    }

    public int getStatsTab() { return tab; }
    public void setStatsTab(int t) { tab = t; }

    public void setPanelSize(int w, int h) {
        this.width = w;
        this.height = h;
        if (this.minecraft == null) {
            this.minecraft = Minecraft.getInstance();
        }
        if (this.font == null && this.minecraft != null) {
            this.font = this.minecraft.font;
        }
    }

    public boolean isLbPagePrevEnabled() { return lbPage > 0; }
    public boolean isLbPageNextEnabled() { return lbPage < lbTotalPages - 1; }
    public int getLbPage() { return lbPage; }
    public int getLbTotalPages() { return lbTotalPages; }
    public void lbPagePrev() { if (lbPage > 0) { lbPage--; scrollOffset = 0; loading = true; requestLeaderboard(); } }
    public void lbPageNext() { if (lbPage < lbTotalPages - 1) { lbPage++; scrollOffset = 0; loading = true; requestLeaderboard(); } }

    public void renderContent(GuiGraphics gui, int mx, int my, int clipY, int clipH) {
        int cx = width / 2;

        if (loading) {
            gui.drawString(PWPTheme.Fonts.display(), "\u00a77Загрузка...", cx - PWPTheme.Fonts.display().width("\u00a77Загрузка...")/2, clipY + 40, PWPTheme.Colors.TEXT_SECONDARY, false);
            return;
        }
        if (errorMsg != null) {
            gui.drawString(PWPTheme.Fonts.display(), "\u00a7c" + errorMsg, cx - PWPTheme.Fonts.display().width("\u00a7c" + errorMsg)/2, clipY + 40, 0xFF5555, false);
            return;
        }

        if (cachedPlayerStats == null && tab == TAB_MY_STATS) return;

        if (tab == TAB_MY_STATS) {
            renderMyStats(gui, mx, my, clipY, clipH);
        } else {
            renderLeaderboard(gui, mx, my, clipY, clipH);
        }

        if (scrollMax > 0) {
            int sbY = clipY + 2;
            int sbH = clipH - 4;
            int sbX = width - 7;
            int barH = Math.max(12, (int) (sbH * clipH / (clipH + scrollMax)));
            int barY = sbY + (int) ((scrollOffset / scrollMax) * (sbH - barH));
            gui.fill(sbX, sbY, sbX + 5, sbY + sbH, PWPTheme.Styles.ScrollBar.TRACK);
            gui.fill(sbX, barY, sbX + 5, barY + barH, PWPTheme.Styles.ScrollBar.THUMB);
            gui.fill(sbX, barY, sbX + 4, barY + barH - 1, PWPTheme.Styles.ScrollBar.THUMB_HOVER);
        }
    }

    public boolean mouseClickedContent(double mx, double my, int btn) {
        if (btn == 0 && tab == TAB_LEADERBOARD) {
            for (int[] b : catBounds) {
                if (mx >= b[0] && mx <= b[0] + b[2] && my >= b[1] && my <= b[1] + b[3]) {
                    String cat = LB_CATEGORIES[b[4]];
                    if (!lbOrderBy.equals(cat)) {
                        lbOrderBy = cat;
                        lbPage = 0;
                        scrollOffset = 0;
                        loading = true;
                        requestLeaderboard();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void render(GuiGraphics gui, int mx, int my, float pt) {
        renderBackground(gui);
        super.render(gui, mx, my, pt);

        int cx = width / 2;
        gui.drawString(PWPTheme.Fonts.display(), Component.literal("СТАТИСТИКА"), cx - PWPTheme.Fonts.display().width(Component.literal("СТАТИСТИКА").getString())/2, 32, PWPTheme.Colors.TEXT_ACCENT, false);
        gui.fill(cx - 55, 42, cx + 55, 43, PWPTheme.Colors.ACCENT);

        if (loading) {
            gui.drawString(PWPTheme.Fonts.display(), "\u00a77Загрузка...", cx - PWPTheme.Fonts.display().width("\u00a77Загрузка...")/2, height / 2, PWPTheme.Colors.TEXT_SECONDARY, false);
            return;
        }
        if (errorMsg != null) {
            gui.drawString(PWPTheme.Fonts.display(), "\u00a7c" + errorMsg, cx - PWPTheme.Fonts.display().width("\u00a7c" + errorMsg)/2, height / 2, 0xFF5555, false);
            return;
        }

        if (cachedPlayerStats == null && tab == TAB_MY_STATS) return;

        int clipY = CONTENT_TOP;
        int clipH = height - CONTENT_TOP - CONTENT_BOTTOM_OFFSET;

        if (tab == TAB_MY_STATS) {
            renderMyStats(gui, mx, my, clipY, clipH);
        } else {
            renderLeaderboard(gui, mx, my, clipY, clipH);
        }

        if (scrollMax > 0) {
            int sbY = clipY + 2;
            int sbH = clipH - 4;
            int sbX = width - 7;
            int barH = Math.max(12, (int) (sbH * clipH / (clipH + scrollMax)));
            int barY = sbY + (int) ((scrollOffset / scrollMax) * (sbH - barH));
            gui.fill(sbX, sbY, sbX + 5, sbY + sbH, PWPTheme.Styles.ScrollBar.TRACK);
            gui.fill(sbX, barY, sbX + 5, barY + barH, PWPTheme.Styles.ScrollBar.THUMB);
            gui.fill(sbX, barY, sbX + 4, barY + barH - 1, PWPTheme.Styles.ScrollBar.THUMB_HOVER);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (delta != 0) {
            scrollOffset -= delta * 12;
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
                        loading = true;
                        requestLeaderboard();
                        init();
                    }
                    return true;
                }
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

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

        int y = clipY + 4 - (int) scrollOffset;

        gui.fill(leftX, y, leftX + panelW, y + 26, PWPTheme.Colors.SURFACE_LIGHT);
        gui.fill(leftX, y + 26, leftX + panelW, y + 27, PWPTheme.Colors.BORDER);
        gui.drawString(PWPTheme.Fonts.display(), "\u00a7f" + nick + "  \u00a77Lv." + level + " \u00a7e\u2726" + prestige, leftX + 8, y + 4, 0xFFFFFF, false);
        gui.drawString(PWPTheme.Fonts.display(), "\u00a77Матчи: " + intVal(st, "matchesPlayed"), leftX + 8, y + 14, PWPTheme.Colors.TEXT_SECONDARY, false);
        y += 30;

        y = drawPanel(gui, leftX, y, panelW, "БОЙ", new String[][]{
            {"Убийства", intVal(st, "kills"), "Смерти", intVal(st, "deaths")},
            {"K/D", formatKd(st), "Спасения", intVal(st, "revives")},
            {"Убито техникой", intVal(st, "vehicleKills"), "Захваты", intVal(st, "captures")},
            {"Лучшая серия", intVal(st, "bestKillStreak"), "Тимкиллы", intVal(st, "teamKills")},
        });
        y += 6;

        y = drawPanel(gui, leftX, y, panelW, "ТЕХНИКА", new String[][]{
            {"Уничтожено", intVal(st, "vehiclesDestroyed"), "Воздух", intVal(st, "airVehiclesDestroyed")},
        });
        y += 6;

        y = drawPanel(gui, leftX, y, panelW, "ОРУЖИЕ", new String[][]{
            {"Урон", doubleVal(st, "damageDealt"), "Лечение", doubleVal(st, "healingDone")},
            {"Припасы", intVal(st, "suppliesDelivered"), "Дальнее убийство", doubleVal(st, "longestKill") + "м"},
        });
        y += 6;

        int matchesBottom = drawPanel(gui, leftX, y, panelW, "МАТЧИ", new String[][]{
            {"Сыграно", intVal(st, "matchesPlayed"), "Победы", intVal(st, "wins")},
            {"Поражения", intVal(st, "losses"), "WinRate", formatWins(st)},
            {"Время", formatPlaytime(st), "Уб./матч", formatKpg(st)},
            {"Лучшая серия побед", intVal(st, "bestWinStreak"), "Тек. серия", intVal(st, "currentWinStreak")},
        });
        scrollMax = Math.max(0, matchesBottom + (int) scrollOffset + 12 - clipY - clipH);
    }

    private int drawPanel(GuiGraphics gui, int x, int y, int w, String title, String[][] rows) {
        int titleH = 16;
        int rowH = 12;
        int pad = 6;
        int h = titleH + rows.length * rowH + pad;
        var font = PWPTheme.Fonts.display();

        gui.fill(x, y, x + w, y + h, PWPTheme.Styles.Panel.BG);
        gui.fill(x, y, x + w, y + 1, PWPTheme.Styles.Panel.BORDER);
        gui.fill(x, y + h - 1, x + w, y + h, PWPTheme.Styles.Panel.BORDER);
        gui.fill(x, y, x + 1, y + h, PWPTheme.Styles.Panel.BORDER);
        gui.fill(x + w - 1, y, x + w, y + h, PWPTheme.Styles.Panel.BORDER);

        gui.fill(x + 1, y + 1, x + w - 1, y + titleH + 1, PWPTheme.Colors.SURFACE_LIGHT);
        gui.drawString(font, "\u00a7e" + title, x + 8, y + 4, PWPTheme.Colors.TEXT_ACCENT, false);

        int ry = y + titleH + pad / 2;
        int col2X = x + w / 2;
        int gap = 8;
        int rightEdge = x + w - 4;

        for (String[] row : rows) {
            if (!row[0].isEmpty()) {
                String label1 = "\u00a77" + row[0] + ":";
                int labelW1 = font.width(label1);
                int valX1 = x + 8 + labelW1 + gap;
                if (valX1 + font.width(row[1]) > col2X - 4 && labelW1 > 40) {
                    label1 = "\u00a77" + font.plainSubstrByWidth(row[0] + ":", 36) + "..:";
                    valX1 = x + 8 + font.width(label1) + gap;
                }
                gui.drawString(font, label1, x + 8, ry, PWPTheme.Colors.TEXT_SECONDARY, false);
                String val = "\u00a7f" + truncateText(formatNum(row[1]), Math.max(30, col2X - valX1 - 4));
                gui.drawString(font, val, valX1, ry, PWPTheme.Colors.TEXT_PRIMARY, false);
            }
            if (!row[2].isEmpty()) {
                String label2 = "\u00a77" + row[2] + ":";
                int labelW2 = font.width(label2);
                int valX2 = col2X + 6 + labelW2 + gap;
                if (valX2 + font.width(row[3]) > rightEdge && labelW2 > 40) {
                    label2 = "\u00a77" + font.plainSubstrByWidth(row[2] + ":", 36) + "..:";
                    valX2 = col2X + 6 + font.width(label2) + gap;
                }
                gui.drawString(font, "\u00a77" + label2, col2X + 6, ry, PWPTheme.Colors.TEXT_SECONDARY, false);
                String val = "\u00a7f" + truncateText(formatNum(row[3]), Math.max(30, rightEdge - valX2));
                gui.drawString(font, val, valX2, ry, PWPTheme.Colors.TEXT_PRIMARY, false);
            }
            ry += rowH;
        }
        return y + h;
    }

    private String formatNum(String raw) {
        try {
            double d = Double.parseDouble(raw);
            if (d >= 1000000) return String.format("%.1fM", d / 1000000);
            if (d >= 1000) return String.format("%.1fK", d / 1000);
            if (d == (long) d) return String.valueOf((long) d);
            return raw;
        } catch (NumberFormatException e) {
            return raw;
        }
    }

    private String truncateText(String text, int maxW) {
        if (PWPTheme.Fonts.display().width(text) > maxW) {
            return PWPTheme.Fonts.display().plainSubstrByWidth(text, maxW - 4) + "...";
        }
        return text;
    }

    private void renderLeaderboard(GuiGraphics gui, int mx, int my, int clipY, int clipH) {
        int leftX = Math.max(10, width / 2 - 180);
        int w = 360;
        int y = clipY + 4 - (int) scrollOffset;

        catBounds.clear();
        catBounds.add(new int[]{leftX - 10, 0, 0, 0, -1});

        int catX = leftX;
        int catY = y;
        for (int i = 0; i < LB_CATEGORIES.length; i++) {
            int bw = PWPTheme.Fonts.display().width(LB_CATEGORY_NAMES[i]) + 14;
            boolean isSel = lbOrderBy.equals(LB_CATEGORIES[i]);
            boolean hover = mx >= catX && mx <= catX + bw && my >= catY && my <= catY + 18;
            int bg = isSel ? PWPTheme.Colors.ACCENT_DIM : (hover ? PWPTheme.Colors.SURFACE_LIGHT : PWPTheme.Colors.SURFACE);
            gui.fill(catX, catY, catX + bw, catY + 18, bg);

            int borderCol = isSel ? PWPTheme.Colors.ACCENT : (hover ? PWPTheme.Colors.BORDER_FOCUS : PWPTheme.Colors.BORDER);
            gui.fill(catX, catY, catX + bw, catY + 1, borderCol);
            gui.fill(catX, catY + 17, catX + bw, catY + 18, borderCol);
            gui.fill(catX, catY, catX + 1, catY + 18, borderCol);
            gui.fill(catX + bw - 1, catY, catX + bw, catY + 18, borderCol);

            String textColor = isSel ? "\u00a7e" : (hover ? "\u00a7f" : "\u00a77");
            gui.drawString(PWPTheme.Fonts.display(), textColor + LB_CATEGORY_NAMES[i], catX + 6, catY + 5, PWPTheme.Colors.TEXT_PRIMARY, false);
            catBounds.add(new int[]{catX, catY, bw, 18, i});
            catX += bw + 4;
            if (catX + 60 > leftX + w) { catX = leftX; catY += 20; }
        }

        y = catY + 22;

        int headerY = y;
        gui.fill(leftX, headerY, leftX + w, headerY + 1, PWPTheme.Colors.BORDER);
        gui.fill(leftX, headerY + 1, leftX + w, headerY + 14, PWPTheme.Colors.SURFACE_LIGHT);
        gui.drawString(PWPTheme.Fonts.display(), "\u00a77#", leftX + 6, headerY + 4, PWPTheme.Colors.TEXT_SECONDARY, false);
        gui.drawString(PWPTheme.Fonts.display(), "\u00a77Игрок", leftX + 28, headerY + 4, PWPTheme.Colors.TEXT_SECONDARY, false);
        gui.drawString(PWPTheme.Fonts.display(), "\u00a77" + getColLabel(lbOrderBy), leftX + 190, headerY + 4, PWPTheme.Colors.TEXT_SECONDARY, false);
        gui.drawString(PWPTheme.Fonts.display(), "\u00a77K/D", leftX + 270, headerY + 4, PWPTheme.Colors.TEXT_SECONDARY, false);
        gui.drawString(PWPTheme.Fonts.display(), "\u00a77WinRate", leftX + 310, headerY + 4, PWPTheme.Colors.TEXT_SECONDARY, false);

        int rowY = headerY + 16;
        int rankOffset = lbPage * 10;
        int rowH = 14;

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
            gui.drawString(PWPTheme.Fonts.display(), rankStr, leftX + 6, rowY + 3, PWPTheme.Colors.TEXT_PRIMARY, false);

            String nameStr = e.nickname;
            if (PWPTheme.Fonts.display().width(nameStr) > 120) nameStr = PWPTheme.Fonts.display().plainSubstrByWidth(nameStr, 118) + "...";
            gui.drawString(PWPTheme.Fonts.display(), (isMe ? "\u00a7e" : "\u00a7f") + nameStr, leftX + 28, rowY + 3, PWPTheme.Colors.TEXT_PRIMARY, false);

            String colVal = truncateText(getColValue(e, lbOrderBy), 70);
            gui.drawString(PWPTheme.Fonts.display(), "\u00a7f" + colVal, leftX + 190, rowY + 3, PWPTheme.Colors.TEXT_PRIMARY, false);

            String kdStr = String.format("%.2f", e.kd);
            gui.drawString(PWPTheme.Fonts.display(), "\u00a77" + kdStr, leftX + 270, rowY + 3, PWPTheme.Colors.TEXT_SECONDARY, false);

            String wrStr = String.format("%.1f%%", e.winRate);
            gui.drawString(PWPTheme.Fonts.display(), "\u00a77" + wrStr, leftX + 310, rowY + 3, PWPTheme.Colors.TEXT_SECONDARY, false);

            if (isMe) {
                String meLabel = "\u00a7e\u25C0 вы";
                int meW = PWPTheme.Fonts.display().width(meLabel);
                gui.drawString(PWPTheme.Fonts.display(), meLabel, leftX + w - meW - 6, rowY + 3, PWPTheme.Colors.TEXT_ACCENT, false);
            }

            rowY += rowH;
        }
        scrollMax = Math.max(0, rowY + (int) scrollOffset + 12 - clipY - clipH);
    }

    private String getColLabel(String orderBy) {
        for (int i = 0; i < LB_CATEGORIES.length; i++) {
            if (LB_CATEGORIES[i].equals(orderBy)) return LB_CATEGORY_NAMES[i];
        }
        return "Убийства";
    }

    private String getColValue(LeaderboardEntry e, String col) {
        return switch (col) {
            case "kills" -> intStr(e.kills);
            case "deaths" -> intStr(e.deaths);
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
            case "playtime" -> formatPlaytimeRaw(e.playtime);
            case "level" -> intStr(e.level);
            default -> intStr(e.kills);
        };
    }

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
        return formatPlaytimeRaw(obj.get("playtimeSeconds").getAsLong());
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

    private static class LeaderboardEntry {
        String uuid, nickname;
        int kills, deaths, wins, vehicleKills, captures, headshots;
        int vehiclesDestroyed, airVehiclesDestroyed;
        int level, matchesPlayed;
        long playtime;
        double damage, healing, kd, winRate;

        LeaderboardEntry(JsonObject obj) {
            JsonObject p = obj.has("player") ? obj.getAsJsonObject("player") : obj;
            JsonObject st = obj.has("stats") ? obj.getAsJsonObject("stats") : obj;
            uuid = get(p, "uuid", "");
            nickname = get(p, "nickname", "");
            kills = get(st, "kills", 0);
            deaths = get(st, "deaths", 0);
            wins = get(st, "wins", 0);
            vehicleKills = get(st, "vehicleKills", 0);
            captures = get(st, "captures", 0);
            headshots = get(st, "headshots", 0);
            vehiclesDestroyed = get(st, "vehiclesDestroyed", 0);
            airVehiclesDestroyed = get(st, "airVehiclesDestroyed", 0);
            level = get(obj, "level", 0);
            matchesPlayed = get(st, "matchesPlayed", 0);
            playtime = getLong(st, "playtimeSeconds");
            damage = getDouble(st, "damageDealt");
            healing = getDouble(st, "healingDone");
            int totalGames = wins + get(st, "losses", 0);
            kd = deaths == 0 ? kills : Math.round((double) kills / deaths * 100.0) / 100.0;
            winRate = totalGames == 0 ? 0 : Math.round((double) wins / totalGames * 1000.0) / 10.0;
        }

        private String get(JsonObject o, String k, String d) { return o.has(k) ? o.get(k).getAsString() : d; }
        private int get(JsonObject o, String k, int d) { return o.has(k) ? o.get(k).getAsInt() : d; }
        private long getLong(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsLong() : 0; }
        private double getDouble(JsonObject o, String k) { return o.has(k) ? o.get(k).getAsDouble() : 0; }
    }
}
