package com.pigeostudios.pwp.warfare.entity;

import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.VehicleSpawnerBlockEntity;
import com.pigeostudios.pwp.warfare.client.ClientHooks;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;

// РЎСѓС‰РЅРѕСЃС‚СЊ СЏС‰РёРєР° СЃРЅР°Р±Р¶РµРЅРёСЏ
// РЎР±СЂР°СЃС‹РІР°РµС‚СЃСЏ СЃ С‚РµС…РЅРёРєРё СЃРЅР°Р±Р¶РµРЅРёСЏ, РїРѕРїРѕР»РЅСЏРµС‚ Р·Р°РїР°СЃС‹ FOB Рё СЂРµРјРѕРЅС‚РёСЂСѓРµС‚ С‚РµС…РЅРёРєСѓ
public class SupplyCrateEntity extends Entity {
   private static final EntityDataAccessor<Integer> MATERIALS = SynchedEntityData.defineId(SupplyCrateEntity.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<String> TEAM_OWNER = SynchedEntityData.defineId(SupplyCrateEntity.class, EntityDataSerializers.STRING);
   private boolean hasResupplied = false;
   private UUID ownerId = null;
   private UUID currentTargetUUID = null;

   public SupplyCrateEntity(EntityType<?> type, Level level) {
      super(type, level);
      this.noCulling = true;
   }

   public SupplyCrateEntity(Level level, double x, double y, double z, String team, UUID ownerId) {
      this((EntityType<?>)ModEntities.SUPPLY_CRATE.get(), level);
      this.setPos(x, y, z);
      this.setTeamOwner(team);
      this.setMaterials((Integer)WarfareConfig.SUPPLY_CRATE_MATERIALS.get());
      this.ownerId = ownerId;
   }

   protected void defineSynchedData() {
      this.entityData.define(MATERIALS, 50);
      this.entityData.define(TEAM_OWNER, "NEUTRAL");
   }

   public int getMaterials() {
      return (Integer)this.entityData.get(MATERIALS);
   }

   public void setMaterials(int amount) {
      this.entityData.set(MATERIALS, amount);
   }

   public String getTeamOwner() {
      return (String)this.entityData.get(TEAM_OWNER);
   }

   public void setTeamOwner(String team) {
      this.entityData.set(TEAM_OWNER, team);
   }

   public boolean isAttackable() {
      return true;
   }

   public boolean hurt(DamageSource source, float amount) {
      if (this.level().isClientSide || this.isRemoved()) {
         return false;
      }

      if (!source.is(DamageTypeTags.IS_EXPLOSION) && !(source.getEntity() instanceof Player)) {
         return false;
      }

      this.destroyCrate();
      return true;
   }

   public void destroyCrate() {
      if (this.level() instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 5, 0.2, 0.2, 0.2, 0.05);
      }

      this.discard();
   }

   public InteractionResult interact(Player player, InteractionHand hand) {
      if (hand != InteractionHand.MAIN_HAND) {
         return InteractionResult.PASS;
      }

      if (this.level().isClientSide) {
         DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHooks.openCrateMenu(this.getId()));
      }

      return InteractionResult.sidedSuccess(this.level().isClientSide);
   }

   public void tick() {
      super.tick();
      if (!this.level().isClientSide && this.getMaterials() <= 0) {
         this.destroyCrate();
      } else {
         if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
         }

         this.move(MoverType.SELF, this.getDeltaMovement());
         float friction = 0.98F;
         if (this.onGround()) {
            friction = 0.7F;
         }

         this.setDeltaMovement(this.getDeltaMovement().scale(friction));
         if (!this.level().isClientSide) {
            if (!this.hasResupplied && this.tickCount > 20 && this.tickCount % 20 == 0 && this.onGround()) {
               this.supplyNearbyHub();
               if (!this.isRemoved()) {
                  this.supplyNearbyVehicle();
               }
            }

            if (this.tickCount > 6000) {
               this.discard();
            }
         }
      }
   }

   // РџРѕРёСЃРє Р±Р»РёР¶Р°Р№С€РµР№ С‚РµС…РЅРёРєРё РґР»СЏ РїРѕРїРѕР»РЅРµРЅРёСЏ Р·Р°РїР°СЃРѕРІ
   private void supplyNearbyVehicle() {
      ServerLevel level = (ServerLevel)this.level();
      if (this.currentTargetUUID != null) {
         Entity target = level.getEntity(this.currentTargetUUID);
         if (target != null && target.isAlive() && !(target.distanceTo(this) > 15.0)) {
            this.processVehicleLogic(target);
         } else {
            if (target != null) {
               target.getPersistentData().putInt("WARFARE_CrateTimer", 0);
            }

            this.currentTargetUUID = null;
         }
      } else {
         AABB searchArea = this.getBoundingBox().inflate(10.0);
         List<Entity> vehicles = level.getEntities(this, searchArea, e -> e.isAlive() && e.getPersistentData().contains("WARFARE_VehicleTeam"));
         vehicles.sort((e1, e2) -> {
            boolean p1 = !e1.getPassengers().isEmpty();
            boolean p2 = !e2.getPassengers().isEmpty();
            if (p1 && !p2) {
               return -1;
            } else {
               return !p1 && p2 ? 1 : 0;
            }
         });

         for (Entity vehicle : vehicles) {
            String vTeam = vehicle.getPersistentData().getString("WARFARE_VehicleTeam");
            if (this.getTeamOwner().equals("NEUTRAL") || vTeam.isEmpty() || vTeam.equalsIgnoreCase(this.getTeamOwner())) {
               boolean needsService = false;
               if (vehicle.getPersistentData().contains("WARFARE_SpawnerPos")) {
                  if (vehicle instanceof LivingEntity living && living.getHealth() < living.getMaxHealth()) {
                     needsService = true;
                  }

                  if (!vehicle.getPersistentData().getBoolean("WARFARE_IsSupplyTruck")) {
                     needsService = true;
                  }
               }

               if (needsService) {
                  this.currentTargetUUID = vehicle.getUUID();
                  vehicle.getPersistentData().putInt("WARFARE_CrateTimer", 0);
                  sendMessageToPassengers(vehicle, "Connecting to Supply Crate...", ChatFormatting.YELLOW);
                  return;
               }
            }
         }
      }
   }

   // РћР±СЂР°Р±РѕС‚РєР° РїРѕРїРѕР»РЅРµРЅРёСЏ РґР»СЏ РєРѕРЅРєСЂРµС‚РЅРѕР№ С‚РµС…РЅРёРєРё
   private void processVehicleLogic(Entity vehicle) {
      int timer = vehicle.getPersistentData().getInt("WARFARE_CrateTimer");
      timer++;
      int timeToWait = 30;
      if (timer >= timeToWait) {
         boolean actionDone = false;
         if (vehicle.getPersistentData().contains("WARFARE_SpawnerPos")) {
            if (vehicle instanceof LivingEntity living) {
               living.setHealth(living.getMaxHealth());
            }

            long spawnerPosLong = vehicle.getPersistentData().getLong("WARFARE_SpawnerPos");
            BlockPos spawnerPos = BlockPos.of(spawnerPosLong);
            if (this.level().isLoaded(spawnerPos) && this.level().getBlockEntity(spawnerPos) instanceof VehicleSpawnerBlockEntity spawner) {
               vehicle.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(vehInv -> {
                  if (vehInv instanceof IItemHandlerModifiable modifiable) {
                     for (int i = 0; i < vehInv.getSlots(); i++) {
                        modifiable.setStackInSlot(i, ItemStack.EMPTY);
                     }
                  }

                  int slotsToCopy = 32;

                  for (int i = 0; i < slotsToCopy && i < vehInv.getSlots(); i++) {
                     ItemStack sourceStack = spawner.inventory.getStackInSlot(i + 1);
                     if (!sourceStack.isEmpty()) {
                        insertItem(vehInv, i, sourceStack.copy());
                     }
                  }

                  Item batteryItem = (Item)ForgeRegistries.ITEMS.getValue(new ResourceLocation("superbwarfare", "large_battery"));
                  if (batteryItem != null) {
                     for (int i = 0; i < vehInv.getSlots(); i++) {
                        if (vehInv.getStackInSlot(i).isEmpty()) {
                           insertItem(vehInv, i, new ItemStack(batteryItem));
                           break;
                        }
                     }
                  }
               });
            }

            vehicle.getPersistentData().putLong("WARFARE_NextSupplyTime", this.level().getGameTime() + 600L);
            sendMessageToPassengers(vehicle, "Vehicle Repaired & Rearmed by Crate!", ChatFormatting.GREEN);
            actionDone = true;
         }

         if (actionDone) {
            if (this.level() instanceof ServerLevel serverLevel) {
               serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, vehicle.getX(), vehicle.getY() + 1.5, vehicle.getZ(), 20, 1.0, 1.0, 1.0, 0.1);
            }

            vehicle.getPersistentData().putInt("WARFARE_CrateTimer", 0);
            this.hasResupplied = true;
            this.destroyCrate();
         } else {
            vehicle.getPersistentData().putInt("WARFARE_CrateTimer", 0);
            this.currentTargetUUID = null;
         }
      } else {
         vehicle.getPersistentData().putInt("WARFARE_CrateTimer", timer);
         sendMessageToPassengers(vehicle, "Resupplying: " + (timeToWait - timer) + "s", ChatFormatting.AQUA);
      }
   }

   private static void insertItem(IItemHandler handler, int slot, ItemStack stack) {
      if (handler instanceof IItemHandlerModifiable modifiable) {
         modifiable.setStackInSlot(slot, stack);
      } else {
         handler.insertItem(slot, stack, false);
      }
   }

   private static void sendMessageToPassengers(Entity vehicle, String msg, ChatFormatting color) {
      for (Entity passenger : vehicle.getPassengers()) {
         if (passenger instanceof Player player) {
            player.displayClientMessage(Component.literal(msg).withStyle(color), true);
         }
      }
   }

   // РџРѕРїРѕР»РЅРµРЅРёРµ РјР°С‚РµСЂРёР°Р»РѕРІ Р±Р»РёР¶Р°Р№С€РµРіРѕ FOB
   private void supplyNearbyHub() {
      if (!this.getTeamOwner().equals("NEUTRAL")) {
         ServerLevel currentLevel = (ServerLevel)this.level();
         WarfareWorldData data = WarfareWorldData.get(currentLevel);
         BlockPos myPos = this.blockPosition();
         double searchRadiusSq = 7500.0;
         String currentDim = currentLevel.dimension().location().toString();

         for (WarfareWorldData.HubInfo hubInfo : data.hubs) {
            if ((hubInfo.dimension == null || hubInfo.dimension.equals(currentDim)) && hubInfo.constructed) {
               BlockPos hubPos = hubInfo.pos;
               if (hubPos.distSqr(myPos) <= searchRadiusSq && currentLevel.isLoaded(hubPos) && currentLevel.getBlockEntity(hubPos) instanceof HubBlockEntity hub) {
                  String hubTeam = hub.getTeam();
                  if (hubTeam.equalsIgnoreCase(this.getTeamOwner()) || hubTeam.equals("NEUTRAL")) {
                     if (hubTeam.equals("NEUTRAL")) {
                        hub.setTeam(this.getTeamOwner());
                        hubInfo.team = this.getTeamOwner();
                        data.setDirty();
                     }

                     hub.addMaterials(this.getMaterials());
                     this.hasResupplied = true;
                     if (this.ownerId != null) {
                        Player player = currentLevel.getPlayerByUUID(this.ownerId);
                        if (player != null) {
                           ChatFormatting color = this.getTeamOwner().equals("BLUE") ? ChatFormatting.BLUE : ChatFormatting.RED;
                           player.displayClientMessage(Component.literal("FOB Resupplied! (+" + this.getMaterials() + " Mats)").withStyle(color), true);
                        }
                     }

                     currentLevel.sendParticles(
                        ParticleTypes.HAPPY_VILLAGER, hubPos.getX() + 0.5, hubPos.getY() + 1.5, hubPos.getZ() + 0.5, 20, 0.5, 0.5, 0.5, 0.1
                     );
                     this.destroyCrate();
                     return;
                  }
               }
            }
         }
      }
   }

   public boolean canBeCollidedWith() {
      return false;
   }

   public boolean isPickable() {
      return true;
   }

   public boolean isPushable() {
      return false;
   }

   protected void readAdditionalSaveData(CompoundTag tag) {
      this.setTeamOwner(tag.getString("Team"));
      this.setMaterials(tag.getInt("Materials"));
      this.hasResupplied = tag.getBoolean("HasResupplied");
      if (tag.hasUUID("Owner")) {
         this.ownerId = tag.getUUID("Owner");
      }

      if (tag.hasUUID("Target")) {
         this.currentTargetUUID = tag.getUUID("Target");
      }
   }

   protected void addAdditionalSaveData(CompoundTag tag) {
      tag.putString("Team", this.getTeamOwner());
      tag.putInt("Materials", this.getMaterials());
      tag.putBoolean("HasResupplied", this.hasResupplied);
      if (this.ownerId != null) {
         tag.putUUID("Owner", this.ownerId);
      }

      if (this.currentTargetUUID != null) {
         tag.putUUID("Target", this.currentTargetUUID);
      }
   }

   public Packet<ClientGamePacketListener> getAddEntityPacket() {
      return NetworkHooks.getEntitySpawningPacket(this);
   }
}
