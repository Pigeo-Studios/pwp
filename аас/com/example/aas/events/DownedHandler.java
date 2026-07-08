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
        if (event.getSource().m_269533_(DamageTypeTags.f_268738_)) {
            return;
        }
        if (player.getPersistentData().m_128471_("AAS_GivingUp")) {
            return;
        }
        if (player.getPersistentData().m_128471_("AAS_IsDowned")) {
            if (event.getSource().m_269533_(DamageTypeTags.f_268415_)) {
                return;
            }
            event.setCanceled(true);
            return;
        }
        if (event.getAmount() >= player.m_21223_()) {
            if (event.getSource().m_269533_(DamageTypeTags.f_268415_)) {
                return;
            }
            long lastRevive = player.getPersistentData().m_128454_("AAS_LastReviveTime");
            if (lastRevive > 0L && player.m_9236_().m_46467_() - lastRevive < (long)((Integer)AASConfig.REVIVE_COOLDOWN_SECONDS.get() * 20)) {
                return;
            }
            event.setCanceled(true);
            Entity attacker = event.getSource().m_7639_();
            if (attacker instanceof Player) {
                Player attackingPlayer = (Player)attacker;
                player.getPersistentData().m_128359_("AAS_KnockedBy", attackingPlayer.m_6302_());
            } else {
                LivingEntity livingEntity2 = player.m_21188_();
                if (livingEntity2 instanceof Player) {
                    Player lastAttacker = (Player)livingEntity2;
                    player.getPersistentData().m_128359_("AAS_KnockedBy", lastAttacker.m_6302_());
                } else {
                    player.getPersistentData().m_128473_("AAS_KnockedBy");
                }
            }
            DownedHandler.enterDownedState(player);
        }
    }

    public static void enterDownedState(ServerPlayer player) {
        if (player.m_20159_()) {
            player.m_8127_();
        }
        player.getPersistentData().m_128379_("AAS_IsDowned", true);
        player.getPersistentData().m_128356_("AAS_DownedTick", player.m_9236_().m_46467_());
        player.m_21153_(6.0f);
        player.m_20124_(Pose.SWIMMING);
        player.m_9236_().m_6263_(null, player.m_20185_(), player.m_20186_(), player.m_20189_(), (SoundEvent)ModSounds.PLAYER_DOWNED.get(), SoundSource.PLAYERS, 1.5f, 1.0f);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncDownedState(player.m_19879_(), true));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.m_9236_().f_46443_) {
            return;
        }
        if (event.player.getPersistentData().m_128471_("AAS_IsDowned")) {
            ServerPlayer player = (ServerPlayer)event.player;
            player.m_20124_(Pose.SWIMMING);
            player.m_20334_(0.0, player.m_20184_().f_82480_, 0.0);
            long downedTick = player.getPersistentData().m_128454_("AAS_DownedTick");
            long maxDownedTicks = (long)((Integer)AASConfig.MAX_DOWNED_TIME_SECONDS.get()).intValue() * 20L;
            if (player.m_9236_().m_46467_() - downedTick >= 3600L) {
                DownedHandler.forceGiveUp(player);
            }
        }
    }

    public static void forceGiveUp(ServerPlayer player) {
        ServerPlayer killer;
        String victimName = player.m_6302_();
        String killerName = player.getPersistentData().m_128461_("AAS_KnockedBy");
        ServerLevel level = player.m_284548_();
        boolean showMessages = level.m_46469_().m_46207_(GameRules.f_46142_);
        if (showMessages) {
            MutableComponent logMsg = killerName != null && !killerName.isEmpty() ? Component.m_237113_((String)victimName).m_130940_(ChatFormatting.WHITE).m_7220_((Component)Component.m_237113_((String)" was finished by ").m_130940_(ChatFormatting.GRAY)).m_7220_((Component)Component.m_237113_((String)killerName).m_130940_(ChatFormatting.GOLD)) : Component.m_237113_((String)victimName).m_130940_(ChatFormatting.WHITE).m_7220_((Component)Component.m_237113_((String)" has bled out").m_130940_(ChatFormatting.GRAY));
            player.f_8924_.m_6846_().m_240416_((Component)logMsg, false);
        }
        if (killerName != null && !killerName.isEmpty() && (killer = player.f_8924_.m_6846_().m_11255_(killerName)) != null) {
            player.m_6598_((Player)killer);
        }
        player.getPersistentData().m_128473_("AAS_KnockedBy");
        player.getPersistentData().m_128379_("AAS_GivingUp", true);
        player.getPersistentData().m_128379_("AAS_IsDowned", false);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncDownedState(player.m_19879_(), false, true));
        player.m_6074_();
    }

    @SubscribeEvent
    public static void onReviveInteract(PlayerInteractEvent.EntityInteract event) {
        Entity entity = event.getTarget();
        if (!(entity instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer target = (ServerPlayer)entity;
        ServerPlayer reviver = (ServerPlayer)event.getEntity();
        if (target.getPersistentData().m_128471_("AAS_IsDowned")) {
            if (reviver.m_5647_() == null || target.m_5647_() == null || !reviver.m_5647_().m_83536_(target.m_5647_())) {
                if (!reviver.m_9236_().f_46443_) {
                    reviver.m_5661_((Component)Component.m_237113_((String)"You cannot revive an ENEMY!").m_130940_(ChatFormatting.RED), true);
                }
                event.setCanceled(true);
                return;
            }
            ItemStack held = reviver.m_21120_(event.getHand());
            String reviveItemName = (String)AASConfig.REVIVE_ITEM.get();
            Item reviveItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation(reviveItemName));
            if (held.m_41720_() == reviveItem) {
                DownedHandler.revivePlayer(target);
                if (!reviver.m_7500_()) {
                    held.m_41774_(1);
                }
                reviver.m_5661_((Component)Component.m_237113_((String)"Teammate revived!").m_130940_(ChatFormatting.GREEN), true);
            }
        }
    }

    public static void revivePlayer(ServerPlayer player) {
        player.getPersistentData().m_128379_("AAS_IsDowned", false);
        player.getPersistentData().m_128473_("AAS_DownedTick");
        player.getPersistentData().m_128473_("AAS_GivingUp");
        player.getPersistentData().m_128356_("AAS_LastReviveTime", player.m_9236_().m_46467_());
        player.m_21153_(2.0f);
        player.m_20124_(Pose.STANDING);
        PacketHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), (Object)new PacketSyncDownedState(player.m_19879_(), false));
    }
}

