package com.pigeostudios.pwp.warfare.client;

import com.pwp.coreclient.gui.theme.PWPTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
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
   private static final long FADE_MS = 400L; // таймерный фейд-аут: не зависит от FPS и не мерцает на хвосте
   private static Component current = Component.empty();
   private static long showUntilTick = 0L;
   private static boolean fading = false;
   private static float alpha = 0.0F;
   private static long fadeStartMs = 0L;
   private static float fadeFromAlpha = 0.0F;

   private FeedbackMessageBlock() {}

   public static void show(Component message) {
      Minecraft mc = Minecraft.getInstance();
      long now = mc.level != null ? mc.level.getGameTime() : 0L;
      // Дедик по тексту (как в ленте уведомлений): пока сообщение ВИСИТ, повторное
      // то же сообщение только продлевает время показа — фейд не рестартует, текст
      // не «моргает». Уже погасшее/гаснущее — показываем заново (свежее событие).
      // Разное сообщение — заменяем, сохраняя текущую альфу (плавно дорастёт до 1).
      if (!fading && current.getString().equals(message.getString())) {
         showUntilTick = now + SHOW_TICKS;
         return;
      }
      current = message;
      showUntilTick = now + SHOW_TICKS;
      fading = false;
      // Замена сообщения без прыжка альфы: гаснущее сообщение плавно дорастёт
      // до 1 с текущей альфы (раньше «поджиг» до 0.3 давал вспышку — мерцание)
   }

   @SubscribeEvent
   public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
      if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) return;
      long now = mc.level.getGameTime();
      if (!fading && now >= showUntilTick) {
         fading = true;
         fadeStartMs = System.currentTimeMillis();
         fadeFromAlpha = alpha;
      }

      if (fading) {
         // Таймерный фейд-аут (ease-out кубический) от зафиксированной альфы.
         // Порог 0.03 (13.08.2026): тёмная плашка на тёмном фоне невидима уже при
         // a≈10, а светлый текст при той же альфе ещё «висит» и в конце резко
         // пропадает — «фон пропал, текст моргнул». Обрубаем до этого хвоста.
         float t = Mth.clamp((System.currentTimeMillis() - fadeStartMs) / (float) FADE_MS, 0.0F, 1.0F);
         float k = 1.0F - (float) Math.pow(1.0F - t, 3.0);
         alpha = fadeFromAlpha * (1.0F - k);
      } else {
         alpha += (1.0F - alpha) * 0.2F;
      }
      if (alpha <= 0.03F) return;

      GuiGraphics gui = event.getGuiGraphics();
      int width = mc.getWindow().getGuiScaledWidth();
      int height = mc.getWindow().getGuiScaledHeight();
      Font font = PWPTheme.Fonts.display();
      int textWidth = font.width(current);
      int padding = PWPTheme.Spacing.SM;
      int boxWidth = textWidth + padding * 2 + PWPTheme.Spacing.XS;
      int boxHeight = PWPTheme.Spacing.LG;
      int x = (width - boxWidth) / 2;
      int y = height - 58; // ярус фидбека: над хотбаром (его верх h-42), ниже ленты (h-76/h-94)
      int a = (int) Math.round(Math.min(1.0F, alpha) * 220.0F);

      gui.fill(x, y, x + boxWidth, y + boxHeight, PWPTheme.Colors.withAlpha(PWPTheme.Colors.BACKGROUND, a));
      gui.fill(x, y, x + 3, y + boxHeight, PWPTheme.Colors.withAlpha(PWPTheme.Colors.ACCENT, a));
      gui.fill(x, y, x + boxWidth, y + 1, PWPTheme.Colors.withAlpha(PWPTheme.Colors.BORDER_LIGHT, a));
      int textY = y + (boxHeight - font.lineHeight) / 2;
      gui.drawString(font, current, x + padding + PWPTheme.Spacing.XS, textY, PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_PRIMARY, a), false);
   }
}
