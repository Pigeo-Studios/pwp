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
        this.m_5602_((Entity)shooter);
        this.m_6034_(shooter.m_20185_(), shooter.m_20188_() - 0.1, shooter.m_20189_());
    }

    public void m_37251_(Entity shooter, float pitch, float yaw, float roll, float velocity, float inaccuracy) {
        float x = -((float)Math.sin(Math.toRadians(yaw))) * (float)Math.cos(Math.toRadians(pitch));
        float y = -((float)Math.sin(Math.toRadians(pitch)));
        float z = (float)Math.cos(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(pitch));
        this.m_6686_(x, y, z, velocity, inaccuracy);
    }

    public void m_8119_() {
        EntityHitResult entityHitResult;
        super.m_8119_();
        if (!this.m_9236_().f_46443_ && this.f_19797_ > 600) {
            this.m_146870_();
            return;
        }
        Vec3 currentPos = this.m_20182_();
        Vec3 motion = this.m_20184_();
        Vec3 nextPos = currentPos.m_82549_(motion);
        BlockHitResult hitResult = this.m_9236_().m_45547_(new ClipContext(currentPos, nextPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)this));
        if (hitResult.m_6662_() != HitResult.Type.MISS) {
            nextPos = hitResult.m_82450_();
        }
        if ((entityHitResult = ProjectileUtil.m_37304_((Level)this.m_9236_(), (Entity)this, (Vec3)currentPos, (Vec3)nextPos, (AABB)this.m_20191_().m_82369_(motion).m_82400_(1.5), x$0 -> this.m_5603_((Entity)x$0))) != null) {
            hitResult = entityHitResult;
        }
        if (hitResult.m_6662_() != HitResult.Type.MISS && !this.m_213877_()) {
            this.m_6532_((HitResult)hitResult);
        }
        if (!this.m_213877_()) {
            this.m_6034_(nextPos.f_82479_, nextPos.f_82480_, nextPos.f_82481_);
            this.m_37283_();
            float airDrag = 1.0f;
            double gravity = 0.0;
            this.m_20256_(this.m_20184_().m_82490_((double)airDrag).m_82492_(0.0, gravity, 0.0));
        }
    }

    protected void m_37283_() {
        Vec3 motion = this.m_20184_();
        double horizontalDistance = Math.sqrt(motion.f_82479_ * motion.f_82479_ + motion.f_82481_ * motion.f_82481_);
        float newYRot = (float)(Mth.m_14136_((double)motion.f_82479_, (double)motion.f_82481_) * 57.2957763671875);
        float newXRot = (float)(Mth.m_14136_((double)motion.f_82480_, (double)horizontalDistance) * 57.2957763671875);
        this.m_146922_(M2BulletEntity.m_37273_(this.f_19859_, newYRot));
        this.m_146926_(M2BulletEntity.m_37273_(this.f_19860_, newXRot));
    }

    protected static float m_37273_(float prevRot, float newRot) {
        while (newRot - prevRot < -180.0f) {
            prevRot -= 360.0f;
        }
        while (newRot - prevRot >= 180.0f) {
            prevRot += 360.0f;
        }
        return Mth.m_14179_((float)0.2f, (float)prevRot, (float)newRot);
    }

    protected void m_5790_(EntityHitResult result) {
        super.m_5790_(result);
        if (!this.m_9236_().f_46443_) {
            Entity target = result.m_82443_();
            Entity owner = this.m_19749_();
            target.m_6469_(this.m_9236_().m_269111_().m_269299_((Entity)this, (LivingEntity)owner), this.damage);
        }
        this.m_146870_();
    }

    protected void m_8060_(BlockHitResult result) {
        super.m_8060_(result);
        if (!this.m_9236_().f_46443_) {
            ((ServerLevel)this.m_9236_()).m_8767_((ParticleOptions)ParticleTypes.f_123759_, this.m_20185_(), this.m_20186_(), this.m_20189_(), 5, 0.1, 0.1, 0.1, 0.05);
        }
        this.m_146870_();
    }

    protected void m_8097_() {
    }

    protected void m_7378_(CompoundTag tag) {
    }

    protected void m_7380_(CompoundTag tag) {
    }

    public Packet<ClientGamePacketListener> m_5654_() {
        return NetworkHooks.getEntitySpawningPacket((Entity)this);
    }
}

