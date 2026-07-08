/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.client.extensions.common.IClientItemExtensions
 *  software.bernie.geckolib.animatable.GeoItem
 *  software.bernie.geckolib.animatable.SingletonGeoAnimatable
 *  software.bernie.geckolib.core.animatable.GeoAnimatable
 *  software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
 *  software.bernie.geckolib.core.animation.AnimatableManager$ControllerRegistrar
 *  software.bernie.geckolib.core.animation.AnimationController
 *  software.bernie.geckolib.core.animation.RawAnimation
 *  software.bernie.geckolib.core.object.PlayState
 *  software.bernie.geckolib.util.GeckoLibUtil
 */
package com.example.aas.item;

import com.example.aas.client.ClientHooks;
import com.example.aas.client.ClientItemExtensions;
import com.example.aas.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RallyItem
extends Item
implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (!slotChanged && oldStack.m_41720_() == newStack.m_41720_()) {
            return false;
        }
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    public RallyItem() {
        super(new Item.Properties().m_41487_(1));
        SingletonGeoAnimatable.registerSyncedAnimatable((GeoAnimatable)this);
    }

    private long getOrAssignID(ItemStack stack, Level level) {
        if (!stack.m_41784_().m_128441_("GeckoLibID")) {
            stack.m_41784_().m_128356_("GeckoLibID", level.m_213780_().m_188505_());
        }
        return stack.m_41784_().m_128454_("GeckoLibID");
    }

    public InteractionResultHolder<ItemStack> m_7203_(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.m_21120_(hand);
        this.getOrAssignID(stack, level);
        if (!level.f_46443_) {
            level.m_6263_(null, player.m_20185_(), player.m_20186_(), player.m_20189_(), (SoundEvent)ModSounds.RADIO_OPEN.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        } else {
            ClientHooks.tryOpenRadioMenu(player);
        }
        return InteractionResultHolder.m_19096_((Object)stack);
    }

    public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
        return true;
    }

    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(ClientItemExtensions.RALLY_RADIO);
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable)this, "RadioController", 5, event -> PlayState.CONTINUE).triggerableAnim("deploy", RawAnimation.begin().thenPlayAndHold("animation.radio.deploy")).triggerableAnim("close", RawAnimation.begin().thenPlay("animation.radio.close"))});
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}

