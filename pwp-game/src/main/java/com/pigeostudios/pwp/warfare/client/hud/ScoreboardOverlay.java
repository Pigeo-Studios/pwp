package com.pigeostudios.pwp.warfare.client.hud;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.stats.ScoreboardData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.data.ClientDataResponses;
import com.pwp.coreclient.network.data.DataRequest;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.layout.Rect;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Табло по Tab в матче (этап 15 ROADMAP) вместо ванильного списка игроков. Слева своя команда по отрядам
 * (роль, У/С, оживления, очки, пинг), справа враг — ники и очки. Данные — {@link ScoreboardData} (сервер решает,
 * что видно), запрос раз в секунду, пока Tab зажат. В лобби — ванильный список.
 */
@OnlyIn(Dist.CLIENT)
public final class ScoreboardOverlay {
    private static final Logger LOG = LoggerFactory.getLogger(ScoreboardOverlay.class);
    private static JsonObject state;
    private static int ticks;

    private ScoreboardOverlay() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(ScoreboardOverlay::onTick);
        MinecraftForge.EVENT_BUS.addListener(ScoreboardOverlay::onOverlay);
        ClientDataResponses.listen((type, json) -> {
            if (!ScoreboardData.TYPE.equals(type)) return;
            try {
                JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                Minecraft.getInstance().execute(() -> state = o);
            } catch (RuntimeException e) {
                LOG.warn("Табло: битый ответ", e);
            }
        });
    }

    private static boolean active() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null
                && mc.getConnection() != null
                && !com.pwp.coreclient.lobby.LobbyHud.isOnLobby()
                && mc.options.keyPlayerList.isDown();
    }

    private static void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!active()) {
            ticks = 19; // при следующем нажатии — запрос сразу
            return;
        }
        if (++ticks >= 20) {
            ticks = 0;
            com.pwp.coreclient.network.PacketHandler.INSTANCE.sendToServer(new DataRequest(ScoreboardData.TYPE, ""));
        }
    }

    private static void onOverlay(RenderGuiOverlayEvent.Pre e) {
        if (!e.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_LIST.id())) return;
        if (!active() || !WHud.hudVisible()) return;
        e.setCanceled(true);
        render(
                e.getGuiGraphics(),
                e.getWindow().getGuiScaledWidth(),
                e.getWindow().getGuiScaledHeight());
    }

    private record Row(
            String name,
            String squad,
            int squadId,
            boolean leader,
            String role,
            int k,
            int d,
            int r,
            int score,
            int ping,
            boolean downed) {}

    private static List<Row> rows(String team) {
        List<Row> out = new ArrayList<>();
        if (state == null || !state.has(team) || !state.get(team).isJsonArray()) return out;
        for (JsonElement el : state.getAsJsonArray(team)) {
            JsonObject p = el.getAsJsonObject();
            out.add(new Row(
                    str(p, "name"),
                    str(p, "squad"),
                    num(p, "squadId"),
                    p.has("leader") && p.get("leader").getAsBoolean(),
                    str(p, "role"),
                    num(p, "kills"),
                    num(p, "deaths"),
                    num(p, "revives"),
                    num(p, "score"),
                    p.has("ping") ? num(p, "ping") : -1,
                    p.has("downed") && p.get("downed").getAsBoolean()));
        }
        return out;
    }

    static void render(GuiGraphics g, int sw, int sh) {
        Font font = Minecraft.getInstance().font;
        String me = Minecraft.getInstance().player == null
                ? ""
                : Minecraft.getInstance().player.getGameProfile().getName();
        String myTeam = Minecraft.getInstance().player != null
                        && Minecraft.getInstance().player.getTeam() != null
                ? Minecraft.getInstance().player.getTeam().getName().toUpperCase(java.util.Locale.ROOT)
                : "BLUE";
        if (!myTeam.equals("RED")) myTeam = "BLUE";
        String enemy = myTeam.equals("BLUE") ? "RED" : "BLUE";

        int w = Math.min(sw - 24, 620);
        int x = (sw - w) / 2;
        int y = Math.max(8, sh / 10);
        int lh = font.lineHeight + 2;
        int maxH = sh - y - 12;
        int headerH = lh + 6;
        Rect panel = Rect.of(x, y, w, maxH);
        // заголовок: команды, тикеты, режим
        UIDraw.glass(g, panel, 0.92F, true);
        String mode = state == null ? "" : str(state, "mode").toUpperCase(java.util.Locale.ROOT);
        Component title = Component.translatable(
                "gui.pwpwarfare.scoreboard.title",
                faction(myTeam),
                tickets(myTeam),
                tickets(enemy),
                faction(enemy),
                mode);
        UIDraw.header(g, font, Rect.of(x, y, w, headerH), UIIcon.PROFILE, title, PWPTheme.Text.HEADING, 1F);
        if (state == null) {
            UIDraw.text(
                    g,
                    font,
                    Component.translatable("pwp_core.ui.loading"),
                    x + 8,
                    y + headerH + 6,
                    PWPTheme.Text.MUTED,
                    false);
            return;
        }
        int colGap = 8;
        int leftW = (w - colGap) * 62 / 100;
        int rightW = w - colGap - leftW;
        int top = y + headerH + 4;
        int bottom = y + maxH - 4;
        drawTeam(g, font, rows(myTeam), x + 4, top, leftW - 4, bottom, lh, me, true, myTeam);
        drawTeam(g, font, rows(enemy), x + leftW + colGap, top, rightW - 4, bottom, lh, me, false, enemy);
    }

    /** Своя команда — отряды и подробности; враг — ники и очки (сервер подробностей не шлёт). */
    private static void drawTeam(
            GuiGraphics g,
            Font font,
            List<Row> list,
            int x,
            int y,
            int w,
            int bottom,
            int lh,
            String me,
            boolean detailed,
            String team) {
        int teamColor = team.equals("BLUE") ? WTheme.TEAM_BLUE : WTheme.TEAM_RED;
        UIDraw.rect(g, Rect.of(x, y, w, 2), teamColor);
        y += 4;
        // колонки
        int cScore = x + w - font.width("0000");
        int cPing = cScore - font.width("000ms") - 6;
        int cR = cPing - font.width("00") - 8;
        int cD = cR - font.width("00") - 8;
        int cK = cD - font.width("00") - 8;
        int cRole = x + w * 42 / 100;
        if (detailed) {
            int mc = PWPTheme.Text.MUTED;
            text(g, font, Component.translatable("gui.pwpwarfare.scoreboard.player"), x + 2, y, mc);
            text(g, font, Component.translatable("gui.pwpwarfare.scoreboard.role"), cRole, y, mc);
            text(g, font, Component.literal("У"), cK, y, mc);
            text(g, font, Component.literal("С"), cD, y, mc);
            text(g, font, Component.literal("О"), cR, y, mc);
            text(g, font, Component.literal("ms"), cPing, y, mc);
        }
        text(g, font, Component.translatable("gui.pwpwarfare.scoreboard.score"), cScore, y, PWPTheme.Text.MUTED);
        y += lh;
        List<Row> sorted = new ArrayList<>(list);
        int shown = 0;
        if (detailed) {
            // по отрядам: сначала отряды по id, «без отряда» в конце; лидер первым
            Map<String, List<Row>> bySquad = new LinkedHashMap<>();
            sorted.sort(Comparator.comparingInt((Row r) -> r.squadId() == 0 ? Integer.MAX_VALUE : r.squadId())
                    .thenComparing(r -> !r.leader())
                    .thenComparing(Comparator.comparingInt(Row::score).reversed()));
            for (Row r : sorted)
                bySquad.computeIfAbsent(r.squadId() == 0 ? "" : r.squad(), k -> new ArrayList<>())
                        .add(r);
            for (var en : bySquad.entrySet()) {
                if (y + lh > bottom) break;
                Component head = en.getKey().isEmpty()
                        ? Component.translatable("gui.pwpwarfare.scoreboard.no_squad")
                        : Component.literal(
                                en.getKey() + "  ·  " + en.getValue().size());
                UIDraw.rect(g, Rect.of(x, y - 1, w, lh), WTheme.MAP_LABEL_BG);
                text(g, font, head, x + 2, y, PWPTheme.Text.HEADING);
                y += lh;
                for (Row r : en.getValue()) {
                    if (y + lh > bottom) break;
                    int c = r.name().equals(me)
                            ? PWPTheme.Colors.TEXT_ACCENT
                            : r.downed() ? PWPTheme.Text.DANGER : PWPTheme.Text.BODY;
                    String name = (r.leader() ? "★ " : "  ") + r.name();
                    text(g, font, UIDraw.ellipsize(font, Component.literal(name), cRole - x - 6), x + 2, y, c);
                    text(
                            g,
                            font,
                            UIDraw.ellipsize(
                                    font, Component.literal(r.role().isEmpty() ? "—" : r.role()), cK - cRole - 6),
                            cRole,
                            y,
                            PWPTheme.Text.MUTED);
                    text(g, font, Component.literal(String.valueOf(r.k())), cK, y, c);
                    text(g, font, Component.literal(String.valueOf(r.d())), cD, y, c);
                    text(g, font, Component.literal(String.valueOf(r.r())), cR, y, c);
                    text(
                            g,
                            font,
                            Component.literal(r.ping() < 0 ? "—" : String.valueOf(r.ping())),
                            cPing,
                            y,
                            pingColor(r.ping()));
                    text(g, font, Component.literal(String.valueOf(r.score())), cScore, y, PWPTheme.Text.HEADING);
                    y += lh;
                    shown++;
                }
            }
        } else {
            sorted.sort(Comparator.comparingInt(Row::score).reversed());
            for (Row r : sorted) {
                if (y + lh > bottom) break;
                text(
                        g,
                        font,
                        UIDraw.ellipsize(font, Component.literal(r.name()), cScore - x - 8),
                        x + 2,
                        y,
                        PWPTheme.Text.BODY);
                text(g, font, Component.literal(String.valueOf(r.score())), cScore, y, PWPTheme.Text.HEADING);
                y += lh;
                shown++;
            }
        }
        if (shown < list.size() && y <= bottom) {
            text(
                    g,
                    font,
                    Component.translatable("gui.pwpwarfare.scoreboard.more", list.size() - shown),
                    x + 2,
                    Math.min(y, bottom - lh),
                    PWPTheme.Text.MUTED);
        }
    }

    private static int pingColor(int ping) {
        if (ping < 0) return PWPTheme.Text.MUTED;
        if (ping < 80) return PWPTheme.Text.BODY;
        return ping < 200 ? PWPTheme.Text.ACCENT : PWPTheme.Text.DANGER;
    }

    private static void text(GuiGraphics g, Font font, Component c, int x, int y, int color) {
        UIDraw.text(g, font, c, x, y, color, false);
    }

    private static String faction(String team) {
        String f = state == null ? "" : str(state, team.equals("BLUE") ? "blueFaction" : "redFaction");
        return f.isEmpty() ? team : f.toUpperCase(java.util.Locale.ROOT);
    }

    private static int tickets(String team) {
        return state == null ? 0 : num(state, team.equals("BLUE") ? "blueTickets" : "redTickets");
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : "";
    }

    private static int num(JsonObject o, String k) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsInt() : 0;
    }

    /** Для превью: подставить данные. */
    public static void setPreviewState(JsonObject o) {
        state = o;
    }
}
