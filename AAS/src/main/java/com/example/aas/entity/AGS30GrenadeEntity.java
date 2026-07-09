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
        this.setOwner((Entity)shooter);
        this.setPos(shooter.getX(), shooter.getEyeY(), shooter.getZ());
        this.originX = this.getX();
        this.originY = this.getY();
        this.originZ = this.getZ();
    }

    public ItemStack getItem() {
        return new ItemStack((ItemLike)ModItems.AGS_PROJECTILE_ITEM.get());
    }

    public void shootFromRotation(Entity shooter, float pitch, float yaw, float roll, float velocity, float inaccuracy) {
        float x = -((float)Math.sin(Math.toRadians(yaw))) * (float)Math.cos(Math.toRadians(pitch));
        float y = -((float)Math.sin(Math.toRadians(pitch)));
        float z = (float)Math.cos(Math.toRadians(yaw)) * (float)Math.cos(Math.toRadians(pitch));
        this.shoot(x, y, z, velocity, inaccuracy);
    }

    public void tick() {
        super.tick();
        if (this.tickCount == 1 && this.originX == 0.0 && this.originY == 0.0 && this.originZ == 0.0) {
            this.originX = this.getX();
            this.originY = this.getY();
            this.originZ = this.getZ();
        }
        if (this.tickCount > 600) {
            this.discard();
            return;
        }
        Vec3 currentPos = this.position();
        Vec3 motion = this.getDeltaMovement();
        Vec3 nextPos = currentPos.add(motion);
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector((Entity)this, x$0 -> this.canHitEntity((Entity)x$0));
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
        }
        if (!this.isRemoved()) {
            this.setPos(nextPos.x, nextPos.y, nextPos.z);
            float drag = 0.99f;
            double gravity = 0.01;
            this.setDeltaMovement(this.getDeltaMovement().scale((double)drag).subtract(0.0, gravity, 0.0));
            if (this.level().isClientSide) {
                this.level().addParticle((ParticleOptions)ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }

    private void explode() {
        if (!this.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel)this.level();
            double distanceSq = this.distanceToSqr(this.originX, this.originY, this.originZ);
            if (distanceSq < 160.0) {
                this.discard();
                return;
            }
            this.spawnShrapnel(serverLevel);
            boolean canDestroy = (Boolean)AASConfig.AGS_PROJECTILE_DESTRUCTION.get();
            Level.ExplosionInteraction interaction = canDestroy ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
            this.level().explode((Entity)this, this.getX(), this.getY(), this.getZ(), 2.0f, interaction);
            this.discard();
        }
    }

    private void spawnShrapnel(ServerLevel level) {
        Vec3 center = this.position().add(0.0, 0.2, 0.0);
        int count = 24;
        for (int i = 0; i < count; ++i) {
            Entity entity;
            double z;
            double y = 1.0 - (double)i / (double)(count - 1) * 2.0;
            double radiusAtY = Math.sqrt(1.0 - y * y);
            double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
            double theta = goldenAngle * (double)i;
            double x = Math.cos(theta) * radiusAtY;
            Vec3 direction = new Vec3(x, y, z = Math.sin(theta) * radiusAtY).normalize();
            Vec3 endPos = center.add(direction.scale(3.5));
            BlockHitResult blockHit = level.clip(new ClipContext(center, endPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, (Entity)this));
            Vec3 finalTargetPos = blockHit.getType() == HitResult.Type.MISS ? endPos : blockHit.getLocation();
            EntityHitResult entityHit = ProjectileUtil.getEntityHitResult((Level)level, (Entity)this, (Vec3)center, (Vec3)finalTargetPos, (AABB)new AABB(center, finalTargetPos).inflate(0.2), e -> e instanceof LivingEntity && !e.isSpectator());
            if (entityHit == null || !((entity = entityHit.getEntity()) instanceof LivingEntity)) continue;
            LivingEntity victim = (LivingEntity)entity;
            victim.hurt(level.damageSources().explosion((Entity)this, this.getOwner()), 20.0f);
            level.sendParticles((ParticleOptions)ParticleTypes.FLASH, entityHit.getLocation().x, entityHit.getLocation().y, entityHit.getLocation().z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(this.level().damageSources().thrown((Entity)this, this.getOwner()), 5.0f);
        this.explode();
    }

    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        this.explode();
    }

    protected void defineSynchedData() {
    }

    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putDouble("OriginX", this.originX);
        tag.putDouble("OriginY", this.originY);
        tag.putDouble("OriginZ", this.originZ);
    }

    protected void readAdditionalSaveData(CompoundTag tag) {
        this.originX = tag.getDouble("OriginX");
        this.originY = tag.getDouble("OriginY");
        this.originZ = tag.getDouble("OriginZ");
    }

    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket((Entity)this);
    }
}

