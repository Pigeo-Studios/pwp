/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.particles.BlockParticleOption
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResultHolder
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.Item
 *  net.minecraft.world.item.Item$Properties
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.item.UseAnim
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
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

import com.example.aas.block.AGSConstructionBlockEntity;
import com.example.aas.block.BarbedWireBlock;
import com.example.aas.block.BarbedWireBlockEntity;
import com.example.aas.block.HubBlock;
import com.example.aas.block.HubBlockEntity;
import com.example.aas.block.M2ConstructionBlockEntity;
import com.example.aas.block.ModBlocks;
import com.example.aas.block.MortarConstructionBlockEntity;
import com.example.aas.block.TOWConstructionBlockEntity;
import com.example.aas.block.WallBlock;
import com.example.aas.block.WallBlockEntity;
import com.example.aas.client.ClientItemExtensions;
import com.example.aas.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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

public class EntrenchingToolItem
extends Item
implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);

    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (this.isAASConstruction(state) && !this.isConstructed(state)) {
            return 20.0f;
        }
        return super.getDestroySpeed(stack, state);
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (!slotChanged && oldStack.getItem() == newStack.getItem()) {
            return false;
        }
        return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
    }

    public EntrenchingToolItem() {
        super(new Item.Properties().stacksTo(1));
        SingletonGeoAnimatable.registerSyncedAnimatable((GeoAnimatable)this);
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
        BlockState state;
        ItemStack itemstack = player.getItemInHand(hand);
        HitResult hit = player.pick(4.5, 0.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK && this.isAASConstruction(state = level.getBlockState(((BlockHitResult)hit).getBlockPos())) && !this.isConstructed(state)) {
            if (!level.isClientSide) {
                long id = this.getOrAssignID(itemstack, level);
                this.triggerAnim((Entity)player, id, "ShovelController", "dig");
            }
            player.startUsingItem(hand);
            return InteractionResultHolder.consume((Object)itemstack);
        }
        return InteractionResultHolder.pass((Object)itemstack);
    }

    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int count) {
        if (entity instanceof Player) {
            Player player = (Player)entity;
            if (!level.isClientSide) {
                HitResult hit = player.pick(4.5, 0.0f, false);
                if (hit instanceof BlockHitResult) {
                    BlockHitResult blockHit = (BlockHitResult)hit;
                    BlockPos pos = blockHit.getBlockPos();
                    BlockState state = level.getBlockState(pos);
                    BlockEntity be = level.getBlockEntity(pos);
                    if (this.isAASConstruction(state)) {
                        if (this.isConstructed(state)) {
                            this.stopDigging((LivingEntity)player, stack);
                            return;
                        }
                        String playerTeam = player.getTeam() != null ? player.getTeam().getName() : "NEUTRAL";
                        String structureTeam = this.getStructureTeam(be);
                        if (!(structureTeam.equals("NEUTRAL") || structureTeam.equalsIgnoreCase(playerTeam) || player.isCreative())) {
                            this.stopDigging((LivingEntity)player, stack);
                            return;
                        }
                        int elapsed = this.getUseDuration(stack) - count;
                        if (elapsed % 20 == 10) {
                            level.playSound(null, player.getX(), player.getY(), player.getZ(), (SoundEvent)ModSounds.SHOVEL_DIG.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
                            ((ServerLevel)level).sendParticles((ParticleOptions)new BlockParticleOption(ParticleTypes.BLOCK, state), (double)pos.getX() + 0.5, (double)pos.getY() + 1.1, (double)pos.getZ() + 0.5, 12, 0.2, 0.2, 0.2, 0.1);
                        }
                        if (player.isCreative()) {
                            this.addCreativeProgressToBE(be, 50);
                        } else {
                            this.addProgressToBE(be, player);
                        }
                    } else {
                        this.stopDigging((LivingEntity)player, stack);
                    }
                } else {
                    this.stopDigging((LivingEntity)player, stack);
                }
            }
        }
    }

    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        this.stopDigging(entity, stack);
    }

    private void stopDigging(LivingEntity entity, ItemStack stack) {
        if (entity instanceof Player) {
            Player player = (Player)entity;
            if (!player.level().isClientSide) {
                long id = this.getOrAssignID(stack, player.level());
                this.triggerAnim((Entity)player, id, "ShovelController", "stop");
                player.stopUsingItem();
            }
        }
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable)this, "ShovelController", 5, event -> PlayState.CONTINUE).triggerableAnim("dig", RawAnimation.begin().thenLoop("animation.shovel.dig")).triggerableAnim("stop", RawAnimation.begin().thenPlay("animation.nothing"))});
    }

    private boolean isAASConstruction(BlockState state) {
        return state.is((Block)ModBlocks.HUB_BLOCK.get()) || state.is((Block)ModBlocks.WALL_BLOCK.get()) || state.is((Block)ModBlocks.BARBED_WIRE_BLOCK.get()) || state.is((Block)ModBlocks.M2_CONSTRUCTION_BLOCK.get()) || state.is((Block)ModBlocks.AGS_CONSTRUCTION_BLOCK.get()) || state.is((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get()) || state.is((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get());
    }

    private String getStructureTeam(BlockEntity be) {
        if (be instanceof HubBlockEntity) {
            HubBlockEntity h = (HubBlockEntity)be;
            return h.getTeam();
        }
        if (be instanceof WallBlockEntity) {
            WallBlockEntity w = (WallBlockEntity)be;
            return w.getTeam();
        }
        if (be instanceof BarbedWireBlockEntity) {
            BarbedWireBlockEntity b = (BarbedWireBlockEntity)be;
            return b.getTeam();
        }
        if (be instanceof AGSConstructionBlockEntity) {
            AGSConstructionBlockEntity a = (AGSConstructionBlockEntity)be;
            return a.getTeam();
        }
        if (be instanceof M2ConstructionBlockEntity) {
            M2ConstructionBlockEntity m = (M2ConstructionBlockEntity)be;
            return m.getTeam();
        }
        if (be instanceof MortarConstructionBlockEntity) {
            MortarConstructionBlockEntity mo = (MortarConstructionBlockEntity)be;
            return mo.getTeam();
        }
        if (be instanceof TOWConstructionBlockEntity) {
            TOWConstructionBlockEntity t = (TOWConstructionBlockEntity)be;
            return t.getTeam();
        }
        return "NEUTRAL";
    }

    private void addCreativeProgressToBE(BlockEntity be, int multiplier) {
        if (be instanceof HubBlockEntity) {
            HubBlockEntity b = (HubBlockEntity)be;
            b.addCreativeProgress(multiplier);
        } else if (be instanceof WallBlockEntity) {
            WallBlockEntity b = (WallBlockEntity)be;
            b.addCreativeProgress(multiplier);
        } else if (be instanceof BarbedWireBlockEntity) {
            BarbedWireBlockEntity b = (BarbedWireBlockEntity)be;
            b.addCreativeProgress(multiplier);
        } else if (be instanceof AGSConstructionBlockEntity) {
            AGSConstructionBlockEntity b = (AGSConstructionBlockEntity)be;
            b.addCreativeProgress(multiplier);
        } else if (be instanceof M2ConstructionBlockEntity) {
            M2ConstructionBlockEntity b = (M2ConstructionBlockEntity)be;
            b.addCreativeProgress(multiplier);
        } else if (be instanceof MortarConstructionBlockEntity) {
            MortarConstructionBlockEntity b = (MortarConstructionBlockEntity)be;
            b.addCreativeProgress(multiplier);
        } else if (be instanceof TOWConstructionBlockEntity) {
            TOWConstructionBlockEntity b = (TOWConstructionBlockEntity)be;
            b.addCreativeProgress(multiplier);
        }
    }

    private boolean isConstructed(BlockState state) {
        if (state.hasProperty((Property)WallBlock.CONSTRUCTED)) {
            return (Boolean)state.getValue((Property)WallBlock.CONSTRUCTED);
        }
        if (state.hasProperty((Property)HubBlock.CONSTRUCTED)) {
            return (Boolean)state.getValue((Property)HubBlock.CONSTRUCTED);
        }
        if (state.hasProperty((Property)BarbedWireBlock.CONSTRUCTED)) {
            return (Boolean)state.getValue((Property)BarbedWireBlock.CONSTRUCTED);
        }
        return false;
    }

    private void addProgressToBE(BlockEntity be, Player player) {
        if (be instanceof HubBlockEntity) {
            HubBlockEntity b = (HubBlockEntity)be;
            b.addProgress();
        } else if (be instanceof WallBlockEntity) {
            WallBlockEntity b = (WallBlockEntity)be;
            b.addProgress();
        } else if (be instanceof BarbedWireBlockEntity) {
            BarbedWireBlockEntity b = (BarbedWireBlockEntity)be;
            b.addProgress();
        } else if (be instanceof AGSConstructionBlockEntity) {
            AGSConstructionBlockEntity b = (AGSConstructionBlockEntity)be;
            b.addProgress();
        } else if (be instanceof M2ConstructionBlockEntity) {
            M2ConstructionBlockEntity b = (M2ConstructionBlockEntity)be;
            b.addProgress();
        } else if (be instanceof MortarConstructionBlockEntity) {
            MortarConstructionBlockEntity b = (MortarConstructionBlockEntity)be;
            b.addProgress();
        } else if (be instanceof TOWConstructionBlockEntity) {
            TOWConstructionBlockEntity b = (TOWConstructionBlockEntity)be;
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

