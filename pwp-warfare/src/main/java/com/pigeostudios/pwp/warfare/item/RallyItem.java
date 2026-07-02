package com.pigeostudios.pwp.warfare.item;

import com.pigeostudios.pwp.warfare.client.ClientHooks;
import com.pigeostudios.pwp.warfare.client.ClientItemExtensions;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

// Предмет — радиостанция командира отряда
// Позволяет устанавливать точки сбора, отдавать приказы и управлять отрядом
public class RallyItem extends Item implements GeoItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return !slotChanged && oldStack.getItem() == newStack.getItem() ? false : super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
   }

   public RallyItem() {
      super(new Properties().stacksTo(1));
      SingletonGeoAnimatable.registerSyncedAnimatable(this);
   }

   private long getOrAssignID(ItemStack stack, Level level) {
      if (!stack.getOrCreateTag().contains("GeckoLibID")) {
         stack.getOrCreateTag().putLong("GeckoLibID", level.getRandom().nextLong());
      }

      return stack.getOrCreateTag().getLong("GeckoLibID");
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack stack = player.getItemInHand(hand);
      this.getOrAssignID(stack, level);
      if (!level.isClientSide) {
         level.playSound(null, player.getX(), player.getY(), player.getZ(), (SoundEvent)ModSounds.RADIO_OPEN.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
      } else {
         ClientHooks.tryOpenRadioMenu(player);
      }

      return InteractionResultHolder.consume(stack);
   }

   public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
      return true;
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(ClientItemExtensions.RALLY_RADIO);
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(
         new AnimationController[]{
            new AnimationController(this, "RadioController", 5, event -> PlayState.CONTINUE)
               .triggerableAnim("deploy", RawAnimation.begin().thenPlayAndHold("animation.radio.deploy"))
               .triggerableAnim("close", RawAnimation.begin().thenPlay("animation.radio.close"))
         }
      );
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }
}
