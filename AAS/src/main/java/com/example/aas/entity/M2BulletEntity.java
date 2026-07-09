/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.particles.ParticleOptions
 *  net.minecraft.core.particles.ParticleTypes
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.network.protocol.Packet
 *  net.minecraft.network.protocol.game.ClientGamePacketListener
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.util.Mth
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.projectile.Projectile
 *  net.minecraft.world.entity.projectile.ProjectileUtil
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.NetworkHooks
 */
package com.example.aas.entity;

import com.example.aas.entity.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class M2BulletEntity
extends Projectile {
    private float damage = 25.0f;

    public M2BulletEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public M2BulletEntity(Level level, LivingEntity shooter) {
        super((EntityType)ModEntities.M2_BULLET.get(), level);
        this.setOwner((Entity)shooter);
        this.setPos(shooter.getX(), shooter.getEyeY() - 0.1, shooter.getZ());
    }

    public void shootFromRotation(Entity shooter, float pitch, float yaw, float roll, float velocity, float inaccuracy) {
        float x = -((float)Math.sin(Math.toRadians(yaw))) * (float)Math.cos(Math.toRadians(pitch));
        float y = -((float)Math.sin(Math.toRadians(pitch)));
        float z = (float)Math.cos(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(pitch));
        this.shoot(x, y, z, velocity, inaccuracy);
    }

    public void tick() {
        EntityHitResult entityHitResult;
        super.tick();
        if (!this.level().isClientSide && this.tickCount > 600) {
            this.discard();
            return;
        }
        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 nextPos = currentPos.add(motion);
        BlockHitResult hitResult = this.level().clip(new ClipContext(currentPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)this));
        if (hitResult.getType() != HitResult.Type.MISS) {
            nextPos = hitResult.getLocation();
        }
        if ((entityHitResult = ProjectileUtil.getEntityHitResult((Level)this.level(), (Entity)this, (Vec3)currentPos, (Vec3)nextPos, (AABB)this.getBoundingBox().expandTowards(motion).inflate(1.5), x$0 -> this.canHitEntity((Entity)x$0))) != null) {
            hitResult = entityHitResult;
        }
        if (hitResult.getType() != HitResult.Type.MISS && !this.isRemoved()) {
            this.onHit((HitResult)hitResult);
        }
        if (!this.isRemoved()) {
            this.setPos(nextPos.x, nextPos.y, nextPos.z);
            this.updateRotation();
            float airDrag = 1.0f;
            double gravity = 0.0;
            this.setDeltaMovement(this.getDeltaMovement().scale((double)airDrag).subtract(0.0, gravity, 0.0));
        }
    }

    protected void updateRotation() {
        Vec3 motion = this.getDeltaMovement();
        double horizontalDistance = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
        float newYRot = (float)(Mth.atan2((double)motion.x, (double)motion.z) * 57.2957763671875);
        float newXRot = (float)(Mth.atan2((double)motion.y, (double)horizontalDistance) * 57.2957763671875);
        this.setYRot(M2BulletEntity.lerpRotation(this.yRotO, newYRot));
        this.setXRot(M2BulletEntity.lerpRotation(this.xRotO, newXRot));
    }

    protected static float lerpRotation(float prevRot, float newRot) {
        while (newRot - prevRot < -180.0f) {
            prevRot -= 360.0f;
        }
        while (newRot - prevRot >= 180.0f) {
            prevRot += 360.0f;
        }
        return Mth.lerp(0.2f, (float)prevRot, (float)newRot);
    }

    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (!this.level().isClientSide) {
            Entity target = result.getEntity();
            Entity owner = this.getOwner();
            target.hurt(this.level().damageSources().mobProjectile((Entity)this, (LivingEntity)owner), this.damage);
        }
        this.discard();
    }

    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (!this.level().isClientSide) {
            ((ServerLevel)this.level()).sendParticles((ParticleOptions)ParticleTypes.POOF, this.getX(), this.getY(), this.getZ(), 5, 0.1, 0.1, 0.1, 0.05);
        }
        this.discard();
    }

    protected void defineSynchedData() {
    }

    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket((Entity)this);
    }
}

