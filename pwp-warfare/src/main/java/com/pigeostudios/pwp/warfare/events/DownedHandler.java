package com.pigeostudios.pwp.warfare.events;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketSyncDownedState;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import com.pigeostudios.pwp.warfare.stats.MatchStatsTracker;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraftforge.event.TickEvent.Phase;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.EntityInteract;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = "pwpwarfare")
// РћР±СЂР°Р±РѕС‚С‡РёРє СЃРѕСЃС‚РѕСЏРЅРёСЏ В«СЂР°РЅРµРЅВ» (knockout)
// РЈРїСЂР°РІР»СЏРµС‚ РЅРѕРєР°СѓС‚РѕРј РёРіСЂРѕРєРѕРІ, РІРѕР·РјРѕР¶РЅРѕСЃС‚СЊСЋ РѕР¶РёРІР»РµРЅРёСЏ Рё РёСЃС‚РµС‡РµРЅРёРµРј РєСЂРѕРІРё
public class DownedHandler {
   @SubscribeEvent
   public static void onPlayerHurt(LivingHurtEvent event) {
      if (event.getEntity() instanceof ServerPlayer player) {
         if ((Boolean)WarfareConfig.ENABLE_KNOCKOUT.get()) {
            if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
               if (!player.getPersistentData().getBoolean("WARFARE_GivingUp")) {
                if (player.getPersistentData().getBoolean("WARFARE_IsDowned")) {
                   if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
                      player.getPersistentData().putString("WARFARE_KnockedBy", attacker.getScoreboardName());
                      if (attacker.getTeam() != null && player.getTeam() != null && attacker.getTeam().isAlliedTo(player.getTeam())) {
                         handleTeamkill(attacker);
                      }
                   }
                   if (event.getAmount() >= player.getHealth()) {
                      event.setCanceled(true);
                      forceGiveUp(player);
                   }
                   return;
               } else {
                     if (event.getAmount() >= player.getHealth()) {
                        if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                           return;
                        }

                        long lastRevive = player.getPersistentData().getLong("WARFARE_LastReviveTime");
                        if (lastRevive > 0L && player.level().getGameTime() - lastRevive < (Integer)WarfareConfig.REVIVE_COOLDOWN_SECONDS.get() * 20) {
                           return;
                        }

                        event.setCanceled(true);
                        if (event.getSource().getEntity() instanceof Player attackingPlayer) {
                           player.getPersistentData().putString("WARFARE_KnockedBy", attackingPlayer.getScoreboardName());
                        } else if (player.getLastHurtByMob() instanceof Player lastAttacker) {
                           player.getPersistentData().putString("WARFARE_KnockedBy", lastAttacker.getScoreboardName());
                        } else {
                           player.getPersistentData().remove("WARFARE_KnockedBy");
                        }

                        enterDownedState(player);
                     }
                  }
               }
            }
         }
      }
   }

   // РџРµСЂРµРІРѕРґ РёРіСЂРѕРєР° РІ СЃРѕСЃС‚РѕСЏРЅРёРµ В«СЂР°РЅРµРЅВ»
   public static void enterDownedState(ServerPlayer player) {
      if (player.isPassenger()) {
         player.stopRiding();
      }

      player.getPersistentData().putBoolean("WARFARE_IsDowned", true);
      player.getPersistentData().putLong("WARFARE_DownedTick", player.level().getGameTime());
      player.setHealth(6.0F);
      player.setPose(Pose.SWIMMING);
      player.level()
         .playSound(null, player.getX(), player.getY(), player.getZ(), (SoundEvent)ModSounds.PLAYER_DOWNED.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
      PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncDownedState(player.getId(), true));
   }

   @SubscribeEvent
   public static void onPlayerTick(PlayerTickEvent event) {
      if (event.phase == Phase.END && !event.player.level().isClientSide) {
         ServerPlayer player = (ServerPlayer)event.player;
         if (player.getPersistentData().getBoolean("WARFARE_IsDowned")) {
            player.setPose(Pose.SWIMMING);
            player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
            long downedTick = player.getPersistentData().getLong("WARFARE_DownedTick");
            long maxDownedTicks = ((Integer)WarfareConfig.MAX_DOWNED_TIME_SECONDS.get()).intValue() * 20L;
            if (player.level().getGameTime() - downedTick >= maxDownedTicks) {
               forceGiveUp(player);
            }
         }
         enforceBaseRestriction(player);
         if (player.getPersistentData().getBoolean("WARFARE_TeamKillSpectator")) {
            enforceTeamkillSpectator(player);
         }
      }
   }

   // РџСЂРёРЅСѓРґРёС‚РµР»СЊРЅР°СЏ СЃРјРµСЂС‚СЊ РёРіСЂРѕРєР° (РёСЃС‚РµС‡РµРЅРёРµ РєСЂРѕРІРё РёР»Рё СЃРґР°С‡Р°)
   public static void forceGiveUp(ServerPlayer player) {
      player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.PLAYER_DOWNED.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
      String victimName = player.getScoreboardName();
      String killerName = player.getPersistentData().getString("WARFARE_KnockedBy");
      ServerLevel level = player.serverLevel();
      boolean showMessages = level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
      if (showMessages) {
         WarfareWorldData wd = WarfareWorldData.get(level);
         if (!wd.hideDeathMessages) {
            MutableComponent logMsg;
            if (killerName != null && !killerName.isEmpty()) {
               logMsg = Component.literal(victimName)
                  .withStyle(ChatFormatting.WHITE)
                  .append(Component.literal(" was finished by ").withStyle(ChatFormatting.GRAY))
                  .append(Component.literal(killerName).withStyle(ChatFormatting.GOLD));
            } else {
               logMsg = Component.literal(victimName)
                  .withStyle(ChatFormatting.WHITE)
                  .append(Component.literal(" has bled out").withStyle(ChatFormatting.GRAY));
            }

            player.server.getPlayerList().broadcastSystemMessage(logMsg, false);
         }
      }

      if (killerName != null && !killerName.isEmpty()) {
         ServerPlayer killer = player.server.getPlayerList().getPlayerByName(killerName);
         if (killer != null) {
            player.setLastHurtByPlayer(killer);
         }
      }

      player.getPersistentData().remove("WARFARE_KnockedBy");
      player.getPersistentData().putBoolean("WARFARE_GivingUp", true);
      player.getPersistentData().putBoolean("WARFARE_IsDowned", false);
      PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncDownedState(player.getId(), false));
      player.kill();
   }

   @SubscribeEvent
   public static void onReviveInteract(EntityInteract event) {
      if (event.getTarget() instanceof ServerPlayer target) {
         ServerPlayer var6 = (ServerPlayer)event.getEntity();
          if (target.getPersistentData().getBoolean("WARFARE_IsDowned")) {
             if (var6.getTeam() == null || target.getTeam() == null || !var6.getTeam().isAlliedTo(target.getTeam())) {
                if (!var6.level().isClientSide) {
                   var6.displayClientMessage(Component.literal("You cannot revive an ENEMY!").withStyle(ChatFormatting.RED), true);
                }

                event.setCanceled(true);
                return;
             }

            ItemStack held = var6.getItemInHand(event.getHand());
            String reviveItemName = (String)WarfareConfig.REVIVE_ITEM.get();
            Item reviveItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation(reviveItemName));
            if (held.getItem() == reviveItem) {
                revivePlayer(target);
                MatchStatsTracker.get().recordRevive(var6);
                if (!var6.isCreative()) {
                   held.shrink(1);
                }

               var6.displayClientMessage(Component.literal("Teammate revived!").withStyle(ChatFormatting.GREEN), true);
            }
         }
      }
   }

   // РћР¶РёРІР»РµРЅРёРµ РёРіСЂРѕРєР° РёР· СЃРѕСЃС‚РѕСЏРЅРёСЏ В«СЂР°РЅРµРЅВ»
   public static void handleTeamkill(ServerPlayer attacker) {
      int teamKills = attacker.getPersistentData().getInt("WARFARE_TeamKills") + 1;
      attacker.getPersistentData().putInt("WARFARE_TeamKills", teamKills);

      if (teamKills >= 8) {
         attacker.setGameMode(GameType.SPECTATOR);
         attacker.getPersistentData().putBoolean("WARFARE_TeamKillSpectator", true);
         MutableComponent broadcast = Component.translatable("pwpwarfare.message.teamkill_spectator_broadcast", attacker.getScoreboardName())
            .withStyle(ChatFormatting.RED);
         attacker.server.getPlayerList().broadcastSystemMessage(broadcast, false);
         attacker.sendSystemMessage(Component.translatable("pwpwarfare.message.teamkill_spectator_self").withStyle(ChatFormatting.RED));
         return;
      }

      if (teamKills >= 5) {
         sendTeamkillWarning(attacker);
         long restrictionUntil = attacker.level().getGameTime() + 20L * 60L * 5L;
         attacker.getPersistentData().putLong("WARFARE_BaseRestrictedUntil", restrictionUntil);
         attacker.sendSystemMessage(Component.translatable("pwpwarfare.message.teamkill_restrict").withStyle(ChatFormatting.RED));
         return;
      }

      if (teamKills >= 1) {
         sendTeamkillWarning(attacker);
      }
   }

   private static void sendTeamkillWarning(ServerPlayer target) {
      target.connection.send(new ClientboundSetTitlesAnimationPacket(10, 140, 20));
      target.connection.send(new ClientboundSetSubtitleTextPacket(Component.translatable("pwpwarfare.message.teamkill_warn_subtitle").withStyle(ChatFormatting.YELLOW)));
      target.connection
         .send(
            new ClientboundSetTitleTextPacket(Component.literal("!WARNING!").withStyle(new ChatFormatting[]{ChatFormatting.DARK_RED, ChatFormatting.BOLD}))
         );
      target.playNotifySound(net.minecraft.sounds.SoundEvents.ANVIL_LAND, SoundSource.MASTER, 1.0F, 0.8F);
   }

   private static void enforceBaseRestriction(ServerPlayer player) {
      long restrictedUntil = player.getPersistentData().getLong("WARFARE_BaseRestrictedUntil");
      if (restrictedUntil <= 0) return;

      if (player.level().getGameTime() >= restrictedUntil) {
         player.getPersistentData().remove("WARFARE_BaseRestrictedUntil");
         player.sendSystemMessage(Component.translatable("pwpwarfare.message.teamkill_restrict_expired").withStyle(ChatFormatting.GREEN));
         return;
      }

      String teamName = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
      if (teamName.isEmpty()) return;

      WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
      String currentDim = player.level().dimension().location().toString();

      boolean insideZone = false;
      for (WarfareWorldData.MainProtectionZone zone : data.mainZones) {
         if (zone.team.equalsIgnoreCase(teamName) && zone.isInside(player.position())) {
            insideZone = true;
            break;
         }
      }

      if (insideZone) return;

      BlockPos spawn = teamName.equals("BLUE") ? data.blueSpawns.get(currentDim) : data.redSpawns.get(currentDim);
      if (spawn == null) return;

      player.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
      long remaining = (restrictedUntil - player.level().getGameTime()) / 20L;
      player.sendSystemMessage(Component.translatable("pwpwarfare.message.teamkill_restrict_remaining", remaining).withStyle(ChatFormatting.RED));
      player.playNotifySound(net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.MASTER, 1.0F, 1.0F);
   }

   public static void enforceTeamkillSpectator(ServerPlayer player) {
      if (player.getPersistentData().getBoolean("WARFARE_TeamKillSpectator")) {
         if (player.gameMode.getGameModeForPlayer() != GameType.SPECTATOR) {
            player.setGameMode(GameType.SPECTATOR);
         }
      }
   }

   public static void clearTeamkillPunishment(ServerPlayer player) {
      player.getPersistentData().remove("WARFARE_TeamKills");
      player.getPersistentData().remove("WARFARE_BaseRestrictedUntil");
      boolean wasSpectator = player.getPersistentData().getBoolean("WARFARE_TeamKillSpectator");
      player.getPersistentData().remove("WARFARE_TeamKillSpectator");
      if (wasSpectator && player.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
         player.setGameMode(GameType.SURVIVAL);
      }
      player.sendSystemMessage(Component.literal("Your teamkill punishment has been cleared.").withStyle(ChatFormatting.GREEN));
   }

   public static void revivePlayer(ServerPlayer player) {
      player.getPersistentData().putBoolean("WARFARE_IsDowned", false);
      player.getPersistentData().remove("WARFARE_DownedTick");
      player.getPersistentData().remove("WARFARE_GivingUp");
      player.getPersistentData().putLong("WARFARE_LastReviveTime", player.level().getGameTime());
      player.setHealth(2.0F);
      player.setPose(Pose.STANDING);
      PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new PacketSyncDownedState(player.getId(), false));
   }
}
