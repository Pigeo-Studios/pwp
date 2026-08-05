package com.pigeostudios.pwp.warfare.client;

import com.pwp.coreclient.gui.theme.PWPTheme;
import com.tacz.guns.api.event.common.GunShootEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;

// Лента уведомлений: 2 слота над фидбек-блоком (нижний h-76, верхний h-94).
// Новое уведомление появляется в верхнем слоте (плавно), предыдущий верхний
// опускается вниз, нижний плавно гаснет. Дедик: то же сообщение уже висит —
// просто продлеваем таймер (зажатый ЛКМ в мейн-зоне не спамит ленту).
// Анимации: плавный фейд (0.2/кадр) и скольжение между слотами (0.18/кадр) —
// ничего не появляется резко. Отрисовка: низ-центр, шрифт PWPTheme, полоска
// слева DANGER (блок) / ACCENT (инфо). Экшенбар/тайтлы/чаты не трогаем.
@EventBusSubscriber(modid = "pwpwarfare", value = Dist.CLIENT, bus = Bus.FORGE)
public class NotificationFeed {
   public static final byte SEVERITY_INFO = 0;
   public static final byte SEVERITY_DANGER = 1;

   private static final int MAX_ENTRIES = 2;
   private static final int ENTRY_HEIGHT = 16;
   private static final int ENTRY_GAP = 2;
   private static final long LIFE_TICKS = 70L; // 3.5 секунды
   private static final int ANCHOR_Y = 76; // нижний слот: height - ANCHOR_Y (выше фидбек-блока на h-58)

   private static final List<Entry> ENTRIES = new ArrayList<>();

   private static final class Entry {
      String key;
      Component text;
      byte severity;
      long expireTick;
      int slot; // 0 = нижний, 1 = верхний
      boolean fading;
      float y;
      float alpha;
   }

   private NotificationFeed() {}

   // Показать уведомление в ленте (клиентская часть; сервер шлёт PacketNotification)
   public static void push(String textKey, byte severity) {
      Minecraft mc = Minecraft.getInstance();
      long now = mc.level != null ? mc.level.getGameTime() : 0L;

      // Дедик: то же сообщение уже висит или ещё гаснет — оживляем его и
      // продлеваем таймер, чтобы один и тот же текст никогда не дублировался
      for (Entry entry : ENTRIES) {
         if (entry.key.equals(textKey)) {
            entry.fading = false;
            entry.expireTick = now + LIFE_TICKS;
            entry.severity = severity;
            return;
         }
      }

      // Новое: встаёт в верхний слот, бывший верхний опускается вниз,
      // бывший нижний плавно гаснет
      for (Entry entry : ENTRIES) {
         entry.slot--;
         if (entry.slot < 0) entry.fading = true;
      }

      Entry fresh = new Entry();
      fresh.key = textKey;
      fresh.text = Component.translatable(textKey);
      fresh.severity = severity;
      fresh.expireTick = now + LIFE_TICKS;
      fresh.slot = 1;
      fresh.y = slotY(1);
      fresh.alpha = 0.0F;
      ENTRIES.add(fresh);

      while (ENTRIES.size() > MAX_ENTRIES + 1) {
         ENTRIES.remove(0);
      }
   }

   // Есть ли видимые записи (для гибрид-гейта SBW-оверлеев: боевой HUD SBW
   // скрывается, пока лента показывает уведомления)
   public static boolean hasVisibleEntries() {
      for (Entry entry : ENTRIES) {
         if (!entry.fading) return true;
      }
      return false;
   }

   private static int slotY(int slot) {
      Minecraft mc = Minecraft.getInstance();
      int height = mc.getWindow().getGuiScaledHeight();
      return height - ANCHOR_Y - slot * (ENTRY_HEIGHT + ENTRY_GAP);
   }

   // Клиентская отмена выстрела TACZ: только для своего игрока (чужие анимации не трогаем)
   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onGunShootClient(GunShootEvent event) {
      Minecraft mc = Minecraft.getInstance();
      if (!ClientSafetyState.inMainZone) return;
      if (event.getShooter() != mc.player) return;
      event.setCanceled(true);
      push("safety.mainzone.blocked", SEVERITY_DANGER);
   }

   @SubscribeEvent
   public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
      if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;
      Minecraft mc = Minecraft.getInstance();
      if (mc.level == null) return;
      long now = mc.level.getGameTime();

      for (Entry entry : ENTRIES) {
         if (!entry.fading && now >= entry.expireTick) entry.fading = true;
      }

      GuiGraphics gui = event.getGuiGraphics();
      int width = mc.getWindow().getGuiScaledWidth();
      Font font = PWPTheme.Fonts.display();
      ListIterator<Entry> it = ENTRIES.listIterator();
      while (it.hasNext()) {
         Entry entry = it.next();
         float targetAlpha = entry.fading ? 0.0F : 1.0F;
         entry.alpha += (targetAlpha - entry.alpha) * 0.2F;
         float targetY = slotY(entry.slot);
         entry.y += (targetY - entry.y) * 0.18F;
         if (entry.fading && entry.alpha < 0.01F) {
            it.remove();
            continue;
         }
         if (entry.alpha <= 0.01F) continue;
         drawEntry(gui, font, entry, width);
      }
   }

   private static void drawEntry(GuiGraphics gui, Font font, Entry entry, int width) {
      int alpha = (int)(Math.min(1.0F, entry.alpha) * 220.0F);
      if (alpha <= 2) return;
      int textWidth = font.width(entry.text);
      int padding = PWPTheme.Spacing.SM;
      int boxWidth = textWidth + padding * 2 + PWPTheme.Spacing.XS;
      int x = (width - boxWidth) / 2;
      int y = (int)entry.y;
      int accent = entry.severity == SEVERITY_DANGER ? PWPTheme.Colors.DANGER : PWPTheme.Colors.ACCENT;
      gui.fill(x, y, x + boxWidth, y + ENTRY_HEIGHT, PWPTheme.Colors.withAlpha(PWPTheme.Colors.BACKGROUND, alpha));
      gui.fill(x, y, x + 3, y + ENTRY_HEIGHT, PWPTheme.Colors.withAlpha(accent, alpha));
      gui.fill(x, y, x + boxWidth, y + 1, PWPTheme.Colors.withAlpha(PWPTheme.Colors.BORDER_LIGHT, alpha));
      int textY = y + (ENTRY_HEIGHT - font.lineHeight) / 2;
      gui.drawString(font, entry.text, x + padding + PWPTheme.Spacing.XS, textY, PWPTheme.Colors.withAlpha(PWPTheme.Colors.TEXT_PRIMARY, alpha), false);
   }
}
