package com.pigeostudios.pwp.warfare.events;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.network.PacketDroneDeployState;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

// Система деплоя дронов uncomplicated-fpv (FPV/Mavic).
//
// Штатный спавн UFPV (мгновенный useOn предмета) блокируется на сервере —
// вместо него канальный деплой: ПКМ по земле дроном в руке -> проверки ->
// дрон сразу спавнится в точке (лежит, неуязвим, флаг WARFARE_DroneDeploying),
// установка занимает DRONE_DEPLOY_TIME_TICKS -> готов: полный боезапас,
// энергия, теги команды/владельца.
//
// Анти-абьюз:
// - Разбор дрона (снифт+ПКМ пустой рукой, возврат предмета) запрещён.
// - Кулдаун 45с ОТ СТАРТА установки (потеря дрона его не продлевает).
// - Лимит активных: DRONE_MAX_ACTIVE_PER_PLAYER всего, DRONE_MAX_ACTIVE_PER_TYPE
//   на тип (1 FPV + 1 мавик одновременно).
// - Отмена (сдвиг/смерть/смена предмета/выход) = потеря предмета.
// - Брошенный дрон (на земле без управления) — авто-деспаун.
// - Чанк-страховка: управляемый дрон в чанке, который не тикается ->
//   принудительный обрыв связи (handleSignalLoss) + дисконнект монитора.
@EventBusSubscriber(modid = "pwpwarfare", bus = Bus.FORGE)
public class DroneDeploymentEvents {
   private static final String[] DRONE_ENTITY_IDS = {
      "uncomplicatedfpv:fpv_drone",
      "uncomplicatedfpv:mavic_drone_no_drop",
      "uncomplicatedfpv:mavic_drone_with_drop"
   };
   private static final Set<String> DRONE_ITEMS = Set.of(
      "uncomplicatedfpv:fpv_drone",
      "uncomplicatedfpv:mavic_drone_no_drop",
      "uncomplicatedfpv:mavic_drone_with_drop"
   );

   // persistentData игрока
   private static final String P_DEPLOY_TYPE = "WARFARE_DroneDeployType";
   private static final String P_DEPLOY_START = "WARFARE_DroneDeployStart";
   private static final String P_DEPLOY_X = "WARFARE_DroneDeployX";
   private static final String P_DEPLOY_Y = "WARFARE_DroneDeployY";
   private static final String P_DEPLOY_Z = "WARFARE_DroneDeployZ";
   private static final String P_DEPLOY_ENTITY = "WARFARE_DroneDeployEntity";
   private static final String P_CD_UNTIL = "WARFARE_DroneDeployCd";
   // Канал перезарядки мавика дрон-подсумком
   private static final String P_RELOAD_TYPE = "WARFARE_DroneReloadType";
   private static final String P_RELOAD_START = "WARFARE_DroneReloadStart";
   private static final String P_RELOAD_X = "WARFARE_DroneReloadX";
   private static final String P_RELOAD_Y = "WARFARE_DroneReloadY";
   private static final String P_RELOAD_Z = "WARFARE_DroneReloadZ";
   private static final String P_RELOAD_ENTITY = "WARFARE_DroneReloadEntity";

   // persistentData дрона
   private static final String E_DEPLOYING = "WARFARE_DroneDeploying";
   private static final String E_OWNER = "WARFARE_DroneOwner";
   private static final String E_IDLE_TICKS = "WARFARE_DroneIdleTicks";
   private static final String E_TEAM = "WARFARE_VehicleTeam";
   private static final String E_TYPE = "WARFARE_VehicleType";
   private static final String E_FALLBACK_TICKS = "WARFARE_DroneFallbackTicks";

   private static final Set<String> DRONE_KITS = Set.of("Drone Operator", "Scout");

   private DroneDeploymentEvents() {
   }

   // ===== Блокировка штатного спавна UFPV + старт деплоя =====

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
      if (event.getHand() != InteractionHand.MAIN_HAND) return;
      Player player = event.getEntity();
      if (player == null) return;
      ItemStack stack = event.getItemStack();
      if (stack.isEmpty()) return;
      ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
      if (itemId == null || !DRONE_ITEMS.contains(itemId.toString())) return;

      // Штатный useOn UFPV не должен сработать ни на одной стороне.
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.SUCCESS);
      if (!(player.level() instanceof ServerLevel serverLevel)) return;
      if (player instanceof FakePlayer || !(player instanceof ServerPlayer serverPlayer)) return;

      if (event.getPos() == null || event.getFace() == null) return;
      startDeploy(serverPlayer, serverLevel, stack, event.getPos().relative(event.getFace()), itemId.toString());
   }

   private static void startDeploy(ServerPlayer player, ServerLevel level, ItemStack stack, BlockPos placePos, String droneId) {
      if (!isDroneEntity(droneId)) return;

      // Игра должна идти.
      WarfareWorldData data = WarfareWorldData.get(level);
      if (!data.isGameStarted && !player.isCreative()) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Игра ещё не началась!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }

      // Мейн-зона: установка дрона (снаряжается ракетой/гранатами) приравнена
      // к стрельбе — тот же гейт MainZoneFireGuard, что и у всего оружия.
      if (MainZoneFireGuard.isFireBlocked(player)) {
         MainZoneFireGuard.notifyBlocked(player);
         return;
      }

      // Кит: только оператор дрона / разведчик.
      if (!player.isCreative()) {
         String kit = KitUtil.getEffectiveKit(player);
         if (!DRONE_KITS.contains(kit)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Дроны доступны только оператору дрона/разведчику!").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
         }
      }

      // Уже деплоим — проверка РАНЬШЕ кулдауна (спам ПКМ во время установки
      // не должен показывать «Кулдаун» вместо «Установка уже идёт»).
      if (!player.getPersistentData().getString(P_DEPLOY_TYPE).isEmpty()) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Установка уже идёт!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }

      // Кулдаун от старта установки. Время — МИРОВОЕ (level.getGameTime(), тики
      // сохраняются в level.dat): серверный tickCount сбрасывается при рестарте
      // сервера/матча, а cdUntil в persistentData игрока переживает рестарт —
      // после него tickCount мал, cdUntil велик, и кулдаун «висел» бесконечно.
      // Каждый новый деплой ПЕРЕЗАПИСЫВАЕТ cdUntil абсолютным значением
      // (gameTime + CD) — обновление, а не прибавление к старому значению.
      // ВАЖНО: это НЕ тот же gameTime, что в P_DEPLOY_START — прогресс деплоя
      // считает elapsed от серверного tickCount (сессионно, чистится при логине).
      long now = level.getGameTime();
      long cdUntil = player.getPersistentData().getLong(P_CD_UNTIL);
      if (!player.isCreative() && now < cdUntil) {
         long sec = (cdUntil - now) / 20L;
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Кулдаун деплоя: " + sec + "с").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }
      // Прогресс установки — ОТДЕЛЬНЫЙ источник: серверный tickCount (сессия).
      // НЕ level.getGameTime() — иначе elapsed = tick - start вечно отрицательный
      // и установка зависает на полном прогрессе (инцидент 06.08.2026).
      long deployStartTick = level.getServer().getTickCount();

      // Лимиты активных дронов.
      int activeTotal = 0;
      int activeType = 0;
      String ownerUuid = player.getStringUUID();
      String deployType = droneId;
      for (Entity e : level.getEntities().getAll()) {
         if (!e.getPersistentData().contains(E_OWNER)) continue;
         if (!ownerUuid.equals(e.getPersistentData().getString(E_OWNER))) continue;
         activeTotal++;
         ResourceLocation eid = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
         if (eid != null && eid.toString().equals(deployType)) activeType++;
      }
      if (activeTotal >= WarfareConfig.DRONE_MAX_ACTIVE_PER_PLAYER.get()) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Слишком много активных дронов!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }
      if (activeType >= WarfareConfig.DRONE_MAX_ACTIVE_PER_TYPE.get()) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Дрон этого типа уже активен!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }

      // Дальний клик (край досягаемости / случайно кликнул далеко) не должен
      // ставить дрон за порогом «стоять на месте» (2.5 блока XZ) и мгновенно
      // отменять установку, сжигая кулдаун. Точка установки затягивается к
      // игроку на DRONE_DEPLOY_MAX_RANGE (ниже порога отмены — канал всегда
      // стартует нормально). Y при клампе — уровень ног игрока, чтобы фолбэк
      // высоты не оказался на высоте далёкой стены.
      int maxRange = WarfareConfig.DRONE_DEPLOY_MAX_RANGE.get();
      double pdx = placePos.getX() + 0.5 - player.getX();
      double pdz = placePos.getZ() + 0.5 - player.getZ();
      double pdist2 = pdx * pdx + pdz * pdz;
      if (pdist2 > maxRange * maxRange) {
         double pscale = maxRange / Math.sqrt(pdist2);
         placePos = new BlockPos(
            (int) Math.floor(player.getX() + pdx * pscale),
            player.blockPosition().getY(),
            (int) Math.floor(player.getZ() + pdz * pscale));
      }

      // Спавн дрона в точке установки (на земле).
      EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation(droneId));
      if (type == null) return;
      Entity drone = type.create(level);
      if (drone == null) return;

      double x = placePos.getX() + 0.5;
      double z = placePos.getZ() + 0.5;
      int height = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos((int) x, placePos.getY(), (int) z)).getY();
      double y = height + 1.0;
      if (y < placePos.getY()) y = placePos.getY() + 0.1;
      drone.setPos(x, y, z);
      drone.setYRot(player.getYRot() + 180.0F);

      drone.getPersistentData().putBoolean(E_DEPLOYING, true);
      drone.getPersistentData().putString(E_OWNER, ownerUuid);
      drone.getPersistentData().putInt(E_IDLE_TICKS, 0);
      drone.setInvulnerable(true);
      level.addFreshEntity(drone);

      // Предмет тратится сразу.
      if (!player.isCreative()) {
         ItemStack main = player.getMainHandItem();
         if (!main.isEmpty()) main.shrink(1);
      }

      // Состояние деплоя.
      player.getPersistentData().putString(P_DEPLOY_TYPE, droneId);
      player.getPersistentData().putLong(P_DEPLOY_START, deployStartTick);
      player.getPersistentData().putDouble(P_DEPLOY_X, x);
      player.getPersistentData().putDouble(P_DEPLOY_Y, y);
      player.getPersistentData().putDouble(P_DEPLOY_Z, z);
      player.getPersistentData().putInt(P_DEPLOY_ENTITY, drone.getId());

      // Кулдаун СЧИТАЕТСЯ ОТ СТАРТА установки (потеря дрона его не продлевает).
      if (!player.isCreative()) {
         player.getPersistentData().putLong(P_CD_UNTIL, now + WarfareConfig.DRONE_DEPLOY_CD_TICKS.get());
      }

      level.playSound(null, x, y, z, sound("pwpwarfare:drone_deploy_start"), SoundSource.PLAYERS, 1.0F, 1.0F);
      sendDeployState(player, true, 0.0F, x, y, z, droneId, 0);
      player.displayClientMessage(net.minecraft.network.chat.Component.literal("Установка дрона...").withStyle(net.minecraft.ChatFormatting.YELLOW), true);
   }

   // ===== Серверный тикер: прогресс деплоя, отмены, завершение =====

   private static int deploySyncCounter = 0;

   @SubscribeEvent
   public static void onServerTick(TickEvent.ServerTickEvent event) {
      if (event.phase != TickEvent.Phase.END) return;
      int tick = event.getServer().getTickCount();

      boolean syncNow = ++deploySyncCounter >= 5;
      if (syncNow) deploySyncCounter = 0;

      for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
         tickPlayerDeploy(player, tick, syncNow);
         tickPlayerReload(player, tick, syncNow);
      }

      if (tick % 20L == 0L) {
         for (ServerLevel level : event.getServer().getAllLevels()) {
            tickIdleDespawn(level, tick);
            tickChunkFallback(level, tick);
         }
      }
   }

   private static void tickPlayerDeploy(ServerPlayer player, int tick, boolean syncNow) {
      var data = player.getPersistentData();
      String deployType = data.getString(P_DEPLOY_TYPE);
      if (deployType.isEmpty()) return;

      int droneEntityId = data.getInt(P_DEPLOY_ENTITY);
      Entity drone = player.serverLevel().getEntity(droneEntityId);
      double x = data.getDouble(P_DEPLOY_X);
      double y = data.getDouble(P_DEPLOY_Y);
      double z = data.getDouble(P_DEPLOY_Z);

      // Отмена: смерть игрока или уход с места установки (юзер: «должен стоять
      // на месте»). Движение/смена предмета деплой НЕ прерывают в остальном —
      // предмет уже израсходован при старте. Порог 2.5 блока по XZ (прыжок
      // с места не считается — Y не проверяется).
      boolean dead = !player.isAlive();
      double dx = player.getX() - x;
      double dz = player.getZ() - z;
      boolean moved = dx * dx + dz * dz > 6.25;
      // Вход в мейн-зону во время канала тоже отменяет установку (флаг
      // пересчитывается раз в 0.5с — без этого дрон «добилдился» бы в зоне,
      // если канал стартовал впритык к границе).
      boolean inMainZone = MainZoneFireGuard.isFireBlocked(player);

      if (dead || moved || inMainZone) {
         cancelDeploy(player, drone, tick, moved, inMainZone);
         return;
      }

      long start = data.getLong(P_DEPLOY_START);
      int total = WarfareConfig.DRONE_DEPLOY_TIME_TICKS.get();
      long elapsed = tick - start;
      float progress = total <= 0 ? 1.0F : Math.min(1.0F, (float) elapsed / total);

      // Звук-гул установки (каждые 20 тиков). Партиклы во время канала НЕ
      // спавним вообще — ELECTRIC_SPARK каждые 4 тика у точки, на которую
      // смотрит игрок, заметно ронял ФПС (юзер: «партиклы очень лагают,
      // смотришь и фпс в 9»). Одноразовые партиклы остались на старте/завершении.
      if (elapsed % 20L == 0L) {
         player.level().playSound(null, x, y, z, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
      }

      if (syncNow) {
         sendDeployState(player, true, progress, x, y, z, deployType, 0);
      }

      if (elapsed >= total) {
         completeDeploy(player, drone, deployType, tick);
      }
   }

   private static void cancelDeploy(ServerPlayer player, Entity drone, int tick, boolean movedAway, boolean inMainZone) {
      var data = player.getPersistentData();
      String droneId = data.getString(P_DEPLOY_TYPE);
      data.remove(P_DEPLOY_TYPE);
      data.remove(P_DEPLOY_START);
      data.remove(P_DEPLOY_X);
      data.remove(P_DEPLOY_Y);
      data.remove(P_DEPLOY_Z);
      data.remove(P_DEPLOY_ENTITY);
      if (drone != null && !drone.isRemoved()) {
         drone.remove(Entity.RemovalReason.DISCARDED);
      }
      // Установка не удалась — возвращаем предмет дрона (в инвентарь, если
      // живы, иначе дроп рядом). Кулдаун при этом остаётся (от старта) —
      // анти-спам по-прежнему работает.
      returnDroneItem(player, droneId);
      sendDeployState(player, false, 0.0F, 0, 0, 0, "", 0);
      player.displayClientMessage(net.minecraft.network.chat.Component.literal(
         inMainZone ? "Установка прервана — вход в мейн-зону!" : movedAway ? "Установка прервана — вы сдвинулись с места!" : "Установка прервана!"
      ).withStyle(net.minecraft.ChatFormatting.RED), true);
   }

   private static void returnDroneItem(ServerPlayer player, String droneId) {
      if (droneId == null || droneId.isEmpty() || player.isCreative()) return;
      Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(droneId));
      if (item == null) return;
      ItemStack stack = new ItemStack(item, 1);
      if (player.isAlive()) {
         if (!player.addItem(stack)) {
            player.drop(stack, false);
         }
      } else {
         player.drop(stack, false);
      }
   }

   private static void completeDeploy(ServerPlayer player, Entity drone, String deployType, int tick) {
      var data = player.getPersistentData();
      data.remove(P_DEPLOY_TYPE);
      data.remove(P_DEPLOY_START);
      data.remove(P_DEPLOY_X);
      data.remove(P_DEPLOY_Y);
      data.remove(P_DEPLOY_Z);
      data.remove(P_DEPLOY_ENTITY);

      if (drone == null || drone.isRemoved()) {
         sendDeployState(player, false, 0.0F, 0, 0, 0, "", 0);
         return;
      }

      drone.getPersistentData().remove("WARFARE_DroneDeploying");
      drone.setInvulnerable(false);
      // Тег команды обязателен (AGENTS.md): без него дрон «без команды» и
      // будущие проверки фракций не сработают.
      String team = player.getTeam() != null ? player.getTeam().getName() : "NONE";
      drone.getPersistentData().putString(E_TEAM, team);
      applySpawnLoadout(drone, deployType);
      drone.setYRot(player.getYRot() + 180.0F);

      ServerLevel level = (ServerLevel) player.level();
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, drone.getX(), drone.getY() + 0.5, drone.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
      level.sendParticles(ParticleTypes.ELECTRIC_SPARK, drone.getX(), drone.getY() + 0.5, drone.getZ(), 8, 0.5, 0.3, 0.5, 0.1);
      level.playSound(null, drone.getX(), drone.getY(), drone.getZ(), sound("pwpwarfare:drone_ready"), SoundSource.PLAYERS, 1.0F, 1.0F);

      sendDeployState(player, false, 1.0F, 0, 0, 0, "", 0);
      player.displayClientMessage(net.minecraft.network.chat.Component.literal("Дрон готов! Подключите монитор.").withStyle(net.minecraft.ChatFormatting.GREEN), true);
   }

   // ===== Боезапас и энергия при спавне =====

   private static void applySpawnLoadout(Entity drone, String droneType) {
      try {
         switch (droneType) {
            case "uncomplicatedfpv:fpv_drone" -> {
               quickLoad(drone, "superbwarfare:rpg_rocket_standard", 1);
            }
            case "uncomplicatedfpv:mavic_drone_with_drop" -> {
               // UFPV quickLoadAttachment кладёт ОДНУ штуку за вызов (штатный
               // interact fake-player'а) — два вызова = 2 гранаты.
               quickLoad(drone, "superbwarfare:rgo_grenade", 1);
               quickLoad(drone, "superbwarfare:rgo_grenade", 1);
            }
            default -> {
            }
         }
         com.pigeostudios.pwp.warfare.util.DroneCompat.setEnergyFull(drone);
      } catch (Throwable ignored) {
         // UFPV/SBW не загружены — без лоаута, но дрон жив.
      }
   }

   private static void quickLoad(Entity drone, String itemId, int count) {
      Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
      if (item == null) return;
      com.pigeostudios.pwp.warfare.util.DroneCompat.quickLoadAttachment(drone, new ItemStack(item, count));
   }

   // ===== Разбор запрещён / гейт установки / перезарядка мавика =====

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
      if (event.getHand() != InteractionHand.MAIN_HAND) return;
      Player player = event.getEntity();
      if (player == null) return;
      Entity target = event.getTarget();
      if (target == null) return;
      ResourceLocation eid = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
      if (eid == null || !DRONE_ITEMS.contains(eid.toString())) return;

      // Снифт + пустая рука = штатный разбор UFPV с возвратом предмета — запрещён.
      if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
         if (!event.getLevel().isClientSide && player instanceof ServerPlayer sp) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Дрон не разбирается!").withStyle(net.minecraft.ChatFormatting.RED), true);
         }
         return;
      }

      // Дрон ещё устанавливается: блокируем ВСЕ взаимодействия (монитор не
      // подключится, инвентарь не откроется, разбор не пройдёт) — юзер:
      // «монитор не подключать, пока дрон не забилдится».
      if (target.getPersistentData().getBoolean(E_DEPLOYING)) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
         if (!event.getLevel().isClientSide && player instanceof ServerPlayer sp) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Дрон устанавливается!").withStyle(net.minecraft.ChatFormatting.RED), true);
         }
         return;
      }

      // Перезарядка мавика дрон-подсумком (только mavic_drone_with_drop).
      ItemStack stack = player.getMainHandItem();
      if (!stack.isEmpty() && stack.is(ModItems.DRONE_AMMO_POUCH.get())) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
         if (!event.getLevel().isClientSide && player instanceof ServerPlayer sp
            && event.getLevel() instanceof ServerLevel sl) {
            startReload(sp, sl, target);
         }
      }
   }

   // ===== Перезарядка мавика (дрон-подсумок) =====

   private static void startReload(ServerPlayer player, ServerLevel level, Entity drone) {
      ResourceLocation eid = ForgeRegistries.ENTITY_TYPES.getKey(drone.getType());
      if (eid == null || !"uncomplicatedfpv:mavic_drone_with_drop".equals(eid.toString())) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Перезарядить можно только мавик с боезапасом!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }
      String owner = drone.getPersistentData().getString(E_OWNER);
      if (owner == null || owner.isEmpty() || !player.getStringUUID().equals(owner)) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Это не ваш дрон!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }
      if (!drone.onGround()) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Мавик должен приземлиться!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }
      if (isDroneControlled(drone)) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Мавик под управлением!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }
      if (com.pigeostudios.pwp.warfare.util.DroneCompat.isFullyLoaded(drone)) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Мавик уже заряжен!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }
      if (drone.getPersistentData().getBoolean(E_DEPLOYING)) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Мавик ещё устанавливается!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }
      var data = player.getPersistentData();
      if (!data.getString(P_DEPLOY_TYPE).isEmpty()) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Сначала завершите установку дрона!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }
      if (!data.getString(P_RELOAD_TYPE).isEmpty()) {
         player.displayClientMessage(net.minecraft.network.chat.Component.literal("Перезарядка уже идёт!").withStyle(net.minecraft.ChatFormatting.RED), true);
         return;
      }
      // Мейн-зона: перезарядка снаряжает мавик гранатами — тот же гейт, что у деплоя.
      if (MainZoneFireGuard.isFireBlocked(player)) {
         MainZoneFireGuard.notifyBlocked(player);
         return;
      }
      if (!player.isCreative()) {
         ItemStack stack = player.getMainHandItem();
         if (stack.isEmpty() || !stack.is(ModItems.DRONE_AMMO_POUCH.get())) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Нужен дрон-подсумок!").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
         }
      }

      long now = level.getServer().getTickCount();
      data.putString(P_RELOAD_TYPE, eid.toString());
      data.putLong(P_RELOAD_START, now);
      data.putDouble(P_RELOAD_X, drone.getX());
      data.putDouble(P_RELOAD_Y, drone.getY());
      data.putDouble(P_RELOAD_Z, drone.getZ());
      data.putInt(P_RELOAD_ENTITY, drone.getId());
      // Подсумок тратится сразу (канал запущен — анти-спам, как у деплоя).
      if (!player.isCreative()) {
         player.getMainHandItem().shrink(1);
      }

      level.playSound(null, drone.getX(), drone.getY(), drone.getZ(), sound("pwpwarfare:drone_deploy_start"), SoundSource.PLAYERS, 1.0F, 1.0F);
      sendDeployState(player, true, 0.0F, drone.getX(), drone.getY(), drone.getZ(), eid.toString(), 1);
      player.displayClientMessage(net.minecraft.network.chat.Component.literal("Перезарядка мавика...").withStyle(net.minecraft.ChatFormatting.YELLOW), true);
   }

   private static void tickPlayerReload(ServerPlayer player, int tick, boolean syncNow) {
      var data = player.getPersistentData();
      String reloadType = data.getString(P_RELOAD_TYPE);
      if (reloadType.isEmpty()) return;

      int entityId = data.getInt(P_RELOAD_ENTITY);
      Entity drone = player.serverLevel().getEntity(entityId);
      double x = data.getDouble(P_RELOAD_X);
      double y = data.getDouble(P_RELOAD_Y);
      double z = data.getDouble(P_RELOAD_Z);

      // Отмена: смерть игрока, дрон пропал/взлетел/под управлением/в установке,
      // или игрок вошёл в мейн-зону (канал снаряжает дрон гранатами).
      boolean cancelled = !player.isAlive()
         || drone == null || drone.isRemoved()
         || isDroneControlled(drone)
         || !drone.onGround()
         || drone.getPersistentData().getBoolean(E_DEPLOYING)
         || MainZoneFireGuard.isFireBlocked(player);
      if (cancelled) {
         cancelReload(player);
         return;
      }

      long start = data.getLong(P_RELOAD_START);
      int total = WarfareConfig.DRONE_RELOAD_TIME_TICKS.get();
      long elapsed = tick - start;
      float progress = total <= 0 ? 1.0F : Math.min(1.0F, (float) elapsed / total);

      // Звук-гул перезарядки. Партиклы не спавним (см. tickPlayerDeploy — лагают).
      if (elapsed % 20L == 0L) {
         player.level().playSound(null, x, y, z, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.PLAYERS, 0.5F, 1.0F);
      }

      if (syncNow) {
         sendDeployState(player, true, progress, x, y, z, reloadType, 1);
      }

      if (elapsed >= total) {
         completeReload(player, drone);
      }
   }

   private static void cancelReload(ServerPlayer player) {
      var data = player.getPersistentData();
      data.remove(P_RELOAD_TYPE);
      data.remove(P_RELOAD_START);
      data.remove(P_RELOAD_X);
      data.remove(P_RELOAD_Y);
      data.remove(P_RELOAD_Z);
      data.remove(P_RELOAD_ENTITY);
      sendDeployState(player, false, 0.0F, 0, 0, 0, "", 1);
      player.displayClientMessage(net.minecraft.network.chat.Component.literal("Перезарядка прервана!").withStyle(net.minecraft.ChatFormatting.RED), true);
   }

   private static void completeReload(ServerPlayer player, Entity drone) {
      var data = player.getPersistentData();
      data.remove(P_RELOAD_TYPE);
      data.remove(P_RELOAD_START);
      data.remove(P_RELOAD_X);
      data.remove(P_RELOAD_Y);
      data.remove(P_RELOAD_Z);
      data.remove(P_RELOAD_ENTITY);

      if (drone == null || drone.isRemoved() || !drone.onGround() || isDroneControlled(drone)) {
         sendDeployState(player, false, 0.0F, 0, 0, 0, "", 1);
         return;
      }

      try {
         Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare:rgo_grenade"));
         if (item != null) {
            // Две гранаты = два вызова (quickLoadAttachment кладёт 1 шт/вызов).
            com.pigeostudios.pwp.warfare.util.DroneCompat.quickLoadAttachment(drone, new ItemStack(item, 1));
            com.pigeostudios.pwp.warfare.util.DroneCompat.quickLoadAttachment(drone, new ItemStack(item, 1));
         }
      } catch (Throwable ignored) {
      }

      ServerLevel level = (ServerLevel) player.level();
      level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, drone.getX(), drone.getY() + 0.5, drone.getZ(), 6, 0.4, 0.3, 0.4, 0.02);
      level.playSound(null, drone.getX(), drone.getY(), drone.getZ(), sound("pwpwarfare:drone_ready"), SoundSource.PLAYERS, 1.0F, 1.0F);
      sendDeployState(player, false, 1.0F, 0, 0, 0, "", 1);
      player.displayClientMessage(net.minecraft.network.chat.Component.literal("Мавик перезаряжен!").withStyle(net.minecraft.ChatFormatting.GREEN), true);
   }

   // ===== Кулдаун при потере дрона НЕ ставится — CD идёт от старта установки.
   // Здесь только чистим состояние, если что-то пошло не так.

   // ===== Авто-деспаун брошенных дронов + чанк-страховка =====

   private static void tickIdleDespawn(ServerLevel level, int tick) {
      int threshold = WarfareConfig.DRONE_IDLE_DESPAWN_TICKS.get();
      for (Entity e : level.getEntities().getAll()) {
         ResourceLocation eid = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
         if (eid == null || !DRONE_ITEMS.contains(eid.toString())) continue;

         // Дрон-зомби: установка не завершилась (игрок вышел/умер при деплое),
         // а флаг E_DEPLOYING вечно защищал дрон от деспауна и делал его
         // неуязвимым. Если владелец не деплоит ИМЕННО этого дрона — сносим.
         if (e.getPersistentData().getBoolean(E_DEPLOYING)) {
            ServerPlayer owner = findOwner(level, e);
            boolean activeDeploy = owner != null
               && owner.getPersistentData().getInt(P_DEPLOY_ENTITY) == e.getId();
            if (activeDeploy) continue;
            e.remove(Entity.RemovalReason.DISCARDED);
            if (owner != null) {
               owner.displayClientMessage(net.minecraft.network.chat.Component.literal("Установка дрона прервана — дрон демонтирован.").withStyle(net.minecraft.ChatFormatting.RED), true);
            }
            continue;
         }

         boolean controlled = isDroneControlled(e);
         if (!e.onGround() || controlled) {
            e.getPersistentData().putInt(E_IDLE_TICKS, 0);
            continue;
         }

         int idle = e.getPersistentData().getInt(E_IDLE_TICKS) + 20;
         e.getPersistentData().putInt(E_IDLE_TICKS, idle);
         if (idle >= threshold) {
            e.remove(Entity.RemovalReason.DISCARDED);
            ServerPlayer owner = findOwner(level, e);
            if (owner != null) {
               owner.displayClientMessage(net.minecraft.network.chat.Component.literal("Дрон деактивирован (простаивает).").withStyle(net.minecraft.ChatFormatting.RED), true);
               sendDeployState(owner, false, 0.0F, 0, 0, 0, "", 0);
            }
         }
      }
   }

   private static void tickChunkFallback(ServerLevel level, int tick) {
      for (Entity e : level.getEntities().getAll()) {
         ResourceLocation eid = ForgeRegistries.ENTITY_TYPES.getKey(e.getType());
         if (eid == null || !DRONE_ITEMS.contains(eid.toString())) continue;
         if (e.getPersistentData().getBoolean(E_DEPLOYING)) continue;
         if (!isDroneControlled(e)) continue;

         // Чанк дрона не тикается (между view и sim, или трекинг чанков выключен).
         // НЕ удаляем мгновенно (grace): в микро-окнах после взлёта/подключения
         // монитора трекер чанков (sbwchunkload) ещё не успел поставить тикеты —
         // ложное удаление управляемого дрона. Сносим только после 40 тиков
         // непрерывного не-тикания (трекер гарантированно подхватил бы за 2с;
         // не подхватил — честный обрыв связи).
         boolean ticking = level.isPositionEntityTicking(e.blockPosition());
         int fb = e.getPersistentData().getInt(E_FALLBACK_TICKS);
         if (ticking) {
            if (fb != 0) {
               e.getPersistentData().putInt(E_FALLBACK_TICKS, 0);
            }
            continue;
         }
         fb += 20;
         e.getPersistentData().putInt(E_FALLBACK_TICKS, fb);
         if (fb < 40) continue;

         com.pigeostudios.pwp.warfare.util.DroneCompat.forceSignalLoss(e);
         e.remove(Entity.RemovalReason.DISCARDED);
         ServerPlayer owner = findOwner(level, e);
         if (owner != null) {
            owner.displayClientMessage(net.minecraft.network.chat.Component.literal("Связь с дроном потеряна!").withStyle(net.minecraft.ChatFormatting.RED), true);
            sendDeployState(owner, false, 0.0F, 0, 0, 0, "", 0);
         }
      }
   }

   // ===== Хелперы =====

   private static boolean isDroneEntity(String id) {
      for (String d : DRONE_ENTITY_IDS) {
         if (d.equals(id)) return true;
      }
      return false;
   }

   private static boolean isDroneControlled(Entity drone) {
      try {
         return com.pigeostudios.pwp.warfare.util.DroneCompat.isControlled(drone);
      } catch (Throwable t) {
         return false;
      }
   }

   private static ServerPlayer findOwner(ServerLevel level, Entity drone) {
      String owner = drone.getPersistentData().getString(E_OWNER);
      if (owner == null || owner.isEmpty()) return null;
      try {
         return level.getServer().getPlayerList().getPlayer(UUID.fromString(owner));
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   private static void sendDeployState(ServerPlayer player, boolean active, float progress, double x, double y, double z, String typeId, int mode) {
      PacketHandler.INSTANCE.send(
         PacketDistributor.PLAYER.with(() -> player),
         new PacketDroneDeployState(active, progress, (int) x, (int) y, (int) z, typeId, WarfareConfig.DRONE_DEPLOY_TIME_TICKS.get(), mode)
      );
   }

   // ===== Чистка залипшего состояния при входе =====

   @SubscribeEvent
   public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
      if (!(event.getEntity() instanceof ServerPlayer sp)) return;
      var data = sp.getPersistentData();
      boolean deployActive = !data.getString(P_DEPLOY_TYPE).isEmpty();
      boolean reloadActive = !data.getString(P_RELOAD_TYPE).isEmpty();
      if (!deployActive && !reloadActive) return;

      // Дрон уже не наш: мир пережил выход игрока, сущность могла исчезнуть —
      // но если жива (зомби-деплой), сносим, чтобы не остался вечный дрон.
      if (deployActive) {
         int droneId = data.getInt(P_DEPLOY_ENTITY);
         Entity drone = sp.serverLevel().getEntity(droneId);
         if (drone != null && !drone.isRemoved()) {
            drone.remove(Entity.RemovalReason.DISCARDED);
         }
         data.remove(P_DEPLOY_TYPE);
         data.remove(P_DEPLOY_START);
         data.remove(P_DEPLOY_X);
         data.remove(P_DEPLOY_Y);
         data.remove(P_DEPLOY_Z);
         data.remove(P_DEPLOY_ENTITY);
      }
      if (reloadActive) {
         data.remove(P_RELOAD_TYPE);
         data.remove(P_RELOAD_START);
         data.remove(P_RELOAD_X);
         data.remove(P_RELOAD_Y);
         data.remove(P_RELOAD_Z);
         data.remove(P_RELOAD_ENTITY);
      }
      sendDeployState(sp, false, 0.0F, 0, 0, 0, "", 0);
   }

   private static SoundEvent sound(String id) {
      return SoundEvent.createVariableRangeEvent(new ResourceLocation(id));
   }
}
