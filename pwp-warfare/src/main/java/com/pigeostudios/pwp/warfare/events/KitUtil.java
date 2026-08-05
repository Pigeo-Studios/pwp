package com.pigeostudios.pwp.warfare.events;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSyncMyKit;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

// Хелпер работы с китом игрока. Выбранный в меню деплоя кит лежит в
// WARFARE_PendingKit и применяется при старте матча/спавне (WARFARE_CurrentKit).
// Все проверки (посадка в технику, выброс из сиденья, look-at подсказки) должны
// использовать effective-кит = PendingKit ?: CurrentKit — иначе в мейн-зоне до
// старта матча игрок с выбранным китом Пилота/Механика «не имеет» кита.
// syncMyKit — единственная точка синхронизации кита клиенту (PacketSyncMyKit
// раньше был зарегистрирован, но ни разу не отправлялся — ClientData.myCurrentKit
// всегда висел на "Unassigned", ломая подсказки и медик-индикаторы).
public class KitUtil {
   private KitUtil() {}

   public static String getEffectiveKit(ServerPlayer player) {
      String pending = player.getPersistentData().getString("WARFARE_PendingKit");
      if (pending != null && !pending.isEmpty()) return pending;
      return player.getPersistentData().getString("WARFARE_CurrentKit");
   }

   public static void syncMyKit(ServerPlayer player) {
      if (player == null) return;
      PacketHandler.INSTANCE.send(
         PacketDistributor.PLAYER.with(() -> player),
         new PacketSyncMyKit(getEffectiveKit(player))
      );
   }
}
