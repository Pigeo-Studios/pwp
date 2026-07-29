package com.pwp.coreclient.gui.screens.tabs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pwp.coreclient.gui.components.PWPScrollPanel;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.ClientResponseCache;
import com.pwp.coreclient.network.PacketDataRequest;
import com.pwp.coreclient.network.PacketHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.*;

public class LeaderTabRenderer {

    public enum State { LOADING, LOADED }

    private static final String[] CATS = { "SCORE", "УБИЙСТВА", "K/D", "ПОБЕДЫ", "WINRATE", "ЗАХВАТЫ", "УРОН", "ЛЕЧЕНИЕ", "НАИГРАНО" };
    private static final int CAT_PADDING = 14;

    private static class Entry implements Comparable<Entry> {
        String name, uuid; int kills, deaths, wins, losses, captures, revives, vehicleKills;
        int damage, healing, score, playtimeSec, kd100, wr100;
        int catVal;

        Entry(JsonObject json) {
            JsonObject p = json.has("player") ? json.getAsJsonObject("player") : json;
            JsonObject s = json.has("stats") ? json.getAsJsonObject("stats") : json;
            name = p.has("nickname") ? p.get("nickname").getAsString() : "—";
            uuid = p.has("uuid") ? p.get("uuid").getAsString() : "";
            kills =          getInt(s, "kills", 0);
            deaths =         getInt(s, "deaths", 0);
            wins =           getInt(s, "wins", 0);
            losses =         getInt(s, "losses", 0);
            captures =       getInt(s, "captures", 0);
            revives =        getInt(s, "revives", 0);
            vehicleKills =   getInt(s, "vehicleKills", 0);
            damage =         getInt(s, "damageDealt", 0);
            healing =        getInt(s, "healingDone", 0);
            playtimeSec =    getInt(s, "playtimeSeconds", 0);
            score = kills*100 + vehicleKills*150 + captures*200 + revives*75 + healing;
            kd100 = deaths > 0 ? (int)(kills*1000f/deaths) : (kills*1000);
            wr100 = (wins+losses) > 0 ? (int)(wins*10000f/(wins+losses)) : 0;
        }
        public int compareTo(Entry o) { return Integer.compare(o.catVal, this.catVal); }

        private static int getInt(JsonObject o, String k, int def) {
            return o.has(k) ? o.get(k).getAsInt() : def;
        }
    }

    private State state = State.LOADING;
    private final List<Entry> entries = new ArrayList<>();
    private int sortCat = 0;
    private int hoveredPill = -1, hoveredRow = -1;
    private static final int ROW_H = 16;

    private final PWPScrollPanel scrollPanel = new PWPScrollPanel(20, 100, 200, 200);
    private int pillY;

    public void requestData(String orderBy, int page) {
        state = State.LOADING;
        entries.clear();
        PacketHandler.INSTANCE.sendToServer(
            new PacketDataRequest("leaderboard",
                "{\"orderBy\":\"" + orderBy + "\",\"page\":" + page + ",\"limit\":100}"));
    }

    public void tick() {
        if (state != State.LOADING) return;
        JsonObject data = ClientResponseCache.leaderboardData;
        if (data == null) return;
        JsonArray players = data.getAsJsonArray("players");
        entries.clear();
        for (JsonElement e : players) entries.add(new Entry(e.getAsJsonObject()));
        sortBy(0);
        state = State.LOADED;
        ClientResponseCache.leaderboardData = null;
    }

    private void sortBy(int cat) {
        sortCat = cat;
        for (Entry e : entries) {
            e.catVal = switch (cat) {
                case 0 -> e.score; case 1 -> e.kills; case 2 -> e.kd100;
                case 3 -> e.wins; case 4 -> e.wr100; case 5 -> e.captures;
                case 6 -> e.damage; case 7 -> e.healing; case 8 -> e.playtimeSec;
                default -> e.score;
            };
        }
        Collections.sort(entries);
        scrollPanel.setScrollOffset(0);
    }

    public void render(GuiGraphics g, int sw, int sh, int mx, int my) {
        var font = PWPTheme.Fonts.display();
        int titleY = 78;

        if (state == State.LOADING) {
            g.drawString(font, Component.literal("ЛИДЕРЫ"), sw/2 - font.width("ЛИДЕРЫ")/2, titleY, PWPTheme.Colors.TEXT_ACCENT, false);
            g.fill(sw/2-35, titleY+14, sw/2+35, titleY+15, PWPTheme.Colors.ACCENT);
            String msg = "Загрузка...";
            g.drawString(font, Component.literal(msg), sw/2-font.width(msg)/2, sh/2, PWPTheme.Colors.TEXT_DIM, false);
            return;
        }

        if (entries.isEmpty()) {
            g.drawString(font, Component.literal("ЛИДЕРЫ"), sw/2 - font.width("ЛИДЕРЫ")/2, titleY, PWPTheme.Colors.TEXT_ACCENT, false);
            g.fill(sw/2-35, titleY+14, sw/2+35, titleY+15, PWPTheme.Colors.ACCENT);
            String msg = "Нет данных";
            g.drawString(font, Component.literal(msg), sw/2-font.width(msg)/2, sh/2, PWPTheme.Colors.TEXT_DIM, false);
            return;
        }

        String title = "ЛИДЕРЫ";
        g.drawString(font, Component.literal(title), sw/2-font.width(title)/2, titleY, PWPTheme.Colors.TEXT_ACCENT, false);
        g.fill(sw/2-35, titleY+14, sw/2+35, titleY+15, PWPTheme.Colors.ACCENT);

        Entry top = entries.get(0);
        int cardX = 50, cardW = sw-100, cardY = titleY+26, cardH = 82;
        RoundedRect.fill(g, cardX, cardY, cardW, cardH, 6, 0xFF1A180E);
        RoundedRect.border(g, cardX, cardY, cardW, cardH, 6, 2, PWPTheme.Colors.ACCENT);
        RoundedRect.glow(g, cardX, cardY, cardW, cardH, 6, 3, PWPTheme.Shadows.ACCENT_GLOW_MEDIUM);

        String star = "★ TOP SCORE ★";
        g.drawString(font, Component.literal(star), sw/2-font.width(star)/2, cardY+8, PWPTheme.Colors.TEXT_ACCENT, false);
        String topName = top.name;
        g.drawString(font, Component.literal(topName), sw/2-font.width(topName)/2, cardY+24, 0xFFFFFF, false);
        String scoreStr = String.format("%,d", top.score) + " очков";
        g.drawString(font, Component.literal(scoreStr), sw/2-font.width(scoreStr)/2, cardY+40, PWPTheme.Colors.TEXT_ACCENT, false);
        String detail = "Убийств: "+top.kills+"  |  Побед: "+top.wins+"  |  WinRate: "+(top.wins+top.losses>0 ? String.format("%.0f%%", top.wins*100f/(top.wins+top.losses)) : "0%");
        g.drawString(font, Component.literal(detail), sw/2-font.width(detail)/2, cardY+58, PWPTheme.Colors.TEXT_SECONDARY, false);

        int pillY = cardY+cardH+10;
        this.pillY = pillY;
        int px = (sw - totalPillWidth(font)) / 2;
        hoveredPill = -1;
        for (int i = 0; i < CATS.length; i++) {
            int pw = font.width(CATS[i]) + CAT_PADDING*2;
            boolean hovered = mx>=px && mx<=px+pw && my>=pillY && my<=pillY+18;
            if (hovered) hoveredPill = i;
            boolean active = sortCat == i;
            int pillBg = active ? PWPTheme.Colors.ACCENT : (hovered ? PWPTheme.Colors.SURFACE_LIGHT : PWPTheme.Colors.SURFACE);
            RoundedRect.fill(g, px, pillY, pw, 18, 9, pillBg);
            if (active) RoundedRect.border(g, px, pillY, pw, 18, 9, 1, PWPTheme.Colors.ACCENT);
            g.drawString(font, Component.literal(CATS[i]), px+CAT_PADDING, pillY+4, active ? 0xFF0A0C0E : PWPTheme.Colors.TEXT_SECONDARY, false);
            px += pw+4;
        }

        int tblY = pillY+24, tblX = 40, tblW = sw-80;
        int hcol = PWPTheme.Colors.TEXT_ACCENT;
        g.drawString(font, "#",           tblX+4,   tblY, hcol, false);
        g.drawString(font, "Игрок",       tblX+30,  tblY, hcol, false);
        g.drawString(font, CATS[sortCat], tblX+200, tblY, hcol, false);
        g.drawString(font, "K/D",         tblX+320, tblY, hcol, false);
        g.drawString(font, "Win%",        tblX+380, tblY, hcol, false);
        g.fill(tblX, tblY+14, tblX+tblW, tblY+15, PWPTheme.Colors.BORDER);

        int bodyY = tblY+18, bodyH = sh-bodyY-16;
        scrollPanel.setBounds(tblX, bodyY, tblW, bodyH);
        scrollPanel.setContentHeight(entries.size() * ROW_H);
        scrollPanel.enableScissor(g);
        double scrollOff = scrollPanel.getScrollOffset();
        int soff = -(int)scrollOff;
        hoveredRow = -1;

        for (int i = 0; i < entries.size(); i++) {
            int ry = bodyY + i*ROW_H + soff;
            if (ry+ROW_H < bodyY || ry > bodyY+bodyH) continue;
            Entry e = entries.get(i);
            boolean me = false; // подсветка будет через UUID
            boolean hr = mx>=tblX && mx<=tblX+tblW && my>=ry && my<=ry+ROW_H;
            if (hr) hoveredRow = i;
            if (me) {
                RoundedRect.fill(g, tblX, ry, tblW, ROW_H, 3, PWPTheme.Colors.withAlpha(PWPTheme.Colors.ACCENT, 25));
                RoundedRect.border(g, tblX, ry, tblW, ROW_H, 3, 1, PWPTheme.Colors.withAlpha(PWPTheme.Colors.ACCENT, 50));
            } else if (i % 2 == 0) g.fill(tblX, ry, tblX+tblW, ry+ROW_H, 0x08000000);
            int rank = i+1;
            int rc = rank==1 ? 0xFFFFD700 : (rank==2 ? 0xFFC0C0C0 : (rank==3 ? 0xFFCD7F32 : PWPTheme.Colors.TEXT_SECONDARY));
            g.drawString(font, Component.literal(String.valueOf(rank)), tblX+4, ry+2, rc, false);
            String n = e.name;
            if (font.width(n) > 160) n = font.plainSubstrByWidth(n, 158)+".";
            g.drawString(font, Component.literal(n), tblX+30, ry+2, me ? PWPTheme.Colors.TEXT_ACCENT : PWPTheme.Colors.TEXT_PRIMARY, false);
            String val = formatCatValue(sortCat, e);
            if (font.width(val) > 100) val = font.plainSubstrByWidth(val, 98);
            g.drawString(font, Component.literal(val), tblX+200, ry+2, PWPTheme.Colors.TEXT_PRIMARY, false);
            String kdStr = String.format("%.2f", e.deaths>0 ? e.kills/(double)e.deaths : (double)e.kills);
            g.drawString(font, Component.literal(kdStr), tblX+320, ry+2, PWPTheme.Colors.TEXT_SECONDARY, false);
            String wrStr = (e.wins+e.losses)>0 ? String.format("%.0f%%", e.wins*100f/(e.wins+e.losses)) : "0%";
            g.drawString(font, Component.literal(wrStr), tblX+380, ry+2, PWPTheme.Colors.TEXT_SECONDARY, false);
            if (me) g.drawString(font, Component.literal("◀ вы"), tblX+tblW-50, ry+2, PWPTheme.Colors.TEXT_ACCENT, false);
        }
        scrollPanel.disableScissor(g);
        scrollPanel.renderScrollbar(g);
    }

    private int totalPillWidth(net.minecraft.client.gui.Font font) {
        int w = 0;
        for (String s : CATS) w += font.width(s) + CAT_PADDING*2 + 4;
        return w-4;
    }

    private String formatCatValue(int cat, Entry e) {
        return switch (cat) {
            case 0 -> String.format("%,d", e.score);
            case 1 -> String.format("%,d", e.kills);
            case 2 -> String.format("%.2f", e.deaths>0 ? e.kills/(double)e.deaths : (double)e.kills);
            case 3 -> String.valueOf(e.wins);
            case 4 -> (e.wins+e.losses)>0 ? String.format("%.0f%%", e.wins*100f/(e.wins+e.losses)) : "0%";
            case 5 -> String.valueOf(e.captures);
            case 6 -> String.format("%,d", e.damage);
            case 7 -> String.format("%,d", e.healing);
            case 8 -> formatPlaytime(e.playtimeSec);
            default -> "";
        };
    }

    private static String formatPlaytime(int secs) {
        int h = secs/3600;
        if (h >= 24) return (h/24)+"д "+(h%24)+"ч";
        return h+"ч";
    }

    public boolean mouseClicked(double mx, double my) {
        if (scrollPanel.mouseClicked(mx, my, 0)) return true;
        if (my >= pillY && my <= pillY+18 && hoveredPill >= 0) {
            sortBy(hoveredPill);
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return scrollPanel.mouseDragged(mx, my, button, dx, dy);
    }

    public boolean mouseReleased(double mx, double my, int button) {
        return scrollPanel.mouseReleased(mx, my, button);
    }

    public boolean mouseScrolled(double mx, double my, double delta) {
        return Double.compare(scrollPanel.mouseScrolled(mx, my, delta), 0) != 0;
    }
}
