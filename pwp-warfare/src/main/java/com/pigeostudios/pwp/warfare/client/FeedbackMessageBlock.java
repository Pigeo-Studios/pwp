package com.pigeostudios.pwp.warfare.client;

import com.pigeostudios.pwp.warfare.client.gui.ui.WHud;
import com.pwp.coreclient.gui.theme.PWPTheme;
import com.pwp.coreclient.ui.UIDamp;
import com.pwp.coreclient.ui.hud.HudDraw;
import com.pwp.coreclient.ui.hud.HudZones;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

// Фидбек-блок: стильная замена ванильного экшенбара (ярус h-58, центр — над
// хотбаром, под лентой уведомлений h-76/h-94). Все мгновенные сообщения
// («Нет магазина!», «Запрос отклонён», «Чужая команда!» и ~50 других)
// перехватываются миксином GuiOverlayMessageMixin в единой точке — никакой
// миграции вызовов. Одна запись, живёт 3 секунды (как ваниль), плавный фейд.
// Стиль — как у ленты уведомлений (PWPTheme), цвета сообщений берутся из
// самих компонентов (withStyle RED и т.д.).
@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.FORGE)
public class FeedbackMessageBlock {
    private static final long SHOW_TICKS = 60L; // 3 секунды, как ванильный экшенбар
    private static Component current = Component.empty();
    private static long showUntilTick = 0L;
    private static boolean fading = false;
    private static float alpha = 0.0F;
    private static final UIDamp.Frame FRAME = new UIDamp.Frame();

    private FeedbackMessageBlock() {}

    public static void show(Component message) {
        Minecraft mc = Minecraft.getInstance();
        long now = mc.level != null ? mc.level.getGameTime() : 0L;
        current = message;
        // настройка «Длительность сообщений» (по умолчанию SHOW_TICKS = 3 с, как ваниль)
        showUntilTick = now + WarfareUiSettings.ticks(WarfareUiSettings.FEEDBACK_SECONDS);
        fading = false;
        if (alpha < 0.3F) alpha = 0.3F; // уже видимое сообщение не «прыгает» при замене
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        if (!fading && now >= showUntilTick) fading = true;

        float target = fading ? 0.0F : 1.0F;
        alpha = UIDamp.approach(alpha, target, 0.2F, FRAME.dtMs()); // 0.2/кадр при 60 FPS
        if (alpha <= 0.01F) return;
        if (!WHud.hudVisible()) return; // общий гейт HUD (F1)

        GuiGraphics gui = event.getGuiGraphics();
        HudZones z = WHud.zones();
        Font font = PWPTheme.Fonts.display();
        int textWidth = font.width(current);
        int padding = PWPTheme.Spacing.SM;
        int boxWidth = textWidth + padding * 2 + PWPTheme.Spacing.XS;
        int boxHeight = PWPTheme.Spacing.LG;
        // ярус фидбека HudZones.feedback() (над хотбаром, под лентой); масштаб HUD — HudDraw.begin
        int x = -boxWidth / 2;
        int y = -boxHeight;
        int a = (int) (Math.min(1.0F, alpha) * 220.0F * WarfareUiSettings.MESSAGE_OPACITY.get());
        if (a <= 0) return;
        HudDraw.begin(gui, z.feedback().centerX(), z.feedback().bottom(), z.scale());
        draw(gui, font, x, y, boxWidth, boxHeight, padding, a);
        HudDraw.end(gui);
    }

    private static void draw(
            GuiGraphics gui, Font font, int x, int y, int boxWidth, int boxHeight, int padding, int a) {

        gui.fill(x, y, x + boxWidth, y + boxHeight, PWPTheme.Colors.withAlpha(PWPTheme.Colors.BACKGROUND, a));
        gui.fill(x, y, x + 3, y + boxHeight, PWPTheme.Colors.withAlpha(PWPTheme.Colors.ACCENT, a));
        gui.fill(x, y, x + boxWidth, y + 1, PWPTheme.Colors.withAlpha(PWPTheme.Colors.BORDER_LIGHT, a));
        // текст с альфой <4 ваниль рисует НЕПРОЗРАЧНЫМ — на хвосте фейда текст не рисуем
        if (a < 4) return;
        int textY = y + (boxHeight - font.lineHeight) / 2;
        gui.drawString(
                font,
                current,
                x + padding + PWPTheme.Spacing.XS,
                textY,
                PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_PRIMARY, a),
                false);
    }
}
