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
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.EntityType
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.projectile.ItemSupplier
 *  net.minecraft.world.entity.projectile.Projectile
 *  net.minecraft.world.entity.projectile.ProjectileUtil
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.ClipContext
 *  net.minecraft.world.level.ClipContext$Block
 *  net.minecraft.world.level.ClipContext$Fluid
 *  net.minecraft.world.level.ItemLike
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.Level$ExplosionInteraction
 *  net.minecraft.world.phys.AABB
 *  net.minecraft.world.phys.BlockHitResult
 *  net.minecraft.world.phys.EntityHitResult
 *  net.minecraft.world.phys.HitResult
 *  net.minecraft.world.phys.HitResult$Type
 *  net.minecraft.world.phys.Vec3
 *  net.minecraftforge.network.NetworkHooks
 */
package com.example.aas.entity;

import com.example.aas.config.AASConfig;
import com.example.aas.entity.ModEntities;
import com.example.aas.item.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class AGS30GrenadeEntity
extends Projectile
implements ItemSupplier {
    private double originX;
    private double originY;
    private double originZ;

    public AGS30GrenadeEntity(EntityType<? extends Projectile> type, Level level) {
        super(type, level);
    }

    public AGS30GrenadeEntity(Level level, LivingEntity shooter) {
        super((EntityType)ModEntities.AGS_30_GRENADE.get(), level);
        this.m_5602_((Entity)shooter);
        this.m_6034_(shooter.m_20185_(), shooter.m_20188_(), shooter.m_20189_());
        this.originX = this.m_20185_();
        this.originY = this.m_20186_();
        this.originZ = this.m_20189_();
    }

    public ItemStack m_7846_() {
        return new ItemStack((ItemLike)ModItems.AGS_PROJECTILE_ITEM.get());
    }

    public void m_37251_(Entity shooter, float pitch, float yaw, float roll, float velocity, float inaccuracy) {
        float x = -((float)Math.sin(Math.toRadians(yaw))) * (float)Math.cos(Math.toRadians(pitch));
        float y = -((float)Math.sin(Math.toRadians(pitch)));
        float z = (float)Math.cos(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(pitch));
        this.m_6686_(x, y, z, velocity, inaccuracy);
    }

    public void m_8119_() {
        super.m_8119_();
        if (this.f_19797_ == 1 && this.originX == 0.0 && this.originY == 0.0 && this.originZ == 0.0) {
            this.originX = this.m_20185_();
            this.originY = this.m_20186_();
            this.originZ = this.m_20189_();
        }
        if (this.f_19797_ > 600) {
            this.m_146870_();
            return;
        }
        Vec3 currentPos = this.m_20182_();
        Vec3 motion = this.m_20184_();
        Vec3 nextPos = currentPos.m_82549_(motion);
        HitResult hitResult = ProjectileUtil.m_278158_((Entity)this, x$0 -> this.m_5603_((Entity)x$0));
        if (hitResult.m_6662_() != HitResult.Type.MISS) {
            this.m_6532_(hitResult);
        }
        if (!this.m_213877_()) {
            this.m_6034_(nextPos.f_82479_, nextPos.f_82480_, nextPos.f_82481_);
            float drag = 0.99f;
            double gravity = 0.01;
            this.m_20256_(this.m_20184_().m_82490_((double)drag).m_82492_(0.0, gravity, 0.0));
            if (this.m_9236_().f_46443_) {
                this.m_9236_().m_7106_((ParticleOptions)ParticleTypes.f_123762_, this.m_20185_(), this.m_20186_(), this.m_20189_(), 0.0, 0.0, 0.0);
            }
        }
    }

    private void explode() {
        if (!this.m_9236_().f_46443_) {
            ServerLevel serverLevel = (ServerLevel)this.m_9236_();
            double distanceSq = this.m_20275_(this.originX, this.originY, this.originZ);
            if (distanceSq < 160.0) {
                this.m_146870_();
                return;
            }
            this.spawnShrapnel(serverLevel);
            boolean canDestroy = (Boolean)AASConfig.AGS_PROJECTILE_DESTRUCTION.get();
            Level.ExplosionInteraction interaction = canDestroy ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
            this.m_9236_().m_254849_((Entity)this, this.m_20185_(), this.m_20186_(), this.m_20189_(), 2.0f, interaction);
            this.m_146870_();
        }
    }

    private void spawnShrapnel(ServerLevel level) {
        Vec3 center = this.m_20182_().m_82520_(0.0, 0.2, 0.0);
        int count = 24;
        for (int i = 0; i < count; ++i) {
            Entity entity;
            double z;
            double y = 1.0 - (double)i / (double)(count - 1) * 2.0;
            double radiusAtY = Math.sqrt(1.0 - y * y);
            double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
            double theta = goldenAngle * (double)i;
            double x = Math.cos(theta) * radiusAtY;
            Vec3 direction = new Vec3(x, y, z = Math.sin(theta) * radiusAtY).m_82541_();
            Vec3 endPos = center.m_82549_(direction.m_82490_(3.5));
            BlockHitResult blockHit = level.m_45547_(new ClipContext(center, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)this));
            Vec3 finalTargetPos = blockHit.m_6662_() == HitResult.Type.MISS ? endPos : blockHit.m_82450_();
            EntityHitResult entityHit = ProjectileUtil.m_37304_((Level)level, (Entity)this, (Vec3)center, (Vec3)finalTargetPos, (AABB)new AABB(center, finalTargetPos).m_82400_(0.2), e -> e instanceof LivingEntity && !e.m_5833_());
            if (entityHit == null || !((entity = entityHit.m_82443_()) instanceof LivingEntity)) continue;
            LivingEntity victim = (LivingEntity)entity;
            victim.m_6469_(level.m_269111_().m_269036_((Entity)this, this.m_19749_()), 20.0f);
            level.m_8767_((ParticleOptions)ParticleTypes.f_123747_, entityHit.m_82450_().f_82479_, entityHit.m_82450_().f_82480_, entityHit.m_82450_().f_82481_, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    protected void m_5790_(EntityHitResult result) {
        super.m_5790_(result);
        result.m_82443_().m_6469_(this.m_9236_().m_269111_().m_269390_((Entity)this, this.m_19749_()), 5.0f);
        this.explode();
    }

    protected void m_8060_(BlockHitResult result) {
        super.m_8060_(result);
        this.explode();
    }

    protected void m_8097_() {
    }

    protected void m_7380_(CompoundTag tag) {
        tag.m_128347_("OriginX", this.originX);
        tag.m_128347_("OriginY", this.originY);
        tag.m_128347_("OriginZ", this.originZ);
    }

    protected void m_7378_(CompoundTag tag) {
        this.originX = tag.m_128459_("OriginX");
        this.originY = tag.m_128459_("OriginY");
        this.originZ = tag.m_128459_("OriginZ");
    }

    public Packet<ClientGamePacketListener> m_5654_() {
        return NetworkHooks.getEntitySpawningPacket((Entity)this);
    }
}

