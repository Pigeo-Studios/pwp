package com.pigeostudios.pwp.medicine.item;

import java.util.function.Consumer;
import com.pigeostudios.pwp.medicine.config.PWPConfig;
import com.pigeostudios.pwp.medicine.effect.ModEffects;
import com.pigeostudios.pwp.medicine.item.client.MedkitRenderer;
import com.pigeostudios.pwp.medicine.sound.ModSounds;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
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

public class MedkitItem
extends Item
implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);

    public MedkitItem(Item.Properties props) {
        super(props);
    }

    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.BOW;
    }

    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (!level.isClientSide && entity instanceof Player) {
            Player toHeal;
            Player player = (Player)entity;
            Player target = this.getTargetPlayer(player);
            Player player2 = toHeal = target != null ? target : player;
            if (toHeal.hasEffect(ModEffects.BLEEDING.get())) {
                if (player.tickCount % 20 == 0) {
                    player.sendSystemMessage(Component.literal("§cНельзя лечить: цель истекает кровью!"));
                }
                return;
            }
            int applyTime = PWPConfig.MEDKIT_APPLY_TIME.get();
            int used = this.getUseDuration(stack) - count;
            if (used > 0 && used % applyTime == 0 && toHeal.getHealth() < toHeal.getMaxHealth()) {
                toHeal.heal(2.0f);
                if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                    com.pigeostudios.pwp.warfare.stats.MatchStatsTracker.get().recordHealing(sp, 2.0f);
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.MEDKIT_APPLY.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
            }
        }
    }

    private Player getTargetPlayer(Player player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getLookAngle();
        Vec3 reachVec = eyePos.add(lookVec.scale(3.0));
        AABB aabb = player.getBoundingBox().inflate(3.0);
        HitResult hit = ProjectileUtil.getEntityHitResult(player, eyePos, reachVec, aabb, e -> e instanceof Player, 3.0);
        if (hit instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof Player target) {
            return target;
        }
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable)this, "controller", 4, event -> {
            net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null && player.isUsingItem() && player.getUseItem().is((Item)this)) {
                return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("use_start"));
            }
            if (event.getController().getCurrentAnimation() != null && event.getController().getCurrentAnimation().animation().name().equals("use_start")) {
                return event.setAndContinue(RawAnimation.begin().thenPlay("use_end"));
            }
            return event.setAndContinue(RawAnimation.begin().thenLoop("idle"));
        })});
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions(){
            private MedkitRenderer renderer;

            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new MedkitRenderer();
                }
                return this.renderer;
            }
        });
    }
}
