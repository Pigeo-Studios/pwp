package com.pigeostudios.pwp.warfare.entity;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.item.ModItems;
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
import net.minecraft.world.level.ClipContext.Block;
import net.minecraft.world.level.ClipContext.Fluid;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.minecraftforge.network.NetworkHooks;

// Сущность гранаты АГС-30
// Снаряд, выпущенный из гранатомёта, взрывается при попадании
public class AGS30GrenadeEntity extends Projectile implements ItemSupplier {
   private double originX;
   private double originY;
   private double originZ;

   public AGS30GrenadeEntity(EntityType<? extends Projectile> type, Level level) {
      super(type, level);
   }

   public AGS30GrenadeEntity(Level level, LivingEntity shooter) {
      super((EntityType)ModEntities.AGS_30_GRENADE.get(), level);
      this.setOwner(shooter);
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
      } else {
         Vec3 currentPos = this.position();
         Vec3 motion = this.getDeltaMovement();
         Vec3 nextPos = currentPos.add(motion);
         HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, x$0 -> this.canHitEntity(x$0));
         if (hitResult.getType() != Type.MISS) {
            this.onHit(hitResult);
         }

         if (!this.isRemoved()) {
            this.setPos(nextPos.x, nextPos.y, nextPos.z);
            float drag = 0.99F;
            double gravity = 0.01;
            this.setDeltaMovement(this.getDeltaMovement().scale(drag).subtract(0.0, gravity, 0.0));
            if (this.level().isClientSide) {
               this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
         }
      }
   }

   // Взрыв гранаты с осколками
   private void explode() {
      if (!this.level().isClientSide) {
         ServerLevel serverLevel = (ServerLevel)this.level();
         double distanceSq = this.distanceToSqr(this.originX, this.originY, this.originZ);
         if (distanceSq < 160.0) {
            this.discard();
            return;
         }

         this.spawnShrapnel(serverLevel);
         boolean canDestroy = (Boolean)WarfareConfig.AGS_PROJECTILE_DESTRUCTION.get();
         ExplosionInteraction interaction = canDestroy ? ExplosionInteraction.BLOCK : ExplosionInteraction.NONE;
         this.level().explode(this, this.getX(), this.getY(), this.getZ(), 2.0F, interaction);
         this.discard();
      }
   }

   // Создание осколков при взрыве (равномерное распределение по сфере)
   private void spawnShrapnel(ServerLevel level) {
      Vec3 center = this.position().add(0.0, 0.2, 0.0);
      int count = 24;

      for (int i = 0; i < count; i++) {
         double y = 1.0 - (double)i / (count - 1) * 2.0;
         double radiusAtY = Math.sqrt(1.0 - y * y);
         double goldenAngle = Math.PI * (3.0 - Math.sqrt(5.0));
         double theta = goldenAngle * i;
         double x = Math.cos(theta) * radiusAtY;
         double z = Math.sin(theta) * radiusAtY;
         Vec3 direction = new Vec3(x, y, z).normalize();
         Vec3 endPos = center.add(direction.scale(3.5));
         BlockHitResult blockHit = level.clip(new ClipContext(center, endPos, Block.COLLIDER, Fluid.NONE, this));
         Vec3 finalTargetPos = blockHit.getType() == Type.MISS ? endPos : blockHit.getLocation();
         EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
            level, this, center, finalTargetPos, new AABB(center, finalTargetPos).inflate(0.2), e -> e instanceof LivingEntity && !e.isSpectator()
         );
         if (entityHit != null && entityHit.getEntity() instanceof LivingEntity victim) {
            victim.hurt(level.damageSources().explosion(this, this.getOwner()), 20.0F);
            level.sendParticles(
               ParticleTypes.FLASH, entityHit.getLocation().x, entityHit.getLocation().y, entityHit.getLocation().z, 1, 0.0, 0.0, 0.0, 0.0
            );
         }
      }
   }

   protected void onHitEntity(EntityHitResult result) {
      super.onHitEntity(result);
      result.getEntity().hurt(this.level().damageSources().thrown(this, this.getOwner()), 5.0F);
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
      return NetworkHooks.getEntitySpawningPacket(this);
   }
}
