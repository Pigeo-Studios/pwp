/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.chat.MutableComponent
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.tags.DamageTypeTags
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Pose
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.GameRules
 *  net.minecraftforge.event.TickEvent$Phase
 *  net.minecraftforge.event.TickEvent$PlayerTickEvent
 *  net.minecraftforge.event.entity.living.LivingHurtEvent
 *  net.minecraftforge.event.entity.player.PlayerInteractEvent$EntityInteract
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.network.PacketDistributor
 *  net.minecraftforge.registries.ForgeRegistries
 */
package com.example.aas.events;

import com.example.aas.config.AASConfig;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketSyncDownedState;
import com.example.aas.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid="aas")
public class DownedHandler {
    @SubscribeEvent
    public static void onPlayerHurt(LivingHurtEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer player = (ServerPlayer)livingEntity;
        if (!((Boolean)AASConfig.ENABLE_KNOCKOUT.get()).booleanValue()) {
            return;
        }
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }
        if (player.getPersistentData().getBoolean("AAS_GivingUp")) {
            return;
        }
        if (player.getPersistentData().getBoolean("AAS_IsDowned")) {
            if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                return;
            }
            event.setCanceled(true);
            return;
        }
        if (event.getAmount() >= player.getHealth()) {
            if (event.getSource().is(DamageTypeTags.IS_EXPLOSION)) {
                return;
            }
            long lastRevive = player.getPersistentData().getLong("AAS_LastReviveTime");
            if (lastRevive > 0L && player.level().getGameTime() - lastRevive < (long)((Integer)AASConfig.REVIVE_COOLDOWN_SECONDS.get() * 20)) {
                return;
            }
            event.setCanceled(true);
            Entity attacker = event.getSource().getEntity();
            if (attacker instanceof Player) {
                Player attackingPlayer = (Player)attacker;
                player.getPersistentData().putString("AAS_KnockedBy", attackingPlayer.getScoreboardName());
            } else {
                LivingEntity livingEntity2 = player.getLastHurtByMob();
                if (livingEntity2 instanceof Player) {
                    Player lastAttacker = (Player)livingEntity2;
                    player.getPersistentData().putString("AAS_KnockedBy", lastAttacker.getScoreboardName());
                } else {
                    player.getPersistentData().remove("AAS_KnockedBy");
                }
            }
            DownedHandler.enterDownedState(player);
        }
    }

    public static void enterDownedState(ServerPlayer player) {
        if (player.isPassenger()) {
            player.stopRiding();
        }
        player.getPersistentData().putBoolean("AAS_IsDowned", true);
        player.getPersistentData().putLong("AAS_DownedTick", player.level().getGameTime());
        player.setHealth(6.0f);
        player.setPose(Pose.SWIMMING);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), (SoundEvent)ModSounds.PLAYER_DOWNED.get(), SoundSource.PLAYERS, 1.5f, 1.0f);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncDownedState(player.getId(), true));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }
        if (event.player.getPersistentData().getBoolean("AAS_IsDowned")) {
            ServerPlayer player = (ServerPlayer)event.player;
            player.setPose(Pose.SWIMMING);
            player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
            long downedTick = player.getPersistentData().getLong("AAS_DownedTick");
            long maxDownedTicks = (long)((Integer)AASConfig.MAX_DOWNED_TIME_SECONDS.get()).intValue() * 20L;
            if (player.level().getGameTime() - downedTick >= 3600L) {
                DownedHandler.forceGiveUp(player);
            }
        }
    }

    public static void forceGiveUp(ServerPlayer player) {
        ServerPlayer killer;
        String victimName = player.getScoreboardName();
        String killerName = player.getPersistentData().getString("AAS_KnockedBy");
        ServerLevel level = player.serverLevel();
        boolean showMessages = level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
        if (showMessages) {
            MutableComponent logMsg = killerName != null && !killerName.isEmpty() ? Component.literal((String)victimName).withStyle(ChatFormatting.WHITE).append((Component)Component.literal((String)" was finished by ").withStyle(ChatFormatting.GRAY)).append((Component)Component.literal((String)killerName).withStyle(ChatFormatting.GOLD)) : Component.literal((String)victimName).withStyle(ChatFormatting.WHITE).append((Component)Component.literal((String)" has bled out").withStyle(ChatFormatting.GRAY));
            player.server.getPlayerList().broadcastSystemMessage((Component)logMsg, false);
        }
        if (killerName != null && !killerName.isEmpty() && (killer = player.server.getPlayerList().getPlayerByName(killerName)) != null) {
            player.setLastHurtByPlayer((Player)killer);
        }
        player.getPersistentData().remove("AAS_KnockedBy");
        player.getPersistentData().putBoolean("AAS_GivingUp", true);
        player.getPersistentData().putBoolean("AAS_IsDowned", false);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncDownedState(player.getId(), false, true));
        player.kill();
    }

    @SubscribeEvent
    public static void onReviveInteract(PlayerInteractEvent.EntityInteract event) {
        Entity entity = event.getTarget();
        if (!(entity instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer target = (ServerPlayer)entity;
        ServerPlayer reviver = (ServerPlayer)event.getEntity();
        if (target.getPersistentData().getBoolean("AAS_IsDowned")) {
            if (reviver.getTeam() == null || target.getTeam() == null || !reviver.getTeam().isAlliedTo(target.getTeam())) {
                if (!reviver.level().isClientSide) {
                    reviver.displayClientMessage((Component)Component.literal((String)"You cannot revive an ENEMY!").withStyle(ChatFormatting.RED), true);
                }
                event.setCanceled(true);
                return;
            }
            ItemStack held = reviver.getItemInHand(event.getHand());
            String reviveItemName = (String)AASConfig.REVIVE_ITEM.get();
            Item reviveItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation(reviveItemName));
            if (held.getItem() == reviveItem) {
                DownedHandler.revivePlayer(target);
                if (!reviver.isCreative()) {
                    held.shrink(1);
                }
                reviver.displayClientMessage((Component)Component.literal((String)"Teammate revived!").withStyle(ChatFormatting.GREEN), true);
            }
        }
    }

    public static void revivePlayer(ServerPlayer player) {
        player.getPersistentData().putBoolean("AAS_IsDowned", false);
        player.getPersistentData().remove("AAS_DownedTick");
        player.getPersistentData().remove("AAS_GivingUp");
        player.getPersistentData().putLong("AAS_LastReviveTime", player.level().getGameTime());
        player.setHealth(2.0f);
        player.setPose(Pose.STANDING);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncDownedState(player.getId(), false));
    }
}

