package com.pigeostudios.pwp.warfare.item;

import com.pigeostudios.pwp.warfare.block.AGSConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.BarbedWireBlock;
import com.pigeostudios.pwp.warfare.block.BarbedWireBlockEntity;
import com.pigeostudios.pwp.warfare.block.HubBlock;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.M2ConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.MortarConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.TOWConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.WallBlock;
import com.pigeostudios.pwp.warfare.block.WallBlockEntity;
import com.pigeostudios.pwp.warfare.client.ClientItemExtensions;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

// Предмет — сапёрная лопатка
// Используется для строительства укреплений (стены, FOB, турели)
public class EntrenchingToolItem extends Item implements GeoItem {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

   public float getDestroySpeed(ItemStack stack, BlockState state) {
      return this.isWarfareConstruction(state) && !this.isConstructed(state) ? 20.0F : super.getDestroySpeed(stack, state);
   }

   public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
      return !slotChanged && oldStack.getItem() == newStack.getItem() ? false : super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
   }

   public EntrenchingToolItem() {
      super(new Properties().stacksTo(1));
      SingletonGeoAnimatable.registerSyncedAnimatable(this);
   }

   private long getOrAssignID(ItemStack stack, Level level) {
      if (!stack.getOrCreateTag().contains("GeckoLibID")) {
         stack.getOrCreateTag().putLong("GeckoLibID", level.getRandom().nextLong());
      }

      return stack.getOrCreateTag().getLong("GeckoLibID");
   }

   public boolean onEntitySwing(ItemStack stack, LivingEntity entity) {
      return true;
   }

   public int getUseDuration(ItemStack stack) {
      return 72000;
   }

   public UseAnim getUseAnimation(ItemStack stack) {
      return UseAnim.NONE;
   }

   public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
      ItemStack itemstack = player.getItemInHand(hand);
      HitResult hit = player.pick(4.5, 0.0F, false);
      if (hit.getType() == Type.BLOCK) {
         BlockState state = level.getBlockState(((BlockHitResult)hit).getBlockPos());
         if (this.isWarfareConstruction(state) && !this.isConstructed(state)) {
            if (!level.isClientSide) {
               long id = this.getOrAssignID(itemstack, level);
               this.triggerAnim(player, id, "ShovelController", "dig");
            }

            player.startUsingItem(hand);
            return InteractionResultHolder.consume(itemstack);
         }
      }

      return InteractionResultHolder.pass(itemstack);
   }

   public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
      if (entity instanceof Player player && !level.isClientSide) {
         if (player.pick(4.5, 0.0F, false) instanceof BlockHitResult blockHit) {
            BlockPos pos = blockHit.getBlockPos();
            BlockState state = level.getBlockState(pos);
            BlockEntity be = level.getBlockEntity(pos);
            if (this.isWarfareConstruction(state)) {
               if (this.isConstructed(state)) {
                  this.stopDigging(player, stack);
                  return;
               }

               String playerTeam = player.getTeam() != null ? player.getTeam().getName() : "NEUTRAL";
               String structureTeam = this.getStructureTeam(be);
               if (!structureTeam.equals("NEUTRAL") && !structureTeam.equalsIgnoreCase(playerTeam) && !player.isCreative()) {
                  this.stopDigging(player, stack);
                  return;
               }

               int elapsed = this.getUseDuration(stack) - count;
               if (elapsed % 20 == 10) {
                  level.playSound(
                     null, player.getX(), player.getY(), player.getZ(), (SoundEvent)ModSounds.SHOVEL_DIG.get(), SoundSource.PLAYERS, 1.0F, 1.0F
                  );
                  ((ServerLevel)level)
                     .sendParticles(
                        new BlockParticleOption(ParticleTypes.BLOCK, state),
                        pos.getX() + 0.5,
                        pos.getY() + 1.1,
                        pos.getZ() + 0.5,
                        12,
                        0.2,
                        0.2,
                        0.2,
                        0.1
                     );
               }

               if (player.isCreative()) {
                  this.addCreativeProgressToBE(be, 50);
               } else {
                  this.addProgressToBE(be, player);
               }
            } else {
               this.stopDigging(player, stack);
            }
         } else {
            this.stopDigging(player, stack);
         }
      }
   }

   public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
      this.stopDigging(entity, stack);
   }

   private void stopDigging(LivingEntity entity, ItemStack stack) {
      if (entity instanceof Player player && !player.level().isClientSide) {
         long id = this.getOrAssignID(stack, player.level());
         this.triggerAnim(player, id, "ShovelController", "stop");
         player.stopUsingItem();
      }
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(
         new AnimationController[]{
            new AnimationController(this, "ShovelController", 5, event -> PlayState.CONTINUE)
               .triggerableAnim("dig", RawAnimation.begin().thenLoop("animation.shovel.dig"))
               .triggerableAnim("stop", RawAnimation.begin().thenPlay("animation.nothing"))
         }
      );
   }

   private boolean isWarfareConstruction(BlockState state) {
      return state.is((Block)ModBlocks.HUB_BLOCK.get())
         || state.is((Block)ModBlocks.WALL_BLOCK.get())
         || state.is((Block)ModBlocks.BARBED_WIRE_BLOCK.get())
         || state.is((Block)ModBlocks.M2_CONSTRUCTION_BLOCK.get())
         || state.is((Block)ModBlocks.AGS_CONSTRUCTION_BLOCK.get())
         || state.is((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get())
         || state.is((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get());
   }

   private String getStructureTeam(BlockEntity be) {
      if (be instanceof HubBlockEntity h) {
         return h.getTeam();
      } else if (be instanceof WallBlockEntity w) {
         return w.getTeam();
      } else if (be instanceof BarbedWireBlockEntity b) {
         return b.getTeam();
      } else if (be instanceof AGSConstructionBlockEntity a) {
         return a.getTeam();
      } else if (be instanceof M2ConstructionBlockEntity m) {
         return m.getTeam();
      } else if (be instanceof MortarConstructionBlockEntity mo) {
         return mo.getTeam();
      } else {
         return be instanceof TOWConstructionBlockEntity t ? t.getTeam() : "NEUTRAL";
      }
   }

   private void addCreativeProgressToBE(BlockEntity be, int multiplier) {
      if (be instanceof HubBlockEntity b) {
         b.addCreativeProgress(multiplier);
      } else if (be instanceof WallBlockEntity b) {
         b.addCreativeProgress(multiplier);
      } else if (be instanceof BarbedWireBlockEntity b) {
         b.addCreativeProgress(multiplier);
      } else if (be instanceof AGSConstructionBlockEntity b) {
         b.addCreativeProgress(multiplier);
      } else if (be instanceof M2ConstructionBlockEntity b) {
         b.addCreativeProgress(multiplier);
      } else if (be instanceof MortarConstructionBlockEntity b) {
         b.addCreativeProgress(multiplier);
      } else if (be instanceof TOWConstructionBlockEntity b) {
         b.addCreativeProgress(multiplier);
      }
   }

   private boolean isConstructed(BlockState state) {
      if (state.hasProperty(WallBlock.CONSTRUCTED)) {
         return (Boolean)state.getValue(WallBlock.CONSTRUCTED);
      } else if (state.hasProperty(HubBlock.CONSTRUCTED)) {
         return (Boolean)state.getValue(HubBlock.CONSTRUCTED);
      } else {
         return state.hasProperty(BarbedWireBlock.CONSTRUCTED) ? (Boolean)state.getValue(BarbedWireBlock.CONSTRUCTED) : false;
      }
   }

   private void addProgressToBE(BlockEntity be, Player player) {
      if (be instanceof HubBlockEntity b) {
         b.addProgress();
      } else if (be instanceof WallBlockEntity b) {
         b.addProgress();
      } else if (be instanceof BarbedWireBlockEntity b) {
         b.addProgress();
      } else if (be instanceof AGSConstructionBlockEntity b) {
         b.addProgress();
      } else if (be instanceof M2ConstructionBlockEntity b) {
         b.addProgress();
      } else if (be instanceof MortarConstructionBlockEntity b) {
         b.addProgress();
      } else if (be instanceof TOWConstructionBlockEntity b) {
         b.addProgress();
      }
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   public void initializeClient(Consumer<IClientItemExtensions> consumer) {
      consumer.accept(ClientItemExtensions.ENTRENCHING_TOOL);
   }
}
