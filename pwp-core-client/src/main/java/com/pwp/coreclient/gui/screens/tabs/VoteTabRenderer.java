package com.pwp.coreclient.gui.screens.tabs;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pwp.coreclient.gui.components.PWPCard;
import com.pwp.coreclient.gui.components.PWPProgressBar;
import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.OpenVotingScreenPacket;
import com.pwp.coreclient.network.OpenModeVotePacket;
import com.pwp.coreclient.network.OpenFactionVotePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class VoteTabRenderer {

    private enum Phase { IDLE, MAP, MODE, FACTION, FINISHED }

    private static class Option {
        String id, name, desc, f1, f2, worldPath;
        int votes, color;
        int remainingSec;
        PWPProgressBar bar;
        ResourceLocation icon;
        java.util.List<ResourceLocation> teamFlags;

        Option(String id, String name, String desc, String f1, String f2, int votes, int color) {
            this(id, name, desc, f1, f2, votes, color, null, null, null);
        }
        Option(String id, String name, String desc, String f1, String f2, int votes, int color, ResourceLocation icon) {
            this(id, name, desc, f1, f2, votes, color, icon, null, null);
        }
        Option(String id, String name, String desc, String f1, String f2, int votes, int color, ResourceLocation icon, String worldPath, java.util.List<ResourceLocation> teamFlags) {
            this.id = id; this.name = name; this.desc = desc; this.f1 = f1; this.f2 = f2;
            this.votes = votes; this.color = color; this.icon = icon; this.worldPath = worldPath; this.teamFlags = teamFlags;
            this.bar = new PWPProgressBar();
        }
    }

    private Phase currentPhase = Phase.IDLE;
    private long phaseStartTime = System.currentTimeMillis();
    private boolean transitioning;
    private long transitionTime;
    private Phase transitionFrom, transitionTo;
    private long finishedAnimStart;
    private boolean finishedBusy;

    private final List<Option> mapOpts = new ArrayList<>();
    private final List<Option> modeOpts = new ArrayList<>();
    private final List<Option> factOpts = new ArrayList<>();

    private int selMap = -1, selMode = -1, selFact1 = -1, selFact2 = -1;
    private int votMap = -1, votMode = -1, votFact1 = -1, votFact2 = -1;
    private int hoveredCard = -1;
    private int screenWidth;
    private int voteButtonY;
    private int tooltipTarget = -1;
    private long tooltipHoverStart;
    private int tooltipMouseX, tooltipMouseY;

    public VoteTabRenderer() {}

    public void initVoteData(OpenVotingScreenPacket pkt) {
        mapOpts.clear();
        for (int i = 0; i < pkt.mapNames.length; i++) {
            mapOpts.add(new Option(pkt.mapNames[i], pkt.mapDisplayNames[i], pkt.mapDescriptions[i],
                pkt.blueFactions[i], pkt.redFactions[i],
                pkt.voteCounts[i], 0xFF8B6B3D + i * 0x112233, null,
                i < pkt.worldPaths.length ? pkt.worldPaths[i] : null, null));
        }
        selMap = -1; votMap = -1;
        recalc(mapOpts);
        if (currentPhase == Phase.IDLE || currentPhase == Phase.FINISHED) xition(Phase.MAP);
        else { currentPhase = Phase.MAP; phaseStartTime = System.currentTimeMillis(); }
    }

    public void updateVoteData(OpenVotingScreenPacket pkt) {
        for (int i = 0; i < pkt.mapNames.length && i < mapOpts.size(); i++) {
            mapOpts.get(i).votes = pkt.voteCounts[i];
        }
        recalc(mapOpts);
    }

    public void initModeVoteData(OpenModeVotePacket pkt) {
        modeOpts.clear();
        for (int i = 0; i < pkt.modeNames.length; i++) {
            modeOpts.add(new Option(pkt.modeNames[i], pkt.modeDisplayNames[i], pkt.modeDescriptions[i],
                null, null, pkt.voteCounts[i], i == 0 ? 0xFFC8812A : 0xFF3D6FA5));
        }
        selMode = -1; votMode = -1;
        recalc(modeOpts);
        xition(Phase.MODE);
    }

    public void updateModeVoteData(OpenModeVotePacket pkt) {
        for (int i = 0; i < pkt.modeNames.length && i < modeOpts.size(); i++) {
            modeOpts.get(i).votes = pkt.voteCounts[i];
        }
        recalc(modeOpts);
    }

    public void initFactionVoteData(OpenFactionVotePacket pkt) {
        factOpts.clear();
        java.util.Map<String,String> fnames = new java.util.HashMap<>();
        fnames.put("usa","США"); fnames.put("ukraine","Украина"); fnames.put("nato","НАТО");
        fnames.put("russia","Россия"); fnames.put("insurgency","Insurgency"); fnames.put("pmc","ЧВК");
        for (int i = 0; i < 3; i++) {
            String n = pkt.team1Factions != null && i < pkt.team1Factions.length ? pkt.team1Factions[i] : "";
            int v = pkt.team1Votes != null && i < pkt.team1Votes.length ? pkt.team1Votes[i] : 0;
            String dn = fnames.getOrDefault(n, n.toUpperCase());
            factOpts.add(new Option(n, dn, null, null, null, v, 0xFF3D6FA5, flag(n)));
        }
        for (int i = 0; i < 3; i++) {
            String n = pkt.team2Factions != null && i < pkt.team2Factions.length ? pkt.team2Factions[i] : "";
            int v = pkt.team2Votes != null && i < pkt.team2Votes.length ? pkt.team2Votes[i] : 0;
            String dn = fnames.getOrDefault(n, n.toUpperCase());
            factOpts.add(new Option(n, dn, null, null, null, v, 0xFFA53D3D, flag(n)));
        }
        selFact1 = -1; selFact2 = -1; votFact1 = -1; votFact2 = -1;
        recalc(factOpts);
        xition(Phase.FACTION);
    }

    public void updateFactionVoteData(OpenFactionVotePacket pkt) {
        for (int i = 0; i < 6 && i < factOpts.size(); i++) {
            int v = i < 3 ? (i < pkt.team1Votes.length ? pkt.team1Votes[i] : 0)
                          : (i-3 < pkt.team2Votes.length ? pkt.team2Votes[i-3] : 0);
            factOpts.get(i).votes = v;
        }
        recalc(factOpts);
    }

    private static ResourceLocation flag(String n) { return n != null && !n.isEmpty() ? new ResourceLocation("pwpwarfare", "textures/gui/flags/" + n + ".png") : null; }
    private static void recalc(List<Option> o) { int t = 0; for (Option x : o) t += x.votes; for (Option x : o) x.bar.setProgress(t > 0 ? (float)x.votes/t : 0); }

    private static ResourceLocation getMapPreview(String worldPath) {
        if (worldPath == null || worldPath.isEmpty()) return null;
        return com.pwp.coreclient.gui.screens.PWPLobbyScreen.getTexture("preview", worldPath, "vote_");
    }

    public void render(GuiGraphics g, int sw, int sh, int mx, int my) {
        screenWidth = sw;
        long now = System.currentTimeMillis();
        if (transitioning) {
            long e = now - transitionTime;
            float t = Math.min(e / 500f, 1), ez = eoc(t);
            if (transitionTo == Phase.FINISHED) {
                // FACTION -> FINISHED: dark overlay fade in/out, then cards slide up
                int ovAlpha = (int)(Math.min(1, t * 2) * 0xCC);
                g.fill(0, 70, sw, sh - 16, (ovAlpha << 24) | 0x06080A);
                if (t > 0.3f) {
                    float cardT = Math.min((t - 0.3f) / 0.7f, 1);
                    RenderSystem.setShaderColor(1,1,1, cardT);
                    renderPhase(g, sw, sh, mx, my, transitionTo, now);
                    RenderSystem.setShaderColor(1,1,1,1);
                }
            } else {
                RenderSystem.setShaderColor(1,1,1,1-ez);
                renderPhase(g, sw, sh, mx, my, transitionFrom, now);
                RenderSystem.setShaderColor(1,1,1,ez);
                renderPhase(g, sw, sh, mx, my, transitionTo, now);
                RenderSystem.setShaderColor(1,1,1,1);
            }
            if (t >= 1) { transitioning = false; currentPhase = transitionTo; phaseStartTime = now; }
        } else { renderPhase(g, sw, sh, mx, my, currentPhase, now); renderAfter(g, sw, sh, mx, my, now); }
    }

    private void renderAfter(GuiGraphics g, int sw, int sh, int mx, int my, long now) {
        // Track tooltip
        int prevTarget = tooltipTarget;
        tooltipTarget = -1;
        if (currentPhase == Phase.MAP || currentPhase == Phase.MODE) {
            if (hoveredCard >= 0) {
                List<Option> opts = currentPhase == Phase.MAP ? mapOpts : modeOpts;
                if (hoveredCard < opts.size() && opts.get(hoveredCard).desc != null && !opts.get(hoveredCard).desc.isEmpty()) {
                    tooltipTarget = hoveredCard;
                }
            }
        }
        String tooltipText = null;
        if (tooltipTarget >= 0) {
            if (tooltipTarget != prevTarget) tooltipHoverStart = now;
            if (now - tooltipHoverStart > 1500) {
                List<Option> opts = currentPhase == Phase.MAP ? mapOpts : modeOpts;
                if (tooltipTarget < opts.size()) tooltipText = opts.get(tooltipTarget).desc;
            }
        } else { tooltipHoverStart = now; }

        if (tooltipText != null) renderTooltip(g, tooltipText, mx, my, sw, sh, now);
    }

    private void renderTooltip(GuiGraphics g, String text, int mx, int my, int sw, int sh, long now) {
        long elapsed = now - tooltipHoverStart - 1500;
        float t = Math.min(elapsed / 400f, 1);
        float alpha = Math.max(0, Math.min(1, eoc2(t)));
        if (alpha < 0.01f) return;

        var f = PWPTheme.Fonts.display();
        int maxW = 280;
        java.util.List<String> lines = new java.util.ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String test = cur.length() == 0 ? w : cur + " " + w;
            if (f.width(test) > maxW && cur.length() > 0) { lines.add(cur.toString()); cur = new StringBuilder(w); }
            else { if (cur.length() > 0) cur.append(" "); cur.append(w); }
        }
        if (cur.length() > 0) lines.add(cur.toString());

        int lineH = 12;
        int tw = 0;
        for (String ln : lines) { int lw = f.width(ln); if (lw > tw) tw = lw; }
        int th = lines.size() * lineH + 12;
        int pad = 8;
        tw = Math.min(tw + pad * 2, sw - 20);

        int tx = mx + 12, ty = my - th - 8;
        if (tx + tw > sw - 8) tx = sw - 8 - tw;
        if (ty < 8) ty = my + 12;

        int bg = PWPTheme.Colors.multiplyAlpha(0xCC0E1117, alpha);
        int bd = PWPTheme.Colors.multiplyAlpha(PWPTheme.Colors.BORDER_LIGHT, alpha);
        RoundedRect.fill(g, tx, ty, tw, th, 4, bg);
        RoundedRect.border(g, tx, ty, tw, th, 4, 1, bd);

        int tc = PWPTheme.Colors.multiplyAlpha(PWPTheme.Colors.TEXT_PRIMARY, alpha);
        int ly = ty + 6;
        for (String ln : lines) {
            g.drawString(f, Component.literal(ln), tx + pad, ly, tc, false);
            ly += lineH;
        }
    }

    private static float eoc2(float t) { return t < 0.5f ? 2*t*t : -1 + (4-2*t)*t; }

    private static float eoc(float t) { float f = 1-t; return 1-f*f*f; }
    private static String suffix(int v) {
        if (v % 10 == 1 && v % 100 != 11) return "";
        if (v % 10 >= 2 && v % 10 <= 4 && (v % 100 < 10 || v % 100 >= 20)) return "а";
        return "ов";
    }

    private void xition(Phase to) { transitionFrom = currentPhase; transitionTo = to; transitioning = true; transitionTime = System.currentTimeMillis(); if (to == Phase.FINISHED) finishedAnimStart = transitionTime; }

    private void renderPhase(GuiGraphics g, int sw, int sh, int mx, int my, Phase p, long now) {
        switch (p) {
            case IDLE -> idle(g, sw, sh, mx, my, now);
            case MAP -> voteGrid(g, sw, sh, mx, my, now, "ГОЛОСОВАНИЕ ЗА КАРТУ", mapOpts, selMap, true);
            case MODE -> voteGrid(g, sw, sh, mx, my, now, "ГОЛОСОВАНИЕ ЗА РЕЖИМ", modeOpts, selMode, false);
            case FACTION -> factionGrid(g, sw, sh, mx, my, now);
            case FINISHED -> finished(g, sw, sh, now);
        }
    }
    private void idle(GuiGraphics g, int sw, int sh, int mx, int my, long now) {
        var f = PWPTheme.Fonts.display();
        g.drawString(f, Component.literal("ГОЛОСОВАНИЕ"), sw/2 - f.width("ГОЛОСОВАНИЕ")/2, 82, PWPTheme.Colors.TEXT_ACCENT, false);
        g.fill(sw/2 - 60, 96, sw/2 + 60, 97, PWPTheme.Colors.ACCENT);
        String s = "ОЖИДАНИЕ ГОЛОСОВАНИЯ";
        g.drawString(f, Component.literal(s), sw/2 - f.width(s)/2, 130, PWPTheme.Colors.TEXT_SECONDARY, false);
        String h = "Голосование начнётся автоматически";
        g.drawString(f, Component.literal(h), sw/2 - f.width(h)/2, 150, PWPTheme.Colors.TEXT_DIM, false);
    }

    private void voteGrid(GuiGraphics g, int sw, int sh, int mx, int my, long now, String title, List<Option> opts, int sel, boolean factions) {
        var f = PWPTheme.Fonts.display();
        long elapsed = now - phaseStartTime, rem = Math.max(0, 120 - elapsed/1000);
        // Store remaining for faction vote timer reference
        for (Option o : opts) o.remainingSec = (int)rem;
        g.drawString(f, Component.literal(title), sw/2 - f.width(title)/2, 80, PWPTheme.Colors.TEXT_ACCENT, false);
        g.fill(sw/2 - f.width(title)/2 - 4, 94, sw/2 + f.width(title)/2 + 4, 95, PWPTheme.Colors.ACCENT);
        String tmr = String.format("Осталось: %d:%02d", rem/60, rem%60);
        g.drawString(f, Component.literal(tmr), sw/2 - f.width(tmr)/2, 104, PWPTheme.Colors.TEXT_SECONDARY, false);
        int cw = 200, ch = 130, gap = 12, cols = 2;
        int rows = (opts.size()+cols-1)/cols, gw = cols*cw+(cols-1)*gap, gh = rows*ch+(rows-1)*gap, gx = (sw-gw)/2, gy = 118;
        hoveredCard = -1;
        for (int i = 0; i < opts.size(); i++) {
            int col = i%cols, row = i/cols, cx = gx+col*(cw+gap), cy = gy+row*(ch+gap);
            boolean hv = mx>=cx&&mx<=cx+cw&&my>=cy&&my<=cy+ch;
            if (hv) hoveredCard = i;
            renderCard(g, cx, cy, cw, ch, opts.get(i), i==sel, hv, now, factions);
        }
        int voted = title.contains("КАРТУ") ? votMap : (title.contains("РЕЖИМ") ? votMode : -1);
        boolean canVote = sel>=0 && sel!=voted;
        int by = gy+gh+14, bw = 180, bh = 28, bx = (sw-bw)/2;
        voteButtonY = by;
        boolean bhv = mx>=bx&&mx<=bx+bw&&my>=by&&my<=by+bh;
        String bt = voted>=0 ? "ИЗМЕНИТЬ ГОЛОС" : "ГОЛОСОВАТЬ";
        RoundedRect.fill(g, bx, by, bw, bh, 3, canVote ? (bhv ? PWPTheme.Colors.ACCENT_SOFT : PWPTheme.Colors.ACCENT) : PWPTheme.Colors.SURFACE_DIM);
        RoundedRect.border(g, bx, by, bw, bh, 3, 1, canVote ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.BORDER);
        g.drawString(f, Component.literal(bt), bx+bw/2-f.width(bt)/2, by+8, canVote ? 0xFF0A0C0E : PWPTheme.Colors.TEXT_DIM, false);
    }
    private void factionGrid(GuiGraphics g, int sw, int sh, int mx, int my, long now) {
        var f = PWPTheme.Fonts.display();
        long elapsed = now - phaseStartTime, rem = Math.max(0, 120 - elapsed/1000);
        String title = "ВЫБОР СТОРОНЫ";
        g.drawString(f, Component.literal(title), sw/2-f.width(title)/2, 80, PWPTheme.Colors.TEXT_ACCENT, false);
        g.fill(sw/2-f.width(title)/2-4, 94, sw/2+f.width(title)/2+4, 95, PWPTheme.Colors.ACCENT);
        String tmr = String.format("Осталось: %d:%02d", rem/60, rem%60);
        g.drawString(f, Component.literal(tmr), sw/2-f.width(tmr)/2, 104, PWPTheme.Colors.TEXT_SECONDARY, false);

        int cw = 155, ch = 80, gap = 10;
        int gw = 2*cw+gap, gx = (sw-gw)/2, gy = 118;
        int cardGap = 4;
        int colH = 3 * (ch + cardGap) - cardGap;

        // Center divider
        g.fill(sw/2-1, gy-12, sw/2+1, gy+colH, PWPTheme.Colors.BORDER);

        // Team labels
        String t1 = "КОМАНДА 1", t2 = "КОМАНДА 2";
        boolean isInv = selMode >= 0 && selMode < modeOpts.size() && modeOpts.get(selMode).name.contains("Invasion");
        if (isInv) { t1 = "АТАКУЕТ"; t2 = "ОБОРОНЯЕТСЯ"; }
        g.drawString(f, Component.literal(t1), gx+cw/2-f.width(t1)/2, gy-12, 0xFF3D6FA5, false);
        g.drawString(f, Component.literal(t2), gx+cw+gap+cw/2-f.width(t2)/2, gy-12, 0xFFA53D3D, false);

        // Smooth recalc for per-card bars
        recalc(factOpts);

        // Cards: 3 per column
        hoveredCard = -1;
        int cardY = gy + 2;
        for (int i = 0; i < factOpts.size(); i++) {
            int col = i / 3, row = i % 3;
            int cx = gx + col * (cw + gap);
            int cy = cardY + row * (ch + cardGap);
            boolean hv = mx>=cx&&mx<=cx+cw&&my>=cy&&my<=cy+ch;
            if (hv) hoveredCard = i;
            boolean sel = (col == 0) ? (row == selFact1) : (row == selFact2);
            renderFactionCard(g, cx, cy, cw, ch, factOpts.get(i), sel, hv, now, i);
        }

        // Vote button
        boolean canVote = (selFact1 >= 0 || votFact1 >= 0) && (selFact2 >= 0 || votFact2 >= 0)
            && (selFact1 != votFact1 || selFact2 != votFact2);
        int by = cardY + colH + 10;
        voteButtonY = by;
        int bw = 180, bh = 26, bx = (sw-bw)/2;
        boolean bhv = mx>=bx&&mx<=bx+bw&&my>=by&&my<=by+bh;
        boolean voted = votFact1 >= 0 && votFact2 >= 0;
        String bt = voted ? "ИЗМЕНИТЬ ВЫБОР" : "ПРОГОЛОСОВАТЬ";
        RoundedRect.fill(g, bx, by, bw, bh, 3, canVote ? (bhv?PWPTheme.Colors.ACCENT_SOFT:PWPTheme.Colors.ACCENT) : PWPTheme.Colors.SURFACE_DIM);
        RoundedRect.border(g, bx, by, bw, bh, 3, 1, canVote ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.BORDER);
        g.drawString(f, Component.literal(bt), bx+bw/2-f.width(bt)/2, by+7, canVote ? 0xFF0A0C0E : PWPTheme.Colors.TEXT_DIM, false);
    }

    private void renderFactionCard(GuiGraphics g, int x, int y, int w, int h, Option o, boolean sel, boolean hv, long now, int cardIndex) {
        var f = PWPTheme.Fonts.display();
        PWPCard.State st = sel ? PWPCard.State.SELECTED : (hv ? PWPCard.State.HOVER : PWPCard.State.DEFAULT);
        PWPCard.render(g, x, y, w, h, st);
        if (sel) RoundedRect.glow(g, x, y, w, h, PWPTheme.Spacing.RADIUS_MEDIUM, 2, PWPTheme.Shadows.ACCENT_GLOW_MEDIUM);

        int cy = y + 6;
        if (o.icon != null) {
            RenderSystem.setShaderColor(1,1,1,1);
            g.blit(o.icon, x+w/2-20, cy, 0, 0, 40, 23, 40, 23);
            cy += 28;
        } else { g.fill(x+6, cy, x+w-6, cy+22, o.color); cy+=26; }

        String n = o.name;
        if (f.width(n) > w-12) n = f.plainSubstrByWidth(n, w-16)+"...";
        g.drawString(f, Component.literal(n), x+w/2-f.width(n)/2, cy, 0xFFFFFF, false);
        cy += 11;

        // Progress bar: relative to team total, not global
        int teamStart = (cardIndex < 3) ? 0 : 3;
        int teamTotal = 0;
        for (int i = teamStart; i < teamStart + 3; i++) teamTotal += factOpts.get(i).votes;
        float prog = teamTotal > 0 ? (float) o.votes / teamTotal : 0;
        int bw = w - 16;
        g.fill(x+8, cy, x+8+bw, cy+5, PWPTheme.Styles.Progress.BG);
        if (prog > 0.01f) { int fw = Math.max(2, (int)(bw*prog)); g.fill(x+8, cy, x+8+fw, cy+5, sel ? PWPTheme.Colors.ACCENT : o.color); }
        cy += 8;
        String vs = o.votes+" голос"+suffix(o.votes);
        g.drawString(f, Component.literal(vs), x+w/2-f.width(vs)/2, cy, PWPTheme.Colors.TEXT_DIM, false);
    }

    private void renderCard(GuiGraphics g, int x, int y, int w, int h, Option o, boolean sel, boolean hv, long now, boolean showF) {
        var f = PWPTheme.Fonts.display();
        PWPCard.render(g, x, y, w, h, sel ? PWPCard.State.SELECTED : (hv ? PWPCard.State.HOVER : PWPCard.State.DEFAULT));
        int px = x+8, py = y+8, pw = w-16, ph = h/3;
        ResourceLocation preview = getMapPreview(o.worldPath);
        if (preview != null) {
            g.fill(px, py, px+pw, py+ph, 0xFF000000);
            int rw = pw - 2, rh = ph - 2;
            g.blit(preview, px+1, py+1, 0, 0, rw, rh, rw, rh);
        } else {
            g.fill(px, py, px+pw, py+ph, 0xFF000000);
            g.fill(px+1, py+1, px+pw-1, py+ph-1, sel ? PWPTheme.Colors.lerp(o.color, PWPTheme.Colors.ACCENT, 0.3f) : o.color);
        }
        String t = o.name;
        if (f.width(t) > pw-8) t = f.plainSubstrByWidth(t, pw-12)+"...";
        g.drawString(f, Component.literal(t), px+6, py+ph/2-4, 0xFFFFFFFF, false);
        int ly = py+ph+6;

        if (o.desc != null && !o.desc.isEmpty()) {
            int maxW = w - 24;
            String d = o.desc;
            if (f.width(d) > maxW) {
                int splitIdx = 0;
                for (int j = 1; j < d.length(); j++) {
                    if (f.width(d.substring(0, j)) > maxW) { splitIdx = j - 1; break; }
                }
                if (splitIdx > 5) {
                    String l1 = d.substring(0, splitIdx);
                    String l2 = d.substring(splitIdx).trim();
                    if (f.width(l2) > maxW) l2 = f.plainSubstrByWidth(l2, maxW-4)+"...";
                    g.drawString(f, Component.literal(l1), x+10, ly, PWPTheme.Colors.TEXT_DIM, false); ly+=10;
                    g.drawString(f, Component.literal(l2), x+10, ly, PWPTheme.Colors.TEXT_DIM, false); ly+=11;
                } else {
                    g.drawString(f, Component.literal(f.plainSubstrByWidth(d, maxW-4)+"..."), x+10, ly, PWPTheme.Colors.TEXT_DIM, false); ly+=11;
                }
            } else {
                g.drawString(f, Component.literal(d), x+10, ly, PWPTheme.Colors.TEXT_DIM, false); ly+=11;
            }
        }
        int bw = w-20, bx = x+10;
        o.bar.render(g, bx, ly, bw, 6, now, sel ? PWPTheme.Colors.ACCENT : PWPTheme.Colors.TEXT_ACCENT);
        ly += 10;
        String vs = o.votes+" голос"+suffix(o.votes);
        g.drawString(f, Component.literal(vs), x+10, ly, PWPTheme.Colors.TEXT_DIM, false);
    }
    private void finished(GuiGraphics g, int sw, int sh, long now) {
        var f = PWPTheme.Fonts.display();
        String title = "ГОЛОСОВАНИЕ ЗАВЕРШЕНО";
        g.drawString(f, Component.literal(title), sw/2-f.width(title)/2, 82, PWPTheme.Colors.TEXT_ACCENT, false);
        g.fill(sw/2-80, 96, sw/2+80, 97, PWPTheme.Colors.ACCENT);

        Option wMap = mapOpts.stream().max((a,b)->Integer.compare(a.votes,b.votes)).orElse(null);
        Option wMode = modeOpts.stream().max((a,b)->Integer.compare(a.votes,b.votes)).orElse(null);
        Option wF1 = factOpts.subList(0,3).stream().max((a,b)->Integer.compare(a.votes,b.votes)).orElse(null);
        Option wF2 = factOpts.subList(3,6).stream().max((a,b)->Integer.compare(a.votes,b.votes)).orElse(null);

        long animStart = finishedAnimStart;
        int cardW = 190, cardH = 160, gap = 16;
        int totalW = 3*cardW + 2*gap;
        int startX = (sw - totalW) / 2;
        int baseY = 130;

        // Card 1: Map (stagger 0ms)
        String w1 = wMap != null ? wMap.name : "-";
        String v1 = wMap != null ? wMap.votes + " голос" + suffix(wMap.votes) : "0";
        drawWinnerCard(g, startX, baseY, cardW, cardH, "КАРТА", w1, v1, wMap != null ? wMap.color : 0xFF666666, now, animStart, 0);

        // Card 2: Mode (stagger 200ms)
        String w2 = wMode != null ? wMode.name : "-";
        String v2 = wMode != null ? wMode.votes + " голос" + suffix(wMode.votes) : "0";
        drawWinnerCard(g, startX+cardW+gap, baseY, cardW, cardH, "РЕЖИМ", w2, v2, wMode != null ? wMode.color : 0xFF666666, now, animStart, 200);

        // Card 3: Sides (stagger 400ms) — split card with two flags
        drawWinnerSplitCard(g, startX+2*(cardW+gap), baseY, cardW, cardH, "СТОРОНЫ", wF1, wF2, now, animStart, 400);

        // New vote button
        int btnY = baseY + cardH + 30;
        int bw = 180, bh = 28, bx = (sw-bw)/2;
        RoundedRect.fill(g, bx, btnY, bw, bh, 3, PWPTheme.Colors.ACCENT);
        RoundedRect.border(g, bx, btnY, bw, bh, 3, 1, PWPTheme.Colors.ACCENT);
        g.drawString(f, Component.literal("НОВОЕ ГОЛОСОВАНИЕ"), bx+bw/2-f.width("НОВОЕ ГОЛОСОВАНИЕ")/2, btnY+8, 0xFF0A0C0E, false);

        // Update mouseClicked target Y for this button
        voteButtonY = btnY;
    }

    private void drawWinnerCard(GuiGraphics g, int x, int baseY, int w, int h, String label, String name, String votes, int color, long now, long animStart, int delayMs) {
        long elapsed = now - animStart - delayMs;
        if (elapsed < 0) return;
        float t = Math.min(elapsed / 600f, 1);
        float eased = eoc(t);
        int y = baseY + (int)(50 * (1 - eased));
        int alpha = Math.min(255, (int)(255 * eased));
        if (alpha < 5) return;

        var f = PWPTheme.Fonts.display();
        int aColor = PWPTheme.Colors.withAlpha(0xFF12151A, alpha);
        int bColor = PWPTheme.Colors.withAlpha(PWPTheme.Colors.ACCENT, alpha);

        RoundedRect.fill(g, x, y, w, h, PWPTheme.Spacing.RADIUS_MEDIUM, aColor);
        RoundedRect.border(g, x, y, w, h, PWPTheme.Spacing.RADIUS_MEDIUM, 1, bColor);

        // Preview color block
        int pColor = PWPTheme.Colors.withAlpha(color, alpha);
        g.fill(x+10, y+10, x+w-10, y+50, pColor);

        // Label
        int lc = PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_ACCENT, alpha);
        g.drawString(f, Component.literal(label), x+w/2-f.width(label)/2, y+58, lc, false);

        // Name
        String n = name;
        if (f.width(n) > w-16) n = f.plainSubstrByWidth(n, w-20)+"...";
        int nc = PWPTheme.Colors.withAlpha(0xFFFFFF, alpha);
        g.drawString(f, Component.literal(n), x+w/2-f.width(n)/2, y+74, nc, false);

        // Votes
        int vc = PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_DIM, alpha);
        g.drawString(f, Component.literal(votes), x+w/2-f.width(votes)/2, y+92, vc, false);
    }

    private void drawWinnerSplitCard(GuiGraphics g, int x, int baseY, int w, int h, String label, Option wF1, Option wF2, long now, long animStart, int delayMs) {
        long elapsed = now - animStart - delayMs;
        if (elapsed < 0) return;
        float t = Math.min(elapsed / 600f, 1);
        float eased = eoc(t);
        int y = baseY + (int)(50 * (1 - eased));
        int alpha = Math.min(255, (int)(255 * eased));
        if (alpha < 5) return;

        var f = PWPTheme.Fonts.display();
        int aColor = PWPTheme.Colors.withAlpha(0xFF12151A, alpha);
        int bColor = PWPTheme.Colors.withAlpha(PWPTheme.Colors.ACCENT, alpha);
        int halfW = w / 2;

        RoundedRect.fill(g, x, y, w, h, PWPTheme.Spacing.RADIUS_MEDIUM, aColor);
        RoundedRect.border(g, x, y, w, h, PWPTheme.Spacing.RADIUS_MEDIUM, 1, bColor);

        // Label at top
        int lc = PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_ACCENT, alpha);
        g.drawString(f, Component.literal(label), x+w/2-f.width(label)/2, y+6, lc, false);

        // Divider line
        int divX = x + halfW;
        g.fill(divX-1, y+18, divX+1, y+h-6, PWPTheme.Colors.BORDER);

        // Left faction (Team 1)
        int ly = y + 22;
        if (wF1 != null && wF1.icon != null) {
            RenderSystem.setShaderColor(1,1,1, alpha/255f);
            g.blit(wF1.icon, x+halfW/2-20, ly, 0, 0, 40, 23, 40, 23);
            ly += 27;
        }
        if (wF1 != null) {
            int nc = PWPTheme.Colors.withAlpha(0xFFFFFF, alpha);
            g.drawString(f, Component.literal(wF1.name), x+halfW/2-f.width(wF1.name)/2, ly, nc, false);
            ly += 12;
            int vc = PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_DIM, alpha);
            g.drawString(f, Component.literal(wF1.votes+" голос"+suffix(wF1.votes)), x+halfW/2-f.width(wF1.votes+" голос"+suffix(wF1.votes))/2, ly, vc, false);
        }
        String t1n = "КОМАНДА 1";
        int tc1 = PWPTheme.Colors.withAlpha(0xFF3D6FA5, alpha);
        g.drawString(f, Component.literal(t1n), x+halfW/2-f.width(t1n)/2, y+h-14, tc1, false);

        // Right faction (Team 2)
        int rx = x + halfW;
        ly = y + 22;
        if (wF2 != null && wF2.icon != null) {
            RenderSystem.setShaderColor(1,1,1, alpha/255f);
            g.blit(wF2.icon, rx+halfW/2-20, ly, 0, 0, 40, 23, 40, 23);
            ly += 27;
        }
        if (wF2 != null) {
            int nc = PWPTheme.Colors.withAlpha(0xFFFFFF, alpha);
            g.drawString(f, Component.literal(wF2.name), rx+halfW/2-f.width(wF2.name)/2, ly, nc, false);
            ly += 12;
            int vc = PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_DIM, alpha);
            g.drawString(f, Component.literal(wF2.votes+" голос"+suffix(wF2.votes)), rx+halfW/2-f.width(wF2.votes+" голос"+suffix(wF2.votes))/2, ly, vc, false);
        }
        String t2n = "КОМАНДА 2";
        int tc2 = PWPTheme.Colors.withAlpha(0xFFA53D3D, alpha);
        g.drawString(f, Component.literal(t2n), rx+halfW/2-f.width(t2n)/2, y+h-14, tc2, false);

        RenderSystem.setShaderColor(1,1,1,1);
    }

    public boolean mouseClicked(double mx, double my) {
        long now = System.currentTimeMillis();
        if (transitioning) return false;

        // Card clicks
        if (hoveredCard >= 0) {
            switch (currentPhase) {
                case MAP -> selMap = hoveredCard;
                case MODE -> selMode = hoveredCard;
                case FACTION -> {
                    int col = hoveredCard / 3, row = hoveredCard % 3;
                    if (col == 0) selFact1 = row; else selFact2 = row;
                }
            }
            return true;
        }

        // Vote button
        if (currentPhase == Phase.MAP || currentPhase == Phase.MODE || currentPhase == Phase.FACTION) {
            int bw = 180, bh = 28, bx = (screenWidth - bw) / 2;
            int by = voteButtonY;
            if (mx >= bx && mx <= bx+bw && my >= by && my <= by+bh) {
                if (currentPhase == Phase.FACTION) {
                    if ((selFact1 < 0 && votFact1 < 0) || (selFact2 < 0 && votFact2 < 0)) return false;
                    if (selFact1 == votFact1 && selFact2 == votFact2) return false;
                    String blueId = selFact1 >= 0 ? factOpts.get(selFact1).id : "";
                    String redId = selFact2 >= 0 ? factOpts.get(3+selFact2).id : "";
                    if (blueId.isEmpty() || redId.isEmpty()) return false;
                    try {
                        var conn = Minecraft.getInstance().player.connection;
                        if (conn != null) conn.sendCommand("votefaction " + blueId + " " + redId);
                    } catch (Exception ignored) {}
                    votFact1 = selFact1; votFact2 = selFact2;
                    recalc(factOpts);
                    return true;
                } else {
                    int sel = currentPhase==Phase.MAP ? selMap : selMode;
                    int vot = currentPhase==Phase.MAP ? votMap : votMode;
                    if (sel >= 0 && sel != vot) {
                        List<Option> opts = currentPhase==Phase.MAP ? mapOpts : modeOpts;
                        String cmdId = opts.get(sel).id;
                        if (cmdId == null || cmdId.isEmpty()) return false;
                        String cmd = currentPhase == Phase.MAP ? "votemap " : "votemode ";
                        try {
                            var conn = Minecraft.getInstance().player.connection;
                            if (conn != null) conn.sendCommand(cmd + cmdId);
                        } catch (Exception ignored) {}
                        if (currentPhase == Phase.MAP) { votMap = sel; recalc(mapOpts); }
                        else { votMode = sel; recalc(modeOpts); }
                        return true;
                    }
                }
            }
        }

        // FINISHED -> new vote button (block during slide-up animation)
        if (currentPhase == Phase.FINISHED) {
            if (now - phaseStartTime < 600) return false;
            int bw = 180, bh = 28, bx = (screenWidth - bw) / 2;
            if (mx >= bx && mx <= bx+bw && my >= voteButtonY && my <= voteButtonY+bh) {
                resetVote();
                return true;
            }
        }

        return false;
    }

    private void resetVote() {
        mapOpts.clear(); modeOpts.clear(); factOpts.clear();
        selMap = -1; selMode = -1; selFact1 = -1; selFact2 = -1;
        votMap = -1; votMode = -1; votFact1 = -1; votFact2 = -1;
        hoveredCard = -1;
        currentPhase = Phase.IDLE;
        phaseStartTime = System.currentTimeMillis();
        transitioning = false;
    }
}