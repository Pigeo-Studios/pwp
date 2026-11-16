package com.pigeostudios.pwp.warfare.client.gui;

import com.pigeostudios.pwp.warfare.client.gui.ui.WDraw;
import com.pigeostudios.pwp.warfare.client.gui.ui.WarfareIcon;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.network.PacketDownedAction;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.gui.theme.PWPTheme.Colors;
import com.pwp.coreclient.gui.theme.PWPTheme.Spacing;
import com.pwp.coreclient.ui.UIButton;
import com.pwp.coreclient.ui.UIDraw;
import com.pwp.coreclient.ui.UIIcon;
import com.pwp.coreclient.ui.UILayer;
import com.pwp.coreclient.ui.layout.Align;
import com.pwp.coreclient.ui.layout.Flow;
import com.pwp.coreclient.ui.layout.Rect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Экран нока в тактическом стиле: красная пульсирующая виньетка градиентами (мир остаётся
 * виден), внизу по центру стеклянная панель — шапка «Ранен», ближайший союзник, полоса
 * кровотечения (цвет по остатку) и кнопки «Сдаться» / «Позвать медика» (кулдаун 15 с).
 * Пакеты прежние: {@code PacketDownedAction(1)} — сдаться, {@code (0)} — вызвать медика.
 * ESC не закрывает (как раньше).
 */
public class DownedScreen extends Screen {
    private static final long MEDIC_COOLDOWN_MS = 15000;

    private final long screenOpenTime;
    private long lastMedicCallTime;
    private final UILayer layer = new UILayer();
    private UIButton callMedicButton;
    private Rect panel = Rect.of(0, 0, 0, 0);
    private Rect allyR = panel, barR = panel, bleedR = panel;
    private boolean preview;
    private int previewMaxSeconds = 60;

    public DownedScreen() {
        super(Component.translatable("gui.pwpwarfare.downed.title"));
        screenOpenTime = System.currentTimeMillis();
    }

    /** Тест-стенд UI: кнопки ничего не отправляют, таймер — {@code maxSeconds}. */
    public static DownedScreen preview(int maxSeconds) {
        DownedScreen s = new DownedScreen();
        s.preview = true;
        s.previewMaxSeconds = Math.max(1, maxSeconds);
        return s;
    }

    @Override
    protected void init() {
        layer.clear();
        int lh = font.lineHeight;
        int btnH = PWPTheme.Spacing.BUTTON_HEIGHT;
        int pw = Math.min(width - 2 * Spacing.SM, Spacing.DOWNED_PANEL_W);
        int headerH = WDraw.headerHeight(height);
        int bodyH = lh + Spacing.SM + Spacing.LOADER_BAR_H * 2 + Spacing.XS + lh + Spacing.SM + btnH;
        int ph = headerH + 2 * Spacing.SM + bodyH;
        panel = Rect.of((width - pw) / 2, height - ph - Spacing.XL, pw, ph);
        Rect body = panel.inset(Spacing.SM, headerH + Spacing.SM, Spacing.SM, Spacing.SM);
        Rect[] col = Flow.column(
                body,
                0,
                Flow.Justify.START,
                lh,
                Spacing.SM,
                Spacing.LOADER_BAR_H * 2,
                Spacing.XS,
                lh,
                Spacing.SM,
                btnH);
        allyR = col[0];
        barR = col[2];
        bleedR = col[4];
        Rect[] buttons = Flow.row(col[6], Spacing.SM, Flow.Justify.START, Flow.flex(1), Flow.flex(1));
        UIButton giveUp = new UIButton(Component.translatable("gui.pwpwarfare.downed.give_up"), () -> {
                    if (!preview) PacketHandler.INSTANCE.sendToServer(new PacketDownedAction(1));
                    onClose();
                })
                .style(UIButton.Style.DANGER)
                .icon(UIIcon.CLOSE);
        giveUp.setBounds(buttons[0]);
        callMedicButton = new UIButton(Component.translatable("gui.pwpwarfare.downed.call_medic"), () -> {
                    long now = System.currentTimeMillis();
                    if (now - lastMedicCallTime >= MEDIC_COOLDOWN_MS) {
                        if (!preview) PacketHandler.INSTANCE.sendToServer(new PacketDownedAction(0));
                        lastMedicCallTime = now;
                    }
                })
                .style(UIButton.Style.ACCENT)
                .icon(UIIcon.SUPPORT);
        callMedicButton.setBounds(buttons[1]);
        layer.add(giveUp);
        layer.add(callMedicButton);
    }

    private int maxSeconds() {
        return preview ? previewMaxSeconds : WarfareConfig.MAX_DOWNED_TIME_SECONDS.get();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        long now = System.currentTimeMillis();
        float pulse = 0.75F + 0.25F * (float) Math.sin((now - screenOpenTime) * 0.005);
        UIDraw.vignette(
                g, width, height, UIDraw.fade(Colors.withAlpha(Colors.DANGER, 0xB0), pulse), Spacing.DOWNED_VIGNETTE);
        UIDraw.vignette(
                g, width, height, PWPTheme.Styles.Backdrop.VIGNETTE, PWPTheme.Styles.Backdrop.VIGNETTE_FRACTION);

        long remaining = Math.max(0, maxSeconds() - (now - screenOpenTime) / 1000);
        Component badge = Component.literal(remaining + "s");
        WDraw.panel(g, font, panel, WDraw.headerHeight(height), WarfareIcon.SHIELD, title, badge, 1F);
        UIDraw.accentBar(g, panel.x(), panel.y(), panel.h(), Spacing.ACCENT_BAR, UIDraw.fade(Colors.DANGER, pulse));

        Component ally = allyStatus();
        WDraw.text(g, font, ally, allyR, Align.CENTER, PWPTheme.Text.BODY, false, 1F);

        float pct = Math.min(1F, remaining / (float) Math.max(1, maxSeconds()));
        int fill = pct > 0.3F ? Colors.SUCCESS_LIGHT : pct > 0.15F ? Colors.WARNING : PWPTheme.Text.DANGER;
        WDraw.progress(g, barR, pct, PWPTheme.Styles.Loader.TRACK, fill, 1F);
        WDraw.text(
                g,
                font,
                Component.translatable("gui.pwpwarfare.downed.bleeding_out", remaining),
                bleedR,
                Align.CENTER,
                PWPTheme.Text.MUTED,
                false,
                1F);

        long cd = MEDIC_COOLDOWN_MS - (now - lastMedicCallTime);
        if (cd > 0) {
            callMedicButton.label(Component.translatable("gui.pwpwarfare.downed.call_medic_cooldown", cd / 1000 + 1));
            callMedicButton.setEnabled(false);
        } else {
            callMedicButton.label(Component.translatable("gui.pwpwarfare.downed.call_medic"));
            callMedicButton.setEnabled(true);
        }
        layer.render(g, mx, my, pt, width, height);
    }

    private Component allyStatus() {
        Minecraft mc = Minecraft.getInstance();
        if (preview) return Component.translatable("gui.pwpwarfare.downed.closest_ally", 42, "MedicMain");
        if (mc.level == null || mc.player == null) return Component.translatable("gui.pwpwarfare.downed.no_allies");
        double minDist = Double.MAX_VALUE;
        Player closest = null;
        for (Player other : mc.level.players()) {
            if (other == mc.player || other.isSpectator() || !other.isAlive()) continue;
            if (mc.player.getTeam() == null || other.getTeam() != mc.player.getTeam()) continue;
            double d = mc.player.distanceTo(other);
            if (d < minDist) {
                minDist = d;
                closest = other;
            }
        }
        if (closest != null && minDist <= 250) {
            return Component.translatable(
                    "gui.pwpwarfare.downed.closest_ally", (int) minDist, closest.getScoreboardName());
        }
        return Component.translatable("gui.pwpwarfare.downed.no_allies");
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (layer.mouseClicked(mx, my, button)) return true;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        layer.mouseReleased(mx, my, button);
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
