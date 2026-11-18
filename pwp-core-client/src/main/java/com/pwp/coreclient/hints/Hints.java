package com.pwp.coreclient.hints;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.settings.CoreUiSettings;
import com.pwp.coreclient.settings.UiSettings;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.layout.Rect;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Подсказки «что делать дальше» (этап 4): реестр {@code register(id, priority, once, when, text)},
 * выбор — чистый {@link HintSelector} (одна подсказка за раз, не перебивает, засыпает, разовые
 * запоминаются). Рисуется справа сверху маленькой плашкой с плавным появлением — и в HUD, и поверх
 * экранов (главное меню, лобби, меню K); в каком контексте показывать, решает условие {@code when}.
 * Текст строят из реальных клавиш ({@code KeyMapping.getTranslatedKeyMessage}).
 *
 * <p>«Уже показано» — файл {@code config/pwp_hints_seen.json} ({@code {"seen":[id…]}}, JsonObject
 * вручную). Выключатель — {@link #ENABLED} в настройках интерфейса (категория «Интерфейс»).
 */
@OnlyIn(Dist.CLIENT)
public final class Hints {
    private static final Logger LOG = LoggerFactory.getLogger(Hints.class);
    private static final String FILE = "pwp_hints_seen.json";
    /** Длительность появления/исчезания, мс. */
    private static final long FADE_MS = 350L;

    /** Показывать подсказки (по умолчанию вкл.). Читается покадрово в {@link #onTick}/{@link #render}. */
    public static final UiSettings.Toggle ENABLED = UiSettings.toggle(
                    "core.hints", CoreUiSettings.INTERFACE, "pwp_core.ui_settings.hints", true)
            .tooltip("pwp_core.ui_settings.hints.tip");

    /** Подсказка: условие и текст вычисляются на клиенте каждый тик / кадр. */
    public record Hint(String id, int priority, boolean once, BooleanSupplier when, Supplier<Component> text) {}

    private static final List<Hint> HINTS = new ArrayList<>();
    private static final HintSelector.State STATE = new HintSelector.State();
    private static boolean loaded;
    private static String shownId;
    private static Component shownText = Component.empty();
    private static long fadeStart;
    private static boolean fadingOut;

    private Hints() {}

    public static void init(IEventBus modBus) {
        modBus.addListener(Hints::onRegisterOverlays);
        MinecraftForge.EVENT_BUS.addListener(Hints::onTick);
        MinecraftForge.EVENT_BUS.addListener(Hints::onScreenRenderPost);
        MinecraftForge.EVENT_BUS.addListener(Hints::onLoggingOut);
        ENABLED.onChange(() -> {
            if (!ENABLED.get()) HintSelector.clear(STATE);
        });
    }

    /** Регистрация подсказки (любой мод, клиентская инициализация). id уникален и не переименовывается. */
    public static synchronized void register(
            String id, int priority, boolean once, BooleanSupplier when, Supplier<Component> text) {
        HINTS.removeIf(h -> h.id().equals(id));
        HINTS.add(new Hint(id, priority, once, when, text));
    }

    /** Сбросить «уже показано» (кнопка не нужна: удалить файл; метод — для тест-стенда). */
    public static synchronized void resetSeen() {
        STATE.seen.clear();
        STATE.dirty = true;
        save();
    }

    private static void onRegisterOverlays(RegisterGuiOverlaysEvent e) {
        e.registerAboveAll("pwp_hints", (gui, g, pt, w, h) -> {
            if (Minecraft.getInstance().screen == null && !Minecraft.getInstance().options.hideGui) render(g, w, h);
        });
    }

    private static void onScreenRenderPost(ScreenEvent.Render.Post e) {
        render(e.getGuiGraphics(), e.getScreen().width, e.getScreen().height);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut e) {
        HintSelector.clear(STATE);
    }

    private static synchronized void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!loaded) load();
        long now = System.currentTimeMillis();
        String id = null;
        if (ENABLED.get()) {
            List<HintSelector.Candidate> cands = new ArrayList<>(HINTS.size());
            for (Hint h : HINTS) cands.add(new HintSelector.Candidate(h.id(), h.priority(), h.once(), active(h)));
            id = HintSelector.update(cands, STATE, now);
        }
        if (STATE.dirty) save();
        if (id != null && !id.equals(shownId)) {
            shownId = id;
            fadeStart = now;
            fadingOut = false;
        } else if (id == null && shownId != null && !fadingOut) {
            fadingOut = true;
            fadeStart = now;
        }
        if (shownId != null && !fadingOut) {
            for (Hint h : HINTS) {
                if (h.id().equals(shownId)) shownText = safeText(h);
            }
        }
    }

    private static boolean active(Hint h) {
        try {
            return h.when().getAsBoolean();
        } catch (RuntimeException ex) {
            com.pwp.api.util.LogOnce.warn(LOG, "hint:" + h.id(), "Условие подсказки " + h.id() + " упало", ex);
            return false;
        }
    }

    private static Component safeText(Hint h) {
        try {
            return h.text().get();
        } catch (RuntimeException ex) {
            com.pwp.api.util.LogOnce.warn(LOG, "hint-text:" + h.id(), "Текст подсказки " + h.id() + " упал", ex);
            return Component.empty();
        }
    }

    /** Плашка справа сверху: иконка «i», до двух строк, плавное появление/исчезание. */
    private static synchronized void render(GuiGraphics g, int w, int h) {
        if (shownId == null) return;
        long t = System.currentTimeMillis() - fadeStart;
        float a = Math.min(1F, t / (float) FADE_MS);
        if (fadingOut) {
            a = 1F - a;
            if (a <= 0F) {
                shownId = null;
                return;
            }
        }
        Font font = Minecraft.getInstance().font;
        int maxW = Math.min(Spacing.HINT_MAX_W, w / 3);
        int is = Spacing.ICON_SIZE;
        List<FormattedCharSequence> lines = UIDraw.wrap(font, shownText, maxW - is - 3 * Spacing.SM, 2);
        if (lines.isEmpty()) return;
        int textW = 0;
        for (FormattedCharSequence l : lines) textW = Math.max(textW, font.width(l));
        int boxW = textW + is + 3 * Spacing.SM;
        int boxH = Math.max(is, UIDraw.linesHeight(font, lines.size())) + 2 * Spacing.SM;
        // мягкий въезд справа на несколько пикселей
        int slide = Math.round((1F - a) * Spacing.MD);
        Rect r = Rect.of(w - boxW - Spacing.MD + slide, Spacing.HINT_TOP, boxW, boxH);
        UIDraw.glass(g, r, a, false);
        UIDraw.accentBar(g, r.x(), r.y(), r.h(), Spacing.ACCENT_BAR, UIDraw.fade(PWPTheme.Colors.ACCENT, a));
        UIDraw.icon(g, UIIcon.INFO, r.x() + Spacing.SM, r.y() + Spacing.SM, is, UIDraw.fade(PWPTheme.Text.ACCENT, a));
        Rect text = Rect.of(r.x() + is + 2 * Spacing.SM, r.y() + Spacing.SM + 1, textW, boxH - 2 * Spacing.SM);
        UIDraw.lines(g, font, lines, text, false, UIDraw.fade(PWPTheme.Text.BODY, a), true);
    }

    // ───────────────────────────── файл «уже показано» ─────────────────────────────

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE);
    }

    private static void load() {
        loaded = true;
        Path f = file();
        if (!Files.isRegularFile(f)) return;
        try {
            JsonElement root = JsonParser.parseString(Files.readString(f, StandardCharsets.UTF_8));
            if (!root.isJsonObject()) return;
            JsonElement seen = root.getAsJsonObject().get("seen");
            if (seen == null || !seen.isJsonArray()) return;
            for (JsonElement el : seen.getAsJsonArray()) {
                if (el.isJsonPrimitive()) STATE.seen.add(el.getAsString());
            }
        } catch (IOException | RuntimeException ex) {
            LOG.warn("Подсказки: не прочитан {} — начинаем с нуля", f, ex);
        }
    }

    private static void save() {
        STATE.dirty = false;
        JsonObject root = new JsonObject();
        JsonArray arr = new JsonArray();
        STATE.seen.stream().sorted().forEach(arr::add);
        root.add("seen", arr);
        try {
            Files.writeString(file(), root.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            LOG.warn("Подсказки: не записан {}", file(), ex);
        }
    }
}
