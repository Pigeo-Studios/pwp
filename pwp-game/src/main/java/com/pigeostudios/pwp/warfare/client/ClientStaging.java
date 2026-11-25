package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.gui.DeployScreen;
import com.pigeostudios.pwp.warfare.client.gui.TeamSelectionScreen;
import com.pigeostudios.pwp.warfare.client.gui.ui.WTheme;
import com.pigeostudios.pwp.warfare.network.PacketVoiceChannelState;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/**
 * Клиент стейджинга (этап 4): состояние ожидания из {@code StagingPackets.State}, ответ выбора
 * команды, плашка «ПОДГОТОВКА · М:СС · [K] … · [M] …» вместо ванильного титра, плашка голосового
 * канала при B/G. Клавиши — из реальных KeyMapping.
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT)
public final class ClientStaging {
    /** Пакеты ожидания идут раз в секунду; дольше этого молчания — плашку гасим. */
    private static final long STALE_MS = 3000L;
    /** Пере-открытие экрана команды игроку без команды (если он чем-то его закрыл). */
    private static final long TEAM_REOPEN_MS = 3000L;

    private static boolean waiting;
    private static boolean countdown;
    private static int secondsLeft;
    private static long receivedAt;
    private static long teamScreenAt;

    private ClientStaging() {}

    public static void onState(boolean w, int secs, boolean cd) {
        waiting = w;
        countdown = cd;
        secondsLeft = secs;
        receivedAt = System.currentTimeMillis();
    }

    /** Идёт фаза ожидания (стейджинг) — по свежему пакету. */
    public static boolean isWaiting() {
        return waiting && System.currentTimeMillis() - receivedAt < STALE_MS;
    }

    public static boolean isCountdown() {
        return countdown && System.currentTimeMillis() - receivedAt < STALE_MS;
    }

    /** Секунд до конца ожидания с учётом времени с последнего пакета. */
    public static int secondsLeft() {
        long el = (System.currentTimeMillis() - receivedAt) / 1000L;
        return (int) Math.max(0, secondsLeft - el);
    }

    public static String mmss(int s) {
        return String.format("%d:%02d", s / 60, s % 60);
    }

    public static boolean hasTeam() {
        var p = Minecraft.getInstance().player;
        String t = p != null && p.getTeam() != null ? p.getTeam().getName() : "";
        return t.equalsIgnoreCase("Blue") || t.equalsIgnoreCase("Red");
    }

    /** Есть ли у игрока роль (кит), с которой он реально в бою. */
    public static boolean hasKit() {
        String k = ClientData.myCurrentKit;
        return k != null && !k.isEmpty() && !"Unassigned".equals(k);
    }

    public static WarfareWorldData.Squad mySquad() {
        var p = Minecraft.getInstance().player;
        if (p == null) return null;
        String n = p.getScoreboardName();
        for (WarfareWorldData.Squad s : ClientData.clientSquads) {
            if (s.members.contains(n)) return s;
        }
        return null;
    }

    public static boolean isLeaderOrFireteamLeader() {
        var p = Minecraft.getInstance().player;
        WarfareWorldData.Squad s = mySquad();
        if (p == null || s == null) return false;
        String n = p.getScoreboardName();
        return n.equals(s.leader) || n.equals(s.bravoLeader) || n.equals(s.charlieLeader);
    }

    /** Ответ сервера на выбор команды: экран закрывается и открывается меню K только при успехе. */
    public static void onTeamResult(boolean ok, String key, String team) {
        Minecraft mc = Minecraft.getInstance();
        ClientData.teamSelectSent = false;
        if (mc.screen instanceof TeamSelectionScreen ts) {
            if (ok) mc.setScreen(new DeployScreen());
            else ts.showError(Component.translatable(key));
        } else if (mc.screen instanceof DeployScreen ds) {
            if (ok) ds.populateData();
            else if (mc.player != null) {
                mc.player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
            }
        } else if (!ok && mc.player != null) {
            mc.player.displayClientMessage(Component.translatable(key).withStyle(ChatFormatting.RED), true);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        // без команды в матче экран команды не теряется: закрыли — откроется снова (тупик §1 п.5)
        boolean matchServer = !ClientData.blueSpawns.isEmpty() || !ClientData.redSpawns.isEmpty();
        if (matchServer && !hasTeam() && mc.screen == null && !mc.player.isCreative() && !mc.player.isSpectator()) {
            long now = System.currentTimeMillis();
            if (now - teamScreenAt > TEAM_REOPEN_MS) {
                teamScreenAt = now;
                mc.setScreen(new TeamSelectionScreen());
            }
        }
    }

    // ───────────────────────────── HUD ─────────────────────────────

    /** Вызов из WarfareOverlay (слой чата): плашка ожидания + плашка голосового канала. */
    public static void renderHud(GuiGraphics g, int w) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        Font font = mc.font;
        int y = WTheme.STAGING_BAR_Y;
        if (isWaiting() || isCountdown()) {
            Component text = isCountdown()
                    ? Component.translatable("gui.pwpwarfare.staging.bar_go")
                    : Component.translatable(
                            "gui.pwpwarfare.staging.bar",
                            mmss(secondsLeft()),
                            ModKeyBindings.OPEN_SQUAD_MENU_KEY.getTranslatedKeyMessage(),
                            ModKeyBindings.OPEN_MAP_KEY.getTranslatedKeyMessage());
            y = bar(g, font, w, y, text, true) + Spacing.XS;
        }
        PacketVoiceChannelState.Channel ch = ClientData.currentVoiceChannel;
        if (ch != null && ch != PacketVoiceChannelState.Channel.LOCAL) {
            Component text;
            boolean ok;
            if (ch == PacketVoiceChannelState.Channel.SQUAD) {
                ok = mySquad() != null;
                text = Component.translatable(ok ? "gui.pwpwarfare.voice.squad" : "gui.pwpwarfare.voice.no_squad");
            } else {
                ok = isLeaderOrFireteamLeader();
                text = Component.translatable(
                        ok ? "gui.pwpwarfare.voice.command" : "gui.pwpwarfare.voice.leaders_only");
            }
            bar(g, font, w, y, text, ok);
        }
    }

    /** Компактная плашка по центру сверху; возвращает нижнюю границу. */
    private static int bar(GuiGraphics g, Font font, int screenW, int y, Component text, boolean accent) {
        int tw = Math.min(font.width(text), screenW - 4 * Spacing.MD);
        int w = tw + 2 * Spacing.MD;
        int h = font.lineHeight + 2 * Spacing.XS;
        Rect r = Rect.of((screenW - w) / 2, y, w, h);
        UIDraw.glass(g, r, 1F, accent);
        UIDraw.accentBarH(g, r.x(), r.bottom() - 1, r.w(), 1, accent ? PWPTheme.Colors.ACCENT : WTheme.STATUS_WARN);
        UIDraw.text(
                g,
                font,
                UIDraw.ellipsize(font, text, tw),
                r.x() + Spacing.MD,
                r.y() + Spacing.XS + 1,
                accent ? PWPTheme.Text.ACCENT : WTheme.STATUS_WARN,
                true);
        return r.bottom();
    }
}
