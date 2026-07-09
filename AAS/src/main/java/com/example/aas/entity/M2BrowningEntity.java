/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.ChatFormatting
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.chat.Component
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.network.syncher.EntityDataAccessor
 *  net.minecraft.network.syncher.EntityDataSerializer
 *  net.minecraft.network.syncher.EntityDataSerializers
 *  net.minecraft.network.syncher.SynchedEntityData
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.tags.DamageTypeTags
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.damagesource.DamageSource
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.Entity$MoveFunction
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraftforge.network.NetworkHooks
 *  net.minecraftforge.network.PacketDistributor
 *  software.bernie.geckolib.animatable.GeoEntity
 *  software.bernie.geckolib.core.animatable.GeoAnimatable
 *  software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache
 *  software.bernie.geckolib.core.animation.AnimatableManager$ControllerRegistrar
 *  software.bernie.geckolib.core.animation.AnimationController
 *  software.bernie.geckolib.core.animation.RawAnimation
 *  software.bernie.geckolib.core.object.PlayState
 *  software.bernie.geckolib.util.GeckoLibUtil
 */
package com.example.aas.entity;

import com.example.aas.entity.M2BulletEntity;
import com.example.aas.entity.ModEntities;
import com.example.aas.item.M2AmmoItem;
import com.example.aas.item.ModItems;
import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketRecoil;
import com.example.aas.sound.ModSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializer;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

public class M2BrowningEntity
extends Entity
implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    static private final EntityDataAccessor<Float> TURRET_YAW = SynchedEntityData.defineId(M2BrowningEntity.class, (EntityDataSerializer)EntityDataSerializers.FLOAT);
    static private final EntityDataAccessor<Float> TURRET_PITCH = SynchedEntityData.defineId(M2BrowningEntity.class, (EntityDataSerializer)EntityDataSerializers.FLOAT);
    static private final EntityDataAccessor<Boolean> HAS_MAGAZINE = SynchedEntityData.defineId(M2BrowningEntity.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    static private final EntityDataAccessor<Integer> AMMO_COUNT = SynchedEntityData.defineId(M2BrowningEntity.class, (EntityDataSerializer)EntityDataSerializers.INT);
    static private final EntityDataAccessor<Boolean> IS_AIMING = SynchedEntityData.defineId(M2BrowningEntity.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    static private final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.defineId(M2BrowningEntity.class, (EntityDataSerializer)EntityDataSerializers.BOOLEAN);
    private final boolean DEBUG_MODE = false;
    private final float SMOOTH_SPEED = 0.12f;
    private int shootCooldown = 0;
    private int firingResetTimer = 0;
    static private final int FIRE_RATE_TICKS = 2;

    public M2BrowningEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    public M2BrowningEntity(Level level, double x, double y, double z) {
        this((EntityType)ModEntities.M2_BROWNING.get(), level);
        this.setPos(x, y, z);
    }

    protected void defineSynchedData() {
        this.entityData.define(TURRET_YAW, (Object)Float.valueOf(0.0f));
        this.entityData.define(TURRET_PITCH, (Object)Float.valueOf(0.0f));
        this.entityData.define(HAS_MAGAZINE, (Object)false);
        this.entityData.define(AMMO_COUNT, (Object)0);
        this.entityData.define(IS_AIMING, (Object)false);
        this.entityData.define(IS_FIRING, (Object)false);
    }

    public boolean isAiming() {
        return (Boolean)this.entityData.get(IS_AIMING);
    }

    public void setAiming(boolean aiming) {
        this.entityData.set(IS_AIMING, (Object)aiming);
    }

    public boolean hasMagazine() {
        return (Boolean)this.entityData.get(HAS_MAGAZINE);
    }

    public void setHasMagazine(boolean has) {
        this.entityData.set(HAS_MAGAZINE, (Object)has);
    }

    public int getAmmoCount() {
        return (Integer)this.entityData.get(AMMO_COUNT);
    }

    public void setAmmoCount(int count) {
        this.entityData.set(AMMO_COUNT, (Object)count);
    }

    public void setTurretYaw(float rot) {
        this.entityData.set(TURRET_YAW, (Object)Float.valueOf(rot));
    }

    public float getTurretYaw() {
        return ((Float)this.entityData.get(TURRET_YAW)).floatValue();
    }

    public void setTurretPitch(float rot) {
        this.entityData.set(TURRET_PITCH, (Object)Float.valueOf(rot));
    }

    public float getTurretPitch() {
        return ((Float)this.entityData.get(TURRET_PITCH)).floatValue();
    }

    public boolean isFiring() {
        return (Boolean)this.entityData.get(IS_FIRING);
    }

    public void setFiring(boolean firing) {
        this.entityData.set(IS_FIRING, (Object)firing);
    }

    public boolean isAttackable() {
        return true;
    }

    public boolean hurt(DamageSource source, float amount) {
        Player player;
        if (this.level().isClientSide || this.isRemoved()) {
            return false;
        }
        Entity entity = source.getEntity();
        if (entity instanceof Player && (player = (Player)entity).isCreative()) {
            this.discard();
            return true;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            this.discard();
            return true;
        }
        return false;
    }

    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            if (this.shootCooldown > 0) {
                --this.shootCooldown;
            }
            if (this.isFiring()) {
                --this.firingResetTimer;
                if (this.firingResetTimer <= 0) {
                    this.setFiring(false);
                }
            }
        }
        if (this.level().isClientSide) {
            // empty if block
        }
        if (!this.level().isClientSide) {
            Entity passenger = this.getFirstPassenger();
            if (passenger instanceof Player) {
                Player player = (Player)passenger;
                float playerYaw = player.getYRot();
                float playerPitch = player.getXRot();
                float targetRelativeYaw = Mth.wrapDegrees((float)(playerYaw - this.getYRot()));
                float currentTurretYaw = this.getTurretYaw();
                float smoothYaw = Mth.rotLerp(0.12f, (float)currentTurretYaw, (float)targetRelativeYaw);
                float limitedPitch = Math.max(-45.0f, Math.min(30.0f, playerPitch));
                this.setTurretYaw(smoothYaw);
                this.setTurretPitch(limitedPitch);
                if (playerPitch < -45.0f || playerPitch > 30.0f) {
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

    public void tryShoot(Player shooter) {
        if (!this.level().isClientSide) {
            this.setFiring(true);
            this.firingResetTimer = 5;
        }
        if (this.shootCooldown > 0) {
            return;
        }
        if (!shooter.isCreative()) {
            if (!this.hasMagazine()) {
                shooter.displayClientMessage((Component)Component.literal((String)"No Magazine!").withStyle(ChatFormatting.RED), true);
                this.shootCooldown = 20;
                return;
            }
            if (this.getAmmoCount() <= 0) {
                shooter.displayClientMessage((Component)Component.literal((String)"Empty!").withStyle(ChatFormatting.RED), true);
                this.shootCooldown = 20;
                return;
            }
        }
        if (!this.level().isClientSide) {
            M2BulletEntity bullet = new M2BulletEntity(this.level(), (LivingEntity)shooter);
            float pitch = this.getTurretPitch();
            float yaw = this.getYRot() + this.getTurretYaw();
            double pivotZ = 0.03125;
            double barrelLength = 3.0;
            double pivotHeight = 1.4;
            double radsBase = Math.toRadians(this.getYRot());
            double pivotX = -Math.sin(radsBase) * pivotZ;
            double pivotZWorld = Math.cos(radsBase) * pivotZ;
            double yawRad = Math.toRadians(yaw);
            double pitchRad = Math.toRadians(pitch);
            double forwardX = -Math.sin(yawRad) * Math.cos(pitchRad) * barrelLength;
            double forwardZ = Math.cos(yawRad) * Math.cos(pitchRad) * barrelLength;
            double forwardY = -Math.sin(pitchRad) * barrelLength;
            double rightOffset = 0.05;
            double rightX = -Math.cos(yawRad) * rightOffset;
            double rightZ = -Math.sin(yawRad) * rightOffset;
            bullet.setPos(this.getX() + pivotX + forwardX + rightX, this.getY() + pivotHeight + forwardY - 0.7 + 0.05, this.getZ() + pivotZWorld + forwardZ + rightZ);
            bullet.shootFromRotation(this, pitch, yaw, 0.0f, 3.0f, 0.05f);
            this.level().addFreshEntity((Entity)bullet);
            this.playSound((SoundEvent)ModSounds.M2_SHOOT.get(), 3.5f, 1.0f / (this.random.nextFloat() * 0.4f + 0.8f));
            if (shooter instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)shooter;
                float recoilKick = 1.0f;
                float sideShake = (this.random.nextFloat() - 0.5f) * 0.5f;
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), (Object)new PacketRecoil(recoilKick, sideShake));
            }
            if (!shooter.isCreative()) {
                this.setAmmoCount(this.getAmmoCount() - 1);
            }
            this.shootCooldown = 2;
        }
    }

    private double[] calculateSeatPosition(float turretRelativeYaw) {
        double modelPivotOffsetZ = 0.03125;
        double baseRad = Math.toRadians(this.getYRot());
        double pivotX = this.getX() - Math.sin(baseRad) * modelPivotOffsetZ;
        double pivotZ = this.getZ() + Math.cos(baseRad) * modelPivotOffsetZ;
        double yOffset = this.isAiming() ? -0.7 : -0.55;
        double pivotY = this.getY() + yOffset;
        double seatDistance = this.isAiming() ? 0.85 : 1.35;
        float seatAngle = this.getYRot() + turretRelativeYaw + 180.0f;
        double rads = Math.toRadians(seatAngle);
        return new double[]{pivotX + -Math.sin(rads) * seatDistance, pivotY, pivotZ + Math.cos(rads) * seatDistance};
    }

    public void positionRider(Entity passenger, Entity.MoveFunction callback) {
        if (this.hasPassenger(passenger)) {
            double[] pos = this.calculateSeatPosition(this.getTurretYaw());
            callback.accept(passenger, pos[0], pos[1], pos[2]);
        }
    }

    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.level().isClientSide && hand == InteractionHand.MAIN_HAND) {
            ItemStack stack = player.getItemInHand(hand);
            if (player.isShiftKeyDown() && stack.isEmpty() && this.hasMagazine()) {
                int remainingAmmo = this.getAmmoCount();
                ItemStack newBox = new ItemStack((ItemLike)ModItems.M2_AMMO.get());
                M2AmmoItem.setAmmo(newBox, remainingAmmo);
                player.setItemInHand(hand, newBox);
                this.setHasMagazine(false);
                this.setAmmoCount(0);
                this.playSound((SoundEvent)ModSounds.M2_UNLOAD.get(), 1.0f, 1.0f);
                player.displayClientMessage((Component)Component.literal((String)("Unloaded (" + remainingAmmo + ")")), true);
                return InteractionResult.SUCCESS;
            }
            if (stack.getItem() instanceof M2AmmoItem) {
                if (!this.hasMagazine()) {
                    int ammoInBox = M2AmmoItem.getAmmo(stack);
                    this.setHasMagazine(true);
                    this.setAmmoCount(ammoInBox);
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    this.playSound((SoundEvent)ModSounds.M2_LOAD.get(), 1.0f, 1.0f);
                    player.displayClientMessage((Component)Component.literal((String)("Loaded: " + ammoInBox + " rounds")), true);
                    return InteractionResult.SUCCESS;
                }
                player.displayClientMessage((Component)Component.literal((String)"Already loaded!"), true);
                return InteractionResult.FAIL;
            }
            if (!player.isShiftKeyDown() && this.getPassengers().isEmpty()) {
                float tripodYaw = this.getYRot();
                float turretYaw = this.getTurretYaw();
                player.setYRot(tripodYaw + turretYaw);
                player.setYHeadRot(tripodYaw + turretYaw);
                player.setXRot(this.getTurretPitch());
                player.startRiding((Entity)this);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable)this, "controller", 0, event -> {
            if (this.isFiring() && this.getAmmoCount() > 0 && this.hasMagazine()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.machinegun.fire"));
            }
            return PlayState.STOP;
        })});
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
        return NetworkHooks.getEntitySpawningPacket((Entity)this);
    }

    public boolean isPickable() {
        return true;
    }
}

