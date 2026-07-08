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

import com.example.aas.entity.AGS30GrenadeEntity;
import com.example.aas.entity.ModEntities;
import com.example.aas.item.AGSAmmoItem;
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

public class AGS30Entity
extends Entity
implements GeoEntity {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache((GeoAnimatable)this);
    private static final EntityDataAccessor<Float> TURRET_YAW = SynchedEntityData.m_135353_(AGS30Entity.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    private static final EntityDataAccessor<Float> TURRET_PITCH = SynchedEntityData.m_135353_(AGS30Entity.class, (EntityDataSerializer)EntityDataSerializers.f_135029_);
    private static final EntityDataAccessor<Boolean> HAS_MAGAZINE = SynchedEntityData.m_135353_(AGS30Entity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    private static final EntityDataAccessor<Integer> AMMO_COUNT = SynchedEntityData.m_135353_(AGS30Entity.class, (EntityDataSerializer)EntityDataSerializers.f_135028_);
    private static final EntityDataAccessor<Boolean> IS_AIMING = SynchedEntityData.m_135353_(AGS30Entity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    private static final EntityDataAccessor<Boolean> IS_FIRING = SynchedEntityData.m_135353_(AGS30Entity.class, (EntityDataSerializer)EntityDataSerializers.f_135035_);
    private int shootCooldown = 0;
    private int firingResetTimer = 0;
    private static final int FIRE_RATE_TICKS = 2;

    public AGS30Entity(EntityType<?> type, Level level) {
        super(type, level);
        this.f_19850_ = true;
    }

    public AGS30Entity(Level level, double x, double y, double z) {
        this((EntityType)ModEntities.AGS_30.get(), level);
        this.m_6034_(x, y, z);
    }

    protected void m_8097_() {
        this.f_19804_.m_135372_(TURRET_YAW, (Object)Float.valueOf(0.0f));
        this.f_19804_.m_135372_(TURRET_PITCH, (Object)Float.valueOf(0.0f));
        this.f_19804_.m_135372_(HAS_MAGAZINE, (Object)false);
        this.f_19804_.m_135372_(AMMO_COUNT, (Object)0);
        this.f_19804_.m_135372_(IS_AIMING, (Object)false);
        this.f_19804_.m_135372_(IS_FIRING, (Object)false);
    }

    public boolean hasMagazine() {
        return (Boolean)this.f_19804_.m_135370_(HAS_MAGAZINE);
    }

    public void setHasMagazine(boolean has) {
        this.f_19804_.m_135381_(HAS_MAGAZINE, (Object)has);
    }

    public int getAmmoCount() {
        return (Integer)this.f_19804_.m_135370_(AMMO_COUNT);
    }

    public void setAmmoCount(int count) {
        this.f_19804_.m_135381_(AMMO_COUNT, (Object)count);
    }

    public void setTurretYaw(float rot) {
        this.f_19804_.m_135381_(TURRET_YAW, (Object)Float.valueOf(rot));
    }

    public float getTurretYaw() {
        return ((Float)this.f_19804_.m_135370_(TURRET_YAW)).floatValue();
    }

    public void setTurretPitch(float rot) {
        this.f_19804_.m_135381_(TURRET_PITCH, (Object)Float.valueOf(rot));
    }

    public float getTurretPitch() {
        return ((Float)this.f_19804_.m_135370_(TURRET_PITCH)).floatValue();
    }

    public boolean isFiring() {
        return (Boolean)this.f_19804_.m_135370_(IS_FIRING);
    }

    public void setFiring(boolean firing) {
        this.f_19804_.m_135381_(IS_FIRING, (Object)firing);
    }

    public boolean isAiming() {
        return (Boolean)this.f_19804_.m_135370_(IS_AIMING);
    }

    public void setAiming(boolean aiming) {
        this.f_19804_.m_135381_(IS_AIMING, (Object)aiming);
    }

    public boolean m_6097_() {
        return true;
    }

    public boolean m_6469_(DamageSource source, float amount) {
        Player player;
        if (this.m_9236_().f_46443_ || this.m_213877_()) {
            return false;
        }
        Entity entity = source.m_7639_();
        if (entity instanceof Player && (player = (Player)entity).m_7500_()) {
            this.m_146870_();
            return true;
        }
        if (source.m_269533_(DamageTypeTags.f_268415_)) {
            this.m_146870_();
            return true;
        }
        return false;
    }

    public void m_8119_() {
        super.m_8119_();
        if (!this.m_9236_().f_46443_) {
            Entity passenger;
            if (this.shootCooldown > 0) {
                --this.shootCooldown;
            }
            if (this.isFiring()) {
                --this.firingResetTimer;
                if (this.firingResetTimer <= 0) {
                    this.setFiring(false);
                }
            }
            if ((passenger = this.m_146895_()) instanceof Player) {
                Player player = (Player)passenger;
                float playerYaw = player.m_146908_();
                float playerPitch = player.m_146909_();
                float targetRelativeYaw = Mth.m_14177_((float)(playerYaw - this.m_146908_()));
                float currentTurretYaw = this.getTurretYaw();
                float smoothYaw = Mth.m_14189_((float)0.12f, (float)currentTurretYaw, (float)targetRelativeYaw);
                float limitedPitch = Math.max(-20.0f, Math.min(45.0f, playerPitch));
                this.setTurretYaw(smoothYaw);
                this.setTurretPitch(limitedPitch);
                if (playerPitch < -20.0f || playerPitch > 45.0f) {
                    player.m_146926_(limitedPitch);
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
        if (!this.m_9236_().f_46443_) {
            if (this.hasMagazine() && this.getAmmoCount() > 0 || shooter.m_7500_()) {
                this.setFiring(true);
                this.firingResetTimer = 10;
            } else {
                this.setFiring(false);
            }
        }
        if (this.shootCooldown > 0) {
            return;
        }
        if (!shooter.m_7500_()) {
            if (!this.hasMagazine()) {
                shooter.m_5661_((Component)Component.m_237113_((String)"No Ammo Box!").m_130940_(ChatFormatting.RED), true);
                this.shootCooldown = 20;
                return;
            }
            if (this.getAmmoCount() <= 0) {
                shooter.m_5661_((Component)Component.m_237113_((String)"Empty!").m_130940_(ChatFormatting.RED), true);
                this.shootCooldown = 20;
                return;
            }
        }
        if (!this.m_9236_().f_46443_) {
            AGS30GrenadeEntity grenade = new AGS30GrenadeEntity(this.m_9236_(), (LivingEntity)shooter);
            float pitch = this.getTurretPitch();
            float yaw = this.m_146908_() + this.getTurretYaw();
            double pivotZ = 0.1875;
            double barrelLength = 1.4;
            double radsBase = Math.toRadians(this.m_146908_());
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
            grenade.m_6034_(this.m_20185_() + pivotX + forwardX + leftX, this.m_20186_() + 0.35 + forwardY, this.m_20189_() + pivotZWorld + forwardZ + leftZ);
            grenade.m_37251_(this, pitch, yaw, 0.0f, 4.0f, 0.0f);
            this.m_9236_().m_7967_((Entity)grenade);
            this.m_5496_((SoundEvent)ModSounds.AGS_SHOOT.get(), 4.0f, 1.0f / (this.f_19796_.m_188501_() * 0.4f + 0.8f));
            if (shooter instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer)shooter;
                float recoilKick = 4.0f;
                float sideShake = (this.f_19796_.m_188501_() - 1.0f) * 1.0f;
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> serverPlayer), (Object)new PacketRecoil(recoilKick, sideShake));
            }
            if (!shooter.m_7500_()) {
                this.setAmmoCount(this.getAmmoCount() - 1);
            }
            this.shootCooldown = 2;
        }
    }

    public InteractionResult m_6096_(Player player, InteractionHand hand) {
        if (!this.m_9236_().f_46443_ && hand == InteractionHand.MAIN_HAND) {
            ItemStack stack = player.m_21120_(hand);
            if (stack.m_41720_() instanceof AGSAmmoItem) {
                if (!this.hasMagazine()) {
                    int ammo = AGSAmmoItem.getAmmo(stack);
                    this.setHasMagazine(true);
                    this.setAmmoCount(ammo);
                    if (!player.m_7500_()) {
                        stack.m_41774_(1);
                    }
                    this.m_5496_((SoundEvent)ModSounds.M2_LOAD.get(), 1.0f, 1.0f);
                    player.m_5661_((Component)Component.m_237113_((String)("AGS Loaded: " + ammo + " rounds")), true);
                    return InteractionResult.SUCCESS;
                }
                player.m_5661_((Component)Component.m_237113_((String)"Already loaded!"), true);
                return InteractionResult.FAIL;
            }
            if (player.m_6144_() && stack.m_41619_() && this.hasMagazine()) {
                int remaining = this.getAmmoCount();
                ItemStack newBox = new ItemStack((ItemLike)ModItems.AGS_AMMO.get());
                AGSAmmoItem.setAmmo(newBox, remaining);
                player.m_21008_(hand, newBox);
                this.setHasMagazine(false);
                this.setAmmoCount(0);
                this.m_5496_((SoundEvent)ModSounds.M2_UNLOAD.get(), 1.0f, 1.0f);
                player.m_5661_((Component)Component.m_237113_((String)("AGS Unloaded (" + remaining + ")")), true);
                return InteractionResult.SUCCESS;
            }
            if (!player.m_6144_() && this.m_20197_().isEmpty()) {
                float gunFacingYaw = this.m_146908_() + this.getTurretYaw();
                float gunFacingPitch = this.getTurretPitch();
                player.m_146922_(gunFacingYaw);
                player.m_5616_(gunFacingYaw);
                player.m_146926_(gunFacingPitch);
                player.m_20329_((Entity)this);
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    public void m_19956_(Entity passenger, Entity.MoveFunction callback) {
        if (this.m_20363_(passenger)) {
            double[] pos = this.calculateSeatPosition(this.getTurretYaw());
            callback.m_20372_(passenger, pos[0], pos[1], pos[2]);
        }
    }

    private double[] calculateSeatPosition(float turretRelativeYaw) {
        double modelPivotOffsetZ = 0.1875;
        double baseRad = Math.toRadians(this.m_146908_());
        double pivotX = this.m_20185_() - Math.sin(baseRad) * modelPivotOffsetZ;
        double pivotZ = this.m_20189_() + Math.cos(baseRad) * modelPivotOffsetZ;
        double pivotY = this.m_20186_() + (this.isAiming() ? -0.6 : -0.35);
        double seatDistance = 1.1;
        float seatAngle = this.m_146908_() + turretRelativeYaw + 180.0f;
        double rads = Math.toRadians(seatAngle);
        return new double[]{pivotX + -Math.sin(rads) * seatDistance, pivotY, pivotZ + Math.cos(rads) * seatDistance};
    }

    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController[]{new AnimationController((GeoAnimatable)this, "controller", 0, event -> {
            if (this.isFiring() && (this.getAmmoCount() > 0 || !this.hasMagazine()) && this.getAmmoCount() > 0 && this.hasMagazine()) {
                return event.setAndContinue(RawAnimation.begin().thenLoop("animation.ags.fire"));
            }
            return PlayState.STOP;
        })});
    }

    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    protected void m_7378_(CompoundTag tag) {
        if (tag.m_128441_("HasMagazine")) {
            this.setHasMagazine(tag.m_128471_("HasMagazine"));
        }
        if (tag.m_128441_("AmmoCount")) {
            this.setAmmoCount(tag.m_128451_("AmmoCount"));
        }
        if (tag.m_128441_("IsAiming")) {
            this.setAiming(tag.m_128471_("IsAiming"));
        }
    }

    protected void m_7380_(CompoundTag tag) {
        tag.m_128379_("HasMagazine", this.hasMagazine());
        tag.m_128405_("AmmoCount", this.getAmmoCount());
        tag.m_128379_("IsAiming", this.isAiming());
    }

    public Packet<ClientGamePacketListener> m_5654_() {
        return NetworkHooks.getEntitySpawningPacket((Entity)this);
    }

    public boolean m_6087_() {
        return true;
    }
}

