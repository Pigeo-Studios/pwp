package com.pwp.coreclient.gui.screens.tabs;

import com.pwp.coreclient.gui.components.PWPMatchCard;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.OpenMatchScreenPacket;
import com.pwp.coreclient.network.OpenMatchListScreenPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class PlayTabRenderer {

    public static class MatchEntry {
        public String title;
        public String mode;
        public String duration;
        public int currentPlayers;
        public int maxPlayers;
        public String blueFaction;
        public String redFaction;
        public int blueTickets;
        public int redTickets;
        public PWPMatchCard.Status status;

        public MatchEntry(String title, String mode, String duration, int current, int max,
                          String blue, String red, int bt, int rt, PWPMatchCard.Status status) {
            this.title = title; this.mode = mode; this.duration = duration;
            this.currentPlayers = current; this.maxPlayers = max;
            this.blueFaction = blue; this.redFaction = red;
            this.blueTickets = bt; this.redTickets = rt; this.status = status;
        }
    }

    private final List<MatchEntry> matches = new ArrayList<>();
    private int hoveredIndex = -1;
    private long tooltipHoverStart;
    private int prevHovered = -1;

    public PlayTabRenderer() {}

    public void setData(OpenMatchScreenPacket matchPkt, OpenMatchListScreenPacket listPkt) {
        matches.clear();
        if (matchPkt != null) {
            matches.add(new MatchEntry(matchPkt.mapDisplayName, matchPkt.modeDisplayName,
                matchPkt.remainingSeconds + "s", 0, 0,
                matchPkt.blueFaction, matchPkt.redFaction,
                matchPkt.blueTickets, matchPkt.redTickets, PWPMatchCard.Status.PLAYING));
        }
        if (listPkt != null) {
            for (int i = 0; i < listPkt.count; i++) {
                PWPMatchCard.Status st = parseStatus(listPkt.statuses[i]);
                matches.add(new MatchEntry(listPkt.displayNames[i], listPkt.statuses[i],
                    listPkt.elapsedSeconds[i] + "s",
                    listPkt.playerCounts[i], listPkt.maxPlayers[i],
                    listPkt.blueFactions[i], listPkt.redFactions[i],
                    listPkt.blueTickets[i], listPkt.redTickets[i], st));
            }
        }
    }

    private static PWPMatchCard.Status parseStatus(String s) {
        return switch (s != null ? s : "") {
            case "PLAYING" -> PWPMatchCard.Status.PLAYING;
            case "STARTING" -> PWPMatchCard.Status.STARTING;
            case "FULL" -> PWPMatchCard.Status.FULL;
            case "CLOSED" -> PWPMatchCard.Status.CLOSED;
            case "VOTING" -> PWPMatchCard.Status.VOTING;
            case "FINISHED" -> PWPMatchCard.Status.FINISHED;
            default -> PWPMatchCard.Status.WAITING;
        };
    }

    public void render(GuiGraphics gui, int screenW, int screenH, int mouseX, int mouseY) {
        long now = System.currentTimeMillis();
        var font = PWPTheme.Fonts.display();
        int titleY = 80;

        String title = "АКТИВНЫЕ МАТЧИ";
        gui.drawString(font, Component.literal(title), screenW / 2 - font.width(title) / 2, titleY, PWPTheme.Colors.TEXT_ACCENT, false);
        gui.fill(screenW / 2 - 60, titleY + 14, screenW / 2 + 60, titleY + 15, PWPTheme.Colors.ACCENT);

        int listX = 60;
        int listW = screenW - 120;
        int cardH = PWPMatchCard.cardHeight();
        int cardY = titleY + 30;
        int gap = 6;

        hoveredIndex = -1;
        for (int i = 0; i < matches.size(); i++) {
            int cy = cardY + i * (cardH + gap);
            boolean hovered = mouseX >= listX && mouseX <= listX + listW && mouseY >= cy && mouseY <= cy + cardH;
            if (hovered) hoveredIndex = i;

            MatchEntry m = matches.get(i);
            PWPMatchCard.render(gui, listX, cy, listW, cardH,
                m.title, m.mode, m.duration,
                m.currentPlayers, m.maxPlayers,
                m.blueFaction, m.redFaction,
                m.blueTickets, m.redTickets,
                m.status, null, false, hovered);
        }

        // Tooltip
        if (hoveredIndex >= 0 && hoveredIndex < matches.size()) {
            if (hoveredIndex != prevHovered) { tooltipHoverStart = now; prevHovered = hoveredIndex; }
            if (now - tooltipHoverStart > 1500) {
                MatchEntry m = matches.get(hoveredIndex);
                renderTooltip(gui, m, mouseX, mouseY, screenW, screenH, now);
            }
        } else { prevHovered = -1; }
    }

    private void renderTooltip(GuiGraphics g, MatchEntry m, int mx, int my, int sw, int sh, long now) {
        long elapsed = now - tooltipHoverStart - 1500;
        float t = Math.min(elapsed / 400f, 1);
        float alpha = Math.max(0, Math.min(1, t < 0.5f ? 2*t*t : -1 + (4-2*t)*t));
        if (alpha < 0.01f) return;

        var f = PWPTheme.Fonts.display();
        String[] lines = {
            m.title,
            m.mode + " | " + m.duration,
            m.blueFaction + " vs " + m.redFaction,
            "Игроки: " + m.currentPlayers + "/" + m.maxPlayers,
            m.status == PWPMatchCard.Status.PLAYING || m.status == PWPMatchCard.Status.FINISHED
                ? "Билеты: " + m.blueTickets + " | " + m.redTickets : ""
        };

        int lineH = 12;
        int pad = 8;
        int tw = 0;
        for (String l : lines) { int lw = f.width(l); if (lw > tw) tw = lw; }
        tw = Math.min(tw + pad * 2, sw - 20);
        int th = lines.length * lineH + pad;

        int tx = mx + 12, ty = my - th - 8;
        if (tx + tw > sw - 8) tx = sw - 8 - tw;
        if (ty < 8) ty = my + 12;

        RoundedRect.fill(g, tx, ty, tw, th, 4, PWPTheme.Colors.multiplyAlpha(0xCC0E1117, alpha));
        RoundedRect.border(g, tx, ty, tw, th, 4, 1, PWPTheme.Colors.multiplyAlpha(PWPTheme.Colors.BORDER_LIGHT, alpha));

        int tc = PWPTheme.Colors.multiplyAlpha(PWPTheme.Colors.TEXT_PRIMARY, alpha);
        int ly = ty + 4;
        for (String l : lines) {
            if (!l.isEmpty()) g.drawString(f, Component.literal(l), tx + pad, ly, tc, false);
            ly += lineH;
        }
    }

    public int getHoveredIndex() { return hoveredIndex; }
    public List<MatchEntry> getMatches() { return matches; }
}
