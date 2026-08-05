package com.pigeostudios.pwp.warfare.events;

import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketMainZoneState;
import com.pigeostudios.pwp.warfare.network.PacketNotification;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.vicmatskiv.pointblank.entity.ProjectileLike;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.PacketDistributor;

// Предохранитель мейн-зоны: пока игрок внутри мейн-зоны (WarfareWorldData.mainZones),
// стрельба всем оружием заблокирована. Работает серверно-авторитетно:
//   - TACZ: отменяем cancellable GunShootEvent (постится сервером ДО расхода патрона);
//   - SBW (техника, ручное, миномёт): отменяется миксином SbwFireMixin на
//     GunItem.shoot(ShootParameters) — единой точке всего SBW-огня;
//   - страховочная сеть: EntityJoinLevelEvent отменяет спавн любых летящих снарядов
//     SBW/pointblank, если их владелец в мейн-зоне (работает, даже если миксин
//     молча не применился — например, после обновления SBW).
// При блокировке игроку отправляется PacketNotification (анти-спам раз в 3 секунды),
// клиент показывает плашку в ленте уведомлений «В мейн-зоне стрельба запрещена».
@EventBusSubscriber(modid = "pwpwarfare", bus = Bus.FORGE)
public class MainZoneFireGuard {
   private static final String ZONE_STATE_TAG = "WARFARE_InMainZone";
   private static final String BLOCK_MSG_TAG = "WARFARE_BlockMsgTick";
   private static final long BLOCK_MSG_INTERVAL = 60L; // раз в 3 секунды

   private MainZoneFireGuard() {}

   // Расчёт состояния «в мейн-зоне» по координатам. Вызывается ТОЛЬКО тикером
   // (раз в полсекунды) и при логине — результат пишется в persistentData флаг.
   // В горячих путях (каждый выстрел) зоны не пересчитываются — там читается флаг.
   private static boolean isInMainZone(Entity entity) {
      if (entity == null || entity.level() == null || entity.level().isClientSide) return false;
      if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
      if (!(entity.level() instanceof ServerLevel level)) return false;
      WarfareWorldData data = WarfareWorldData.get(level);
      if (data == null || data.mainZones.isEmpty()) return false;
      List<WarfareWorldData.MainProtectionZone> zones = data.mainZones;
      for (WarfareWorldData.MainProtectionZone zone : zones) {
         if (zone.isInside(entity.position())) return true;
      }
      return false;
   }

   // Можно ли стрелять: false — если серверный игрок в мейн-зоне.
   // Читает готовый флаг из persistentData (никаких обходов зон на выстрел).
   // На клиенте всегда true (ServerPlayer там не бывает), поэтому миксины,
   // зарегистрированные на обеих сторонах, на клиенте безопасно no-op.
   public static boolean isFireBlocked(Entity shooter) {
      return shooter instanceof ServerPlayer sp && sp.getPersistentData().getBoolean(ZONE_STATE_TAG);
   }

   // Отправляет клиенту сигнал «выстрел заблокирован» не чаще раза в 3 секунды
   public static void notifyBlocked(ServerPlayer player) {
      if (player == null || player.level() == null) return;
      long now = player.level().getGameTime();
      long last = player.getPersistentData().getLong(BLOCK_MSG_TAG);
      if (now - last >= BLOCK_MSG_INTERVAL) {
         player.getPersistentData().putLong(BLOCK_MSG_TAG, now);
         PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketNotification("safety.mainzone.blocked", (byte)1));
      }
   }

   // TACZ: все пушки (включая пассажирские места) — отменяем выстрел до расхода патрона
   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onGunShoot(GunShootEvent event) {
      if (event.getLogicalSide() != LogicalSide.SERVER) return;
      if (isFireBlocked(event.getShooter())) {
         event.setCanceled(true);
         if (event.getShooter() instanceof ServerPlayer sp) notifyBlocked(sp);
      }
   }

   // Страховочная сеть: летящие снаряды SBW/pointblank из мейн-зоны не спавнятся
   @SubscribeEvent(priority = EventPriority.HIGH)
   public static void onEntityJoin(EntityJoinLevelEvent event) {
      if (event.getLevel() == null || event.getLevel().isClientSide()) return;
      Entity entity = event.getEntity();
      if (entity == null || !isFlyingRound(entity)) return;
      Entity owner = entity instanceof net.minecraft.world.entity.projectile.Projectile p ? p.getOwner() : null;
      if (isFireBlocked(owner)) {
         event.setCanceled(true);
         if (owner instanceof ServerPlayer sp) notifyBlocked(sp);
      }
   }

   // Летящий боеприпас: все снаряды SBW и аддонов (FCP/VVP/DragonRise наследуют
   // базовые классы) + все снаряды pointblank (FCL-трубы). Не летящие предметы
   // (C4/Claymore/мины) не входят — это установка, а не стрельба.
   private static boolean isFlyingRound(Entity entity) {
      if (entity instanceof ProjectileLike) return true;
      if (entity instanceof FastThrowableProjectile) return true;
      if (entity instanceof ProjectileEntity) return true;
      return entity instanceof TaserBulletEntity;
   }

   // Синхронизация клиенту состояния «в мейн-зоне» (вход/выход) раз в полсекунды
   @SubscribeEvent
   public static void onServerTick(TickEvent.ServerTickEvent event) {
      if (event.phase != TickEvent.Phase.END) return;
      MinecraftServer server = event.getServer();
      if (server == null || server.getTickCount() % 10 != 0) return;
      for (ServerPlayer player : server.getPlayerList().getPlayers()) {
         boolean in = isInMainZone(player);
         boolean prev = player.getPersistentData().getBoolean(ZONE_STATE_TAG);
         if (in != prev) {
            player.getPersistentData().putBoolean(ZONE_STATE_TAG, in);
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketMainZoneState(in));
            // Разовое уведомление при входе в зону (лента уведомлений)
            if (in) {
               PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketNotification("safety.mainzone.enter", (byte)0));
            }
         }
      }
   }

   // При входе на сервер сразу отправляем актуальное состояние зоны
   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         boolean in = isInMainZone(player);
         player.getPersistentData().putBoolean(ZONE_STATE_TAG, in);
         PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new PacketMainZoneState(in));
      }
   }
}
