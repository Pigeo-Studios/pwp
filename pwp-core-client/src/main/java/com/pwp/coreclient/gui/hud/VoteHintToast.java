package com.pwp.coreclient.gui.hud;

import com.pwp.coreclient.gui.components.RoundedRect;
import com.pwp.coreclient.gui.screens.PWPLobbyScreen;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.network.LobbyStatePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/**
 * HUD-баннер-подсказка голосования (правый край, вертикальный центр).
 *
 * Чисто клиентская машина состояний ОТОБРАЖЕНИЯ — server phase остаётся
 * authoratative (приходит в LobbyStatePacket раз в секунду). Класс отвечает за:
 *  - детект новой фазы голосования (phaseId: счётчик, растёт при смене фазы
 *    и при «прыжке» таймера вверх > 30 с — перезапуск голосования);
 *  - таймеры показа (первичный показ, повтор не чаще 30 с);
 *  - жизненный цикл баннера (слайд 300 мс → 10 с видимости → фейд 300 мс);
 *  - звук при появлении;
 *  - рендер баннера и точки-индикатора ◉.
 *
 * Производительность: строки и размеры собираются ТОЛЬКО в onLobbyState
 * (раз в секунду, на пакете); рендер — только draw + анимационный прогресс.
 * Шрифт кэшируется лениво (без рефлексии на кадр). Всё время — System.nanoTime().
 */
public final class VoteHintToast {

    // ---- Тайминги (наносекунды) ----
    private static final long SLIDE_IN_NS        = 300_000_000L;
    private static final long FADE_OUT_NS        = 300_000_000L;
    private static final long LIFETIME_NS        = 10_000_000_000L;
    private static final long FADE_START_NS      = LIFETIME_NS - FADE_OUT_NS;
    private static final long REPEAT_INTERVAL_NS = 30_000_000_000L;
    private static final long PULSE_PERIOD_NS    = 2_000_000_000L;

    // ---- Геометрия баннера (динамическая ширина по контенту) ----
    private static final int MARGIN            = PWPTheme.Spacing.MD;      // 12
    private static final int PAD               = PWPTheme.Spacing.SM;      // 8
    private static final int GAP               = 4;                        // title → body
    private static final int ICON_DOT          = 10;
    private static final int ICON_TEXT_GAP     = 6;
    private static final int KEYCAP_GAP        = 8;
    private static final int MIN_WIDTH         = 220;
    private static final int MAX_WIDTH         = 340;
    private static final int DOT_SIZE          = 10;

    // ---- Машина состояний показа ----
    private static int lastRawPhase = -1;
    private static int lastRemainingSeconds = -1;
    private static int voteInstance = 0;
    private static int lastPhaseId = -1;
    private static int lastViewedPhaseId = -1;

    private static boolean toastActive;
    private static long toastStartNs;
    private static long lastToastShownAtNs;
    private static long pulseStartNs;

    // ---- Кэш рендера: собирается на пакете, рендер только рисует ----
    private static boolean layoutReady;
    private static String cachedTitle = "";
    private static String cachedBody = "";
    private static String cachedKeycap = "";
    private static int cachedBannerW;
    private static int cachedBannerH;

    /** Ленивый кэш шрифта: PWPTheme.Fonts.display() делает рефлексию — не по кадру. */
    private static Font cachedFont;

    // ---- Hit-зоны для клика (последний отрендеренный кадр) ----
    private static int hitX, hitY, hitW, hitH;
    private static boolean dotHitActive;
    private static int dotHitX, dotHitY, dotHitSize;

    private VoteHintToast() {}

    /**
     * Вызывается при каждом валидном LobbyStatePacket (клиентский поток).
     * Детектит смену фазы голосования — единственный источник сброса таймеров.
     */
    public static void onLobbyState(LobbyStatePacket pkt) {
        if (pkt == null) return;
        int phaseId = computePhaseId(pkt);
        if (!isVotePhase(pkt.phase)) {
            // Голосование закончилось — баннер немедленно прячется
            toastActive = false;
            layoutReady = false;
            return;
        }
        if (phaseId != lastPhaseId) {
            // Новая фаза: первичный показ + звук + старт пульса точки
            lastPhaseId = phaseId;
            pulseStartNs = System.nanoTime();
            showToast(true);
        } else if (System.nanoTime() - lastToastShownAtNs >= REPEAT_INTERVAL_NS
                && !PWPLobbyScreen.isOpen()
                && isPhaseUnseen()) {
            // Повтор не чаще 30 с — ТОЛЬКО для «новичка» (не открывал меню и не голосовал),
            // без звука: опытные игроки повторных уведомлений не получают
            showToast(false);
        }
        // Проголосовал (в т.ч. из чата, не открывая экран) — точка «спокойная», как после просмотра
        if (hasVoted(pkt)) {
            lastViewedPhaseId = lastPhaseId;
        }
        // Раз в секунду обновляем тексты/размеры (лидер и голоса меняются) — рендер не перезапускается
        rebuildLayout(pkt);
    }

    /** Игрок открыл/открывает экран — точка перестаёт быть «новичковой» для текущей фазы. */
    public static void markViewed() {
        lastViewedPhaseId = lastPhaseId;
    }

    public static boolean isPhaseUnseen() {
        return lastPhaseId != lastViewedPhaseId;
    }

    public static void reset() {
        lastRawPhase = -1;
        lastRemainingSeconds = -1;
        voteInstance = 0;
        lastPhaseId = -1;
        lastViewedPhaseId = -1;
        toastActive = false;
        toastStartNs = 0;
        lastToastShownAtNs = 0;
        pulseStartNs = 0;
        layoutReady = false;
        dotHitActive = false;
    }

    public static boolean isVotePhase(int phase) {
        return phase == LobbyStatePacket.PHASE_MAP_VOTE
                || phase == LobbyStatePacket.PHASE_MODE_VOTE
                || phase == LobbyStatePacket.PHASE_FACTION_VOTE;
    }

    /** Клик по баннеру или точке ◉ (scaled GUI-координаты) — открыть лобби-меню. */
    public static boolean clickAt(double mx, double my) {
        if (PWPLobbyScreen.isOpen()) return false;
        LobbyStatePacket st = PWPLobbyScreen.getLastState();
        if (st == null || !isVotePhase(st.phase)) return false;
        if (toastActive && layoutReady
                && mx >= hitX && mx <= hitX + hitW && my >= hitY && my <= hitY + hitH) {
            return true;
        }
        if (dotHitActive
                && mx >= dotHitX && mx <= dotHitX + dotHitSize
                && my >= dotHitY && my <= dotHitY + dotHitSize) {
            return true;
        }
        return false;
    }

    /** Точка ◉ в правом верхнем углу + ховер-тултип (пользователь должен понимать, что это). */
    public static void renderDot(GuiGraphics gui, int mouseX, int mouseY) {
        LobbyStatePacket st = PWPLobbyScreen.getLastState();
        if (st == null || !isVotePhase(st.phase)) return;
        int x = gui.guiWidth() - MARGIN - DOT_SIZE;
        int y = MARGIN;
        // Hit-зона для клика/ховера чуть шире самой точки (точка 10×10 — сложно попасть)
        dotHitActive = true;
        dotHitX = x - 6;
        dotHitY = y - 6;
        dotHitSize = DOT_SIZE + 12;

        float alpha;
        if (isPhaseUnseen()) {
            // Плавная пульсация 2 с, time-based, не перезапускается каждым пакетом
            long now = System.nanoTime();
            float t = ((now - pulseStartNs) % PULSE_PERIOD_NS) / (float) PULSE_PERIOD_NS;
            float wave = 0.5F - 0.5F * (float) Math.cos(t * Math.PI * 2.0);
            alpha = 0.4F + 0.6F * wave;
        } else {
            alpha = 0.5F;
        }
        RoundedRect.fill(gui, x, y, DOT_SIZE, DOT_SIZE, PWPTheme.Spacing.RADIUS_ROUND,
                PWPTheme.Colors.withAlpha(phaseColor(st.phase), Math.round(alpha * 255.0F)));

        // Ховер: тултип слева от точки («Голосование идёт [TAB]»)
        boolean hover = mouseX >= dotHitX && mouseX <= dotHitX + dotHitSize
                && mouseY >= dotHitY && mouseY <= dotHitY + dotHitSize;
        if (hover && !PWPLobbyScreen.isOpen()) {
            Font font = font();
            String tip = lang("pwp.vote.hint.dot_tooltip");
            String keycap = keycapLabel(font);
            int keyW = VoteKeyHint.width(font, keycap);
            int keyH = VoteKeyHint.height(font);
            int tipH = Math.max(font.lineHeight, keyH) + 8;
            int tipW = font.width(tip) + 6 + keyW + 12;
            int tipX = Math.max(4, x - tipW - 6);
            int tipY = y + DOT_SIZE / 2 - tipH / 2;
            RoundedRect.fill(gui, tipX, tipY, tipW, tipH, PWPTheme.Spacing.RADIUS_SMALL,
                    PWPTheme.Styles.Tooltip.BG);
            RoundedRect.border(gui, tipX, tipY, tipW, tipH, PWPTheme.Spacing.RADIUS_SMALL, 1,
                    PWPTheme.Styles.Tooltip.BORDER);
            gui.drawString(font, tip, tipX + 6, tipY + (tipH - font.lineHeight) / 2 + 1,
                    PWPTheme.Styles.Tooltip.TEXT, false);
            VoteKeyHint.render(gui, font, tipX + 6 + font.width(tip) + 6, tipY + (tipH - keyH) / 2,
                    keycap, PWPTheme.Colors.TEXT_PRIMARY);
        }
    }

    /** Баннер-подсказка (правый край, вертикальный центр). Только draw — кэш собран на пакете. */
    public static void render(GuiGraphics gui) {
        if (!toastActive || !layoutReady) return;
        if (PWPLobbyScreen.isOpen()) return;
        long now = System.nanoTime();
        long age = now - toastStartNs;
        if (age >= LIFETIME_NS) {
            toastActive = false;
            return;
        }
        LobbyStatePacket st = PWPLobbyScreen.getLastState();
        if (st == null || !isVotePhase(st.phase)) {
            toastActive = false;
            return;
        }

        Font font = font();
        int w = cachedBannerW;
        int h = cachedBannerH;
        int finalX = gui.guiWidth() - MARGIN - w;
        int finalY = gui.guiHeight() / 2 - h / 2;

        // Единый прогресс анимации: слайд 300 мс → статично → фейд 300 мс.
        // Один progress для всех элементов — фон/рамка/иконка/текст не разъезжаются.
        float slide = easeOutCubic(clamp01(age / (float) SLIDE_IN_NS));
        float fade = 1.0F;
        if (age >= FADE_START_NS) {
            fade = clamp01(1.0F - (age - FADE_START_NS) / (float) FADE_OUT_NS);
        }
        int alpha = Math.round(fade * 255.0F);
        if (alpha <= 0) return;

        int x = finalX + Math.round((1.0F - slide) * (w + MARGIN));
        int y = finalY;
        int accent = phaseColor(st.phase);

        // Hit-зона для клика (последний отрендеренный кадр)
        hitX = x;
        hitY = y;
        hitW = w;
        hitH = h;

        RoundedRect.fill(gui, x, y, w, h, PWPTheme.Spacing.RADIUS_MEDIUM,
                PWPTheme.Colors.withAlpha(PWPTheme.Colors.SURFACE, alpha));
        RoundedRect.border(gui, x, y, w, h, PWPTheme.Spacing.RADIUS_MEDIUM, 1,
                PWPTheme.Colors.withAlpha(PWPTheme.Colors.BORDER_LIGHT, alpha));

        // Ряд 1: иконка фазы + заголовок слева, кейкап справа — тело в ряду 2 занимает всю ширину
        int titleH = font.lineHeight;
        int keycapH = VoteKeyHint.height(font);
        int row1H = Math.max(titleH, keycapH);
        int iconY = y + PAD + (row1H - ICON_DOT) / 2;
        int titleY = y + PAD + (row1H - titleH) / 2;
        RoundedRect.fill(gui, x + PAD, iconY, ICON_DOT, ICON_DOT, PWPTheme.Spacing.RADIUS_ROUND,
                PWPTheme.Colors.withAlpha(accent, alpha));
        gui.drawString(font, cachedTitle, x + PAD + ICON_DOT + ICON_TEXT_GAP, titleY,
                PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_PRIMARY, alpha), false);

        int keyY = y + PAD + (row1H - keycapH) / 2;
        VoteKeyHint.render(gui, font, x + w - PAD - VoteKeyHint.width(font, cachedKeycap), keyY,
                cachedKeycap, PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_PRIMARY, alpha));

        // Ряд 2: тело (лидер голосования) на всю ширину баннера
        int bodyY = y + PAD + row1H + GAP;
        gui.drawString(font, cachedBody, x + PAD, bodyY,
                PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_SECONDARY, alpha), false);
    }

    // ---- Вспомогательное ----

    /**
     * Уникальный id раунда голосования: счётчик растёт при смене фазы и при
     * «прыжке» таймера вверх (перезапуск голосования без смены фазы).
     * Обычные тики пакета (раз в секунду) id не меняют.
     */
    private static int computePhaseId(LobbyStatePacket pkt) {
        int phase = pkt.phase;
        int remaining = pkt.remainingSeconds;
        if (phase != lastRawPhase) {
            lastRawPhase = phase;
            lastRemainingSeconds = remaining;
            voteInstance++;
            return voteInstance;
        }
        if (remaining > lastRemainingSeconds + 30) {
            voteInstance++;
        }
        lastRemainingSeconds = remaining;
        return voteInstance;
    }

    /** Сборка строк и размеров по свежему пакету. Вызывается раз в секунду. */
    private static void rebuildLayout(LobbyStatePacket st) {
        Font font = font();
        int contentMax = MAX_WIDTH - PAD * 2;

        // Кейкап живёт в ряду 1 (справа от заголовка) — ряду 2 (тело/лидер) достаётся вся ширина
        String keycap = keycapLabel(font);
        int keycapW = VoteKeyHint.width(font, keycap);
        int titleMax = contentMax - ICON_DOT - ICON_TEXT_GAP - KEYCAP_GAP - keycapW;

        String title = fit(font, lang("pwp.vote.hint.title." + phaseSuffix(st.phase)), titleMax);
        String body = fit(font, bodyFor(st), contentMax);

        int titleRowW = ICON_DOT + ICON_TEXT_GAP + font.width(title) + KEYCAP_GAP + keycapW;
        int bodyRowW = font.width(body);

        // Ширина по контенту, без пустоты; жёсткий потолок — текст никогда не уйдёт за экран
        int bannerW = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, Math.max(titleRowW, bodyRowW) + PAD * 2));
        int titleH = font.lineHeight;
        int row1H = Math.max(titleH, VoteKeyHint.height(font));
        int bannerH = PAD + row1H + GAP + font.lineHeight + PAD;

        cachedTitle = title;
        cachedBody = body;
        cachedKeycap = keycap;
        cachedBannerW = bannerW;
        cachedBannerH = bannerH;
        layoutReady = true;
    }

    private static void showToast(boolean withSound) {
        toastStartNs = System.nanoTime();
        lastToastShownAtNs = toastStartNs;
        toastActive = true;
        if (withSound) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.getSoundManager() != null) {
                mc.getSoundManager().play(SimpleSoundInstance.forUI(
                        SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 0.8F));
            }
        }
    }

    private static String bodyFor(LobbyStatePacket st) {
        // Проголосовал — показываем и статус, и текущего лидера («Вы проголосовали · <лидер>»)
        String leader = leaderPart(st);
        if (hasVoted(st)) {
            return lang("pwp.vote.hint.you_voted_leader", lang("pwp.vote.hint.you_voted"), leader);
        }
        return leader;
    }

    /** Строка лидера по данным пакета (не зависит от персонального голоса). */
    private static String leaderPart(LobbyStatePacket st) {
        switch (st.phase) {
            case LobbyStatePacket.PHASE_MAP_VOTE -> {
                int idx = bestIndex(st.voteCounts);
                if (idx < 0) return lang("pwp.vote.hint.waiting");
                return lang("pwp.vote.hint.leader", safeName(st.mapDisplayNames, idx), st.voteCounts[idx]);
            }
            case LobbyStatePacket.PHASE_MODE_VOTE -> {
                int idx = bestIndex(st.modeVoteCounts);
                if (idx < 0) return lang("pwp.vote.hint.waiting");
                return lang("pwp.vote.hint.leader", safeName(st.modeDisplayNames, idx), st.modeVoteCounts[idx]);
            }
            case LobbyStatePacket.PHASE_FACTION_VOTE -> {
                int i1 = bestIndex(st.team1Votes);
                int i2 = bestIndex(st.team2Votes);
                if (i1 < 0 && i2 < 0) return lang("pwp.vote.hint.waiting");
                return lang("pwp.vote.hint.faction_leader",
                        safeName(st.team1Factions, i1), safeName(st.team2Factions, i2));
            }
            default -> { return lang("pwp.vote.hint.waiting"); }
        }
    }

    private static boolean hasVoted(LobbyStatePacket st) {
        return switch (st.phase) {
            case LobbyStatePacket.PHASE_MAP_VOTE -> st.myMapVote >= 0;
            case LobbyStatePacket.PHASE_MODE_VOTE -> st.myModeVote >= 0;
            case LobbyStatePacket.PHASE_FACTION_VOTE -> st.myFaction1 >= 0 || st.myFaction2 >= 0;
            default -> false;
        };
    }

    /** Индекс лидера (голоса > 0), -1 если голосов нет. Ничья — первый по списку. */
    private static int bestIndex(int[] votes) {
        if (votes == null || votes.length == 0) return -1;
        int best = -1, bestV = 0;
        for (int i = 0; i < votes.length; i++) {
            if (votes[i] > bestV) {
                bestV = votes[i];
                best = i;
            }
        }
        return best;
    }

    private static String safeName(String[] arr, int idx) {
        if (idx < 0 || arr == null || idx >= arr.length || arr[idx] == null || arr[idx].isEmpty()) return "-";
        return arr[idx];
    }

    /** Текущая клавиша (учитывает переназначение игроком в настройках). */
    private static String keycapLabel(Font font) {
        String label = LobbyKeyMappings.OPEN_LOBBY_MENU_KEY.getTranslatedKeyMessage().getString();
        if (label == null || label.isEmpty()) label = lang("pwp.vote.key.tab");
        return fit(font, label, 60);
    }

    private static String phaseSuffix(int phase) {
        return switch (phase) {
            case LobbyStatePacket.PHASE_MODE_VOTE -> "mode";
            case LobbyStatePacket.PHASE_FACTION_VOTE -> "faction";
            default -> "map";
        };
    }

    private static int phaseColor(int phase) {
        return switch (phase) {
            case LobbyStatePacket.PHASE_MODE_VOTE -> PWPTheme.Colors.TEAM_BLUE;
            case LobbyStatePacket.PHASE_FACTION_VOTE -> PWPTheme.Colors.TEAM_RED;
            default -> PWPTheme.Colors.ACCENT;
        };
    }

    private static String lang(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    /** Обрезка длинных строк по ширине (защита от данных пакета и длинной локализации). */
    private static String fit(Font font, String text, int maxW) {
        if (text == null || text.isEmpty()) return "";
        if (font.width(text) <= maxW) return text;
        String t = text;
        while (!t.isEmpty() && font.width(t + "...") > maxW) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "...";
    }

    /** Ленивый кэш шрифта: PWPTheme.Fonts.display() делает рефлексию — не по кадру. */
    private static Font font() {
        if (cachedFont == null) {
            cachedFont = PWPTheme.Fonts.display();
        }
        return cachedFont;
    }

    private static float clamp01(float v) {
        return Math.max(0.0F, Math.min(1.0F, v));
    }

    private static float easeOutCubic(float t) {
        float u = 1.0F - clamp01(t);
        return 1.0F - u * u * u;
    }
}
