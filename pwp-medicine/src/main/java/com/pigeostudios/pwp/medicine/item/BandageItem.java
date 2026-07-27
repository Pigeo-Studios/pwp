package com.pigeostudios.pwp.medicine.item;

import java.util.function.Consumer;
import com.pigeostudios.pwp.medicine.effect.ModEffects;
import com.pigeostudios.pwp.medicine.event.EventHandler;
import com.pigeostudios.pwp.medicine.item.client.BandageRenderer;
import com.pigeostudios.pwp.medicine.sound.ModSounds;
import com.pigeostudios.pwp.medicine.util.ClientUtils;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class BandageItem
extends Item
implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);

    public BandageItem(Item.Properties pProperties) {
        super(pProperties);
    }

    public int getUseDuration(ItemStack pStack) {
        if (pStack.getOrCreateTag().getBoolean("Reviving")) {
            return 160;
        }
        return 60;
    }

    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW;
    }

    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        Player target = this.getTargetPlayer(pPlayer);
        Player toHeal = target != null ? target : pPlayer;
        if (target != null && target.getPersistentData().getBoolean("WARFARE_IsDowned")) {
            if (!pLevel.isClientSide && (pPlayer.getTeam() == null || !pPlayer.getTeam().isAlliedTo(target.getTeam()))) {
                pPlayer.sendSystemMessage(Component.literal("§cYou cannot revive an ENEMY!"));
                return InteractionResultHolder.fail(pPlayer.getItemInHand(pUsedHand));
            }
            pPlayer.getItemInHand(pUsedHand).getOrCreateTag().putBoolean("Reviving", true);
            pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), ModSounds.BANDAGE_START.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
            pPlayer.startUsingItem(pUsedHand);
            return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
        }
        if (toHeal.getHealth() >= 14.0f && !toHeal.hasEffect(ModEffects.BLEEDING.get())) {
            if (!pLevel.isClientSide) {
                if (toHeal == pPlayer) {
                    pPlayer.sendSystemMessage(Component.literal("§cNeed a medkit"));
                } else {
                    pPlayer.sendSystemMessage(Component.literal("§cPlayer doesn't need a bandage"));
                }
            }
            return InteractionResultHolder.fail(pPlayer.getItemInHand(pUsedHand));
        }
        pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), ModSounds.BANDAGE_START.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
        pPlayer.startUsingItem(pUsedHand);
        return InteractionResultHolder.success(pPlayer.getItemInHand(pUsedHand));
    }

    public ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving) {
        if (!pLevel.isClientSide && pEntityLiving instanceof Player) {
            Player player = (Player)pEntityLiving;
            Player target = this.getTargetPlayer(player);
            Player toHeal = target != null ? target : player;
            if (target != null && target.getPersistentData().getBoolean("WARFARE_IsDowned")) {
                com.pigeostudios.pwp.warfare.events.DownedHandler.revivePlayer((net.minecraft.server.level.ServerPlayer)target);
                com.pigeostudios.pwp.warfare.stats.MatchStatsTracker.get().recordRevive((net.minecraft.server.level.ServerPlayer)player);
                player.sendSystemMessage(Component.literal("§aTeammate revived!"));
            } else {
                float currentHealth = toHeal.getHealth();
                if (currentHealth < 14.0f) {
                    float healed = Math.min(14.0f, currentHealth + 6.0f) - currentHealth;
                    toHeal.setHealth(currentHealth + healed);
                    if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                        com.pigeostudios.pwp.warfare.stats.MatchStatsTracker.get().recordHealing(sp, healed);
                    }
                }
            }
            if (!player.getAbilities().instabuild) {
                pStack.shrink(1);
            }
            pLevel.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BANDAGE_FINISH.get(), SoundSource.PLAYERS, 0.8f, 1.0f);
            toHeal.removeEffect(ModEffects.BLEEDING.get());
            EventHandler.BLEEDING_SOURCES.remove(toHeal.getUUID());
        }
        pStack.getOrCreateTag().remove("Reviving");
        return pStack;
    }

    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeCharged) {
        pStack.getOrCreateTag().remove("Reviving");
    }

    private Player getTargetPlayer(Player player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.scale(3.0));
        AABB aabb = player.getBoundingBox().inflate(3.0);
        HitResult hit = ProjectileUtil.getEntityHitResult(player, eyePos, reachVec, aabb, entity -> entity instanceof Player, 3.0);
        if (hit instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof Player target) {
            return target;
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable)this, "controller", 0, event -> {
            Player player = ClientUtils.getClientPlayer();
            if (player != null && player.isUsingItem() && player.getUseItem().is((Item)this)) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("use"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        })});
    }

    public InteractionResult interactLivingEntity(ItemStack pStack, Player pPlayer, LivingEntity pInteractionTarget, InteractionHand pUsedHand) {
        if (pInteractionTarget instanceof Player target) {
            if (target.getPersistentData().getBoolean("WARFARE_IsDowned")) {
                if (!pPlayer.level().isClientSide && (pPlayer.getTeam() == null || !pPlayer.getTeam().isAlliedTo(target.getTeam()))) {
                    pPlayer.sendSystemMessage(Component.literal("§cYou cannot revive an ENEMY!"));
                    return InteractionResult.FAIL;
                }
                pStack.getOrCreateTag().putBoolean("Reviving", true);
                pPlayer.level().playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), ModSounds.BANDAGE_START.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
                pPlayer.startUsingItem(pUsedHand);
                return InteractionResult.SUCCESS;
            }
            if (target.getHealth() >= 14.0f && !target.hasEffect(ModEffects.BLEEDING.get())) {
                if (!pPlayer.level().isClientSide) {
                    pPlayer.sendSystemMessage(Component.literal("§cPlayer doesn't need a bandage"));
                }
                return InteractionResult.FAIL;
            }
            pPlayer.level().playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), ModSounds.BANDAGE_START.get(), SoundSource.PLAYERS, 0.7f, 1.0f);
            pPlayer.startUsingItem(pUsedHand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions(){
            private BandageRenderer renderer;

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new BandageRenderer();
                }
                return this.renderer;
            }
        });
    }
}
