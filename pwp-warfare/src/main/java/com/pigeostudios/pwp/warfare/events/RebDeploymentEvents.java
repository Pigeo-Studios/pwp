package com.pigeostudios.pwp.warfare.events;

import java.util.Set;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.registries.ForgeRegistries;

// Гейтинг РЭБ (uncomplicated-fpv reb / reb_mini):
// - Прямое использование предмета (мгновенный спавн) ЗАБЛОКИРОВАНО —
//   РЭБ строится только через рацию (блюпринт + лопата).
// - Снифт+ПКМ: демонтаж своей командой БЕЗ возврата предмета (штатный
//   UFPV-возврат блокируется). Чужой команде — запрещено.
// - Обычный ПКМ (вкл/выкл глушилки): только своей команде.
public class RebDeploymentEvents {
   private static final Set<String> REB_ITEMS = Set.of("uncomplicatedfpv:reb", "uncomplicatedfpv:reb_mini");
   private static final Set<String> REB_ENTITIES = Set.of("uncomplicatedfpv:reb", "uncomplicatedfpv:reb_mini");

   private RebDeploymentEvents() {
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
      if (event.getHand() != InteractionHand.MAIN_HAND) return;
      Player player = event.getEntity();
      if (player == null) return;
      ItemStack stack = event.getItemStack();
      if (stack.isEmpty()) return;
      ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
      if (itemId == null || !REB_ITEMS.contains(itemId.toString())) return;
      event.setCanceled(true);
      event.setCancellationResult(InteractionResult.SUCCESS);
      if (!event.getLevel().isClientSide && player instanceof ServerPlayer sp) {
         sp.displayClientMessage(net.minecraft.network.chat.Component.literal("РЭБ ставится только через стройку (рация)!").withStyle(net.minecraft.ChatFormatting.RED), true);
      }
   }

   @SubscribeEvent(priority = EventPriority.HIGHEST)
   public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
      if (event.getHand() != InteractionHand.MAIN_HAND) return;
      Player player = event.getEntity();
      if (player == null) return;
      Entity target = event.getTarget();
      if (target == null) return;
      ResourceLocation eid = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
      if (eid == null || !REB_ENTITIES.contains(eid.toString())) return;

      String rebTeam = target.getPersistentData().getString("WARFARE_VehicleTeam");
      String playerTeam = player.getTeam() != null ? player.getTeam().getName() : "NEUTRAL";
      boolean ownTeam = player.isCreative()
         || rebTeam.isEmpty() || rebTeam.equals("NEUTRAL") || rebTeam.equalsIgnoreCase(playerTeam);

      // Снифт+ПКМ: демонтаж (своя команда) / запрет (чужая). Штатный возврат предмета не сработает.
      if (player.isShiftKeyDown()) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
         if (!event.getLevel().isClientSide) {
            ServerLevel level = (ServerLevel) event.getLevel();
            if (ownTeam) {
               level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, target.getX(), target.getY() + 0.5, target.getZ(), 15, 0.4, 0.3, 0.4, 0.02);
               level.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ANVIL_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
               target.remove(Entity.RemovalReason.DISCARDED);
               if (player instanceof ServerPlayer sp) {
                  sp.displayClientMessage(net.minecraft.network.chat.Component.literal("РЭБ демонтирован").withStyle(net.minecraft.ChatFormatting.GREEN), true);
               }
            } else if (player instanceof ServerPlayer sp) {
               sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Доступ запрещён: РЭБ врага!").withStyle(net.minecraft.ChatFormatting.RED), true);
            }
         }
         return;
      }

      // Обычный ПКМ (вкл/выкл): только своя команда; свой — пропускаем в UFPV (toggle).
      if (!ownTeam) {
         event.setCanceled(true);
         event.setCancellationResult(InteractionResult.SUCCESS);
         if (!event.getLevel().isClientSide && player instanceof ServerPlayer sp) {
            sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Доступ запрещён: РЭБ врага!").withStyle(net.minecraft.ChatFormatting.RED), true);
         }
      }
   }
}
