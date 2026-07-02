package com.pigeostudios.pwp.warfare.entity;

import com.pigeostudios.pwp.warfare.item.AGSAmmoItem;
import com.pigeostudios.pwp.warfare.item.ModItems;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRecoil;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimatableManager.ControllerRegistrar;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

// Сущность автоматического гранатомёта АГС-30
// Стационарный гранатомёт, который игрок может использовать для стрельбы гранатами
public class AGS30Entity extends Entity implements GeoEntity {
   private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
   private static final EntityDataAccessor<Float> TURRET_YAW = SynchedEntityData.defineId(AGS30Entity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> TURRET_PITCH = SynchedEntityData.defineId(AGS30Entity.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> HAS_MAGAZINE = SynchedEntityData.defineId(AGS30Entity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> AMMO_COUNT = SynchedEntityData.defineId(AGS30Entity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> IS_AIMING = SynchedEntityData.defineId(AGS30Entity.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.defineId(AGS30Entity.class, EntityDataSerializers.BOOLEAN);
   private int shootCooldown = 0;
   private int firingResetTimer = 0;
   private static final int FIRE_RATE_TICKS = 2;

   public AGS30Entity(EntityType<?> type, Level level) {
      super(type, level);
      this.blocksBuilding = true;
   }

   public AGS30Entity(Level level, double x, double y, double z) {
      this((EntityType<?>)ModEntities.AGS_30.get(), level);
      this.setPos(x, y, z);
   }

   protected void defineSynchedData() {
      this.entityData.define(TURRET_YAW, 0.0F);
      this.entityData.define(TURRET_PITCH, 0.0F);
      this.entityData.define(HAS_MAGAZINE, false);
      this.entityData.define(AMMO_COUNT, 0);
      this.entityData.define(IS_AIMING, false);
      this.entityData.define(IS_FIRING, false);
   }

   public boolean hasMagazine() {
      return (Boolean)this.entityData.get(HAS_MAGAZINE);
   }

   public void setHasMagazine(boolean has) {
      this.entityData.set(HAS_MAGAZINE, has);
   }

   public int getAmmoCount() {
      return (Integer)this.entityData.get(AMMO_COUNT);
   }

   public void setAmmoCount(int count) {
      this.entityData.set(AMMO_COUNT, count);
   }

   public void setTurretYaw(float rot) {
      this.entityData.set(TURRET_YAW, rot);
   }

   public float getTurretYaw() {
      return (Float)this.entityData.get(TURRET_YAW);
   }

   public void setTurretPitch(float rot) {
      this.entityData.set(TURRET_PITCH, rot);
   }

   public float getTurretPitch() {
      return (Float)this.entityData.get(TURRET_PITCH);
   }

   public boolean isFiring() {
      return (Boolean)this.entityData.get(IS_FIRING);
   }

   public void setFiring(boolean firing) {
      this.entityData.set(IS_FIRING, firing);
   }

   public boolean isAiming() {
      return (Boolean)this.entityData.get(IS_AIMING);
   }

   public void setAiming(boolean aiming) {
      this.entityData.set(IS_AIMING, aiming);
   }

   public boolean isAttackable() {
      return true;
   }

   public boolean hurt(DamageSource source, float amount) {
      if (!this.level().isClientSide && !this.isRemoved()) {
         if (source.getEntity() instanceof Player player && player.isCreative()) {
            this.discard();
            return true;
         } else if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            this.discard();
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide) {
         if (this.shootCooldown > 0) {
            this.shootCooldown--;
         }

         if (this.isFiring()) {
            this.firingResetTimer--;
            if (this.firingResetTimer <= 0) {
               this.setFiring(false);
            }
         }

         if (this.getFirstPassenger() instanceof Player player) {
            float playerYaw = player.getYRot();
            float playerPitch = player.getXRot();
            float targetRelativeYaw = Mth.wrapDegrees(playerYaw - this.getYRot());
            float currentTurretYaw = this.getTurretYaw();
            float smoothYaw = Mth.rotLerp(0.12F, currentTurretYaw, targetRelativeYaw);
            float limitedPitch = Math.max(-20.0F, Math.min(45.0F, playerPitch));
            this.setTurretYaw(smoothYaw);
            this.setTurretPitch(limitedPitch);
            if (playerPitch < -20.0F || playerPitch > 45.0F) {
               player.setXRot(limitedPitch);
            }
         } else {
            if (this.isAiming()) {
               this.setAiming(false);
            }

            if (this.isFiring()) {
               this.setFiring(false);
            }
         }
      }
   }

   // Попытка выстрелить из гранатомёта
   public void tryShoot(Player shooter) {
      if (!this.level().isClientSide) {
         if ((!this.hasMagazine() || this.getAmmoCount() <= 0) && !shooter.isCreative()) {
            this.setFiring(false);
         } else {
            this.setFiring(true);
            this.firingResetTimer = 10;
         }
      }

      if (this.shootCooldown <= 0) {
         if (!shooter.isCreative()) {
            if (!this.hasMagazine()) {
               shooter.displayClientMessage(Component.literal("No Ammo Box!").withStyle(ChatFormatting.RED), true);
               this.shootCooldown = 20;
               return;
            }

            if (this.getAmmoCount() <= 0) {
               shooter.displayClientMessage(Component.literal("Empty!").withStyle(ChatFormatting.RED), true);
               this.shootCooldown = 20;
               return;
            }
         }

         if (!this.level().isClientSide) {
            AGS30GrenadeEntity grenade = new AGS30GrenadeEntity(this.level(), shooter);
            float pitch = this.getTurretPitch();
            float yaw = this.getYRot() + this.getTurretYaw();
            double pivotZ = 0.1875;
            double barrelLength = 1.4;
            double radsBase = Math.toRadians(this.getYRot());
            double pivotX = -Math.sin(radsBase) * pivotZ;
            double pivotZWorld = Math.cos(radsBase) * pivotZ;
            double yawRad = Math.toRadians(yaw);
            double pitchRad = Math.toRadians(pitch);
            double forwardX = -Math.sin(yawRad) * Math.cos(pitchRad) * barrelLength;
            double forwardZ = Math.cos(yawRad) * Math.cos(pitchRad) * barrelLength;
            double forwardY = -Math.sin(pitchRad) * barrelLength;
            double leftOffset = 0.0;
            double leftX = Math.cos(yawRad) * leftOffset;
            double leftZ = Math.sin(yawRad) * leftOffset;
            grenade.setPos(this.getX() + pivotX + forwardX + leftX, this.getY() + 0.35 + forwardY, this.getZ() + pivotZWorld + forwardZ + leftZ);
            grenade.shootFromRotation(this, pitch, yaw, 0.0F, 4.0F, 0.0F);
            this.level().addFreshEntity(grenade);
            this.playSound((SoundEvent)ModSounds.AGS_SHOOT.get(), 4.0F, 1.0F / (this.random.nextFloat() * 0.4F + 0.8F));
            if (shooter instanceof ServerPlayer serverPlayer) {
               float recoilKick = 4.0F;
               float sideShake = (this.random.nextFloat() - 1.0F) * 1.0F;
               PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new PacketRecoil(recoilKick, sideShake));
            }

            if (!shooter.isCreative()) {
               this.setAmmoCount(this.getAmmoCount() - 1);
            }

            this.shootCooldown = 2;
         }
      }
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
         ItemStack stack = player.getItemInHand(hand);
         if (stack.getItem() instanceof AGSAmmoItem) {
            if (!this.hasMagazine()) {
               int ammo = AGSAmmoItem.getAmmo(stack);
               this.setHasMagazine(true);
               this.setAmmoCount(ammo);
               if (!player.isCreative()) {
                  stack.shrink(1);
               }

               this.playSound((SoundEvent)ModSounds.M2_LOAD.get(), 1.0F, 1.0F);
               player.displayClientMessage(Component.literal("AGS Loaded: " + ammo + " rounds"), true);
               return InteractionResult.SUCCESS;
            }

            player.displayClientMessage(Component.literal("Already loaded!"), true);
            return InteractionResult.FAIL;
         }

         if (player.isShiftKeyDown() && stack.isEmpty() && this.hasMagazine()) {
            int remaining = this.getAmmoCount();
            ItemStack newBox = new ItemStack((ItemLike)ModItems.AGS_AMMO.get());
            AGSAmmoItem.setAmmo(newBox, remaining);
            player.setItemInHand(hand, newBox);
            this.setHasMagazine(false);
            this.setAmmoCount(0);
            this.playSound((SoundEvent)ModSounds.M2_UNLOAD.get(), 1.0F, 1.0F);
            player.displayClientMessage(Component.literal("AGS Unloaded (" + remaining + ")"), true);
            return InteractionResult.SUCCESS;
         }

         if (!player.isShiftKeyDown() && this.getPassengers().isEmpty()) {
            float gunFacingYaw = this.getYRot() + this.getTurretYaw();
            float gunFacingPitch = this.getTurretPitch();
            player.setYRot(gunFacingYaw);
            player.setYHeadRot(gunFacingYaw);
            player.setXRot(gunFacingPitch);
            player.startRiding(this);
            return InteractionResult.SUCCESS;
         }
      }

      return InteractionResult.PASS;
   }

   public void positionRider(Entity passenger, MoveFunction callback) {
      if (this.hasPassenger(passenger)) {
         double[] pos = this.calculateSeatPosition(this.getTurretYaw());
         callback.accept(passenger, pos[0], pos[1], pos[2]);
      }
   }

   // Расчёт позиции сиденья для игрока на турели
   private double[] calculateSeatPosition(float turretRelativeYaw) {
      double modelPivotOffsetZ = 0.1875;
      double baseRad = Math.toRadians(this.getYRot());
      double pivotX = this.getX() - Math.sin(baseRad) * modelPivotOffsetZ;
      double pivotZ = this.getZ() + Math.cos(baseRad) * modelPivotOffsetZ;
      double pivotY = this.getY() + (this.isAiming() ? -0.6 : -0.35);
      double seatDistance = 1.1;
      float seatAngle = this.getYRot() + turretRelativeYaw + 180.0F;
      double rads = Math.toRadians(seatAngle);
      return new double[]{pivotX + -Math.sin(rads) * seatDistance, pivotY, pivotZ + Math.cos(rads) * seatDistance};
   }

   public void registerControllers(ControllerRegistrar controllers) {
      controllers.add(
         new AnimationController[]{
            new AnimationController(
               this,
               "controller",
               0,
               event -> this.isFiring() && (this.getAmmoCount() > 0 || !this.hasMagazine()) && this.getAmmoCount() > 0 && this.hasMagazine()
                  ? event.setAndContinue(RawAnimation.begin().thenLoop("animation.ags.fire"))
                  : PlayState.STOP
            )
         }
      );
   }

   public AnimatableInstanceCache getAnimatableInstanceCache() {
      return this.cache;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      if (tag.contains("HasMagazine")) {
         this.setHasMagazine(tag.getBoolean("HasMagazine"));
      }

      if (tag.contains("AmmoCount")) {
         this.setAmmoCount(tag.getInt("AmmoCount"));
      }

      if (tag.contains("IsAiming")) {
         this.setAiming(tag.getBoolean("IsAiming"));
      }
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putBoolean("HasMagazine", this.hasMagazine());
      tag.putInt("AmmoCount", this.getAmmoCount());
      tag.putBoolean("IsAiming", this.isAiming());
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return NetworkHooks.getEntitySpawningPacket(this);
   }

   public boolean isPickable() {
      return true;
   }
}
