package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.registries.ForgeRegistries;

// РЎСѓС‰РЅРѕСЃС‚СЊ Р±Р»РѕРєР° РіР»Р°РІРЅРѕРіРѕ СЃРЅР°Р±Р¶РµРЅРёСЏ
// Р›РµС‡РёС‚ РёРіСЂРѕРєРѕРІ, РїРѕРїРѕР»РЅСЏРµС‚ Р±РѕРµРїСЂРёРїР°СЃС‹ РіСЂСѓР·РѕРІРёРєРѕРІ Рё РїРµСЂРµРІРѕРѕСЂСѓР¶Р°РµС‚/СЂРµРјРѕРЅС‚РёСЂСѓРµС‚ С‚РµС…РЅРёРєСѓ
public class MainSupplyBlockEntity extends BlockEntity {
   // РўР°Р№РјРµСЂ РїСЂРѕРІРµСЂРєРё (РєР°Р¶РґС‹Рµ 20 С‚РёРєРѕРІ)
   private int checkTimer = 0;
   // РњР°РєСЃРёРјР°Р»СЊРЅС‹Р№ РїСЂРѕРіСЂРµСЃСЃ СЃС‚СЂРѕРёС‚РµР»СЊСЃС‚РІР° СЃС‚Р°РЅС†РёРё
   public static final int MAX_PROGRESS = 3000;
   private int currentProgress = 0;
   private int activeDiggers = 0;
   private String teamOwner = "NEUTRAL";

   public MainSupplyBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlocks.MAIN_SUPPLY_BE.get(), pos, state);
   }

   public void addProgress() {
      if (this.currentProgress < MAX_PROGRESS) {
         this.activeDiggers++;
      }
   }

   public void addCreativeProgress(int amount) {
      if (this.currentProgress < MAX_PROGRESS) {
         this.currentProgress += amount;
         if (this.currentProgress >= MAX_PROGRESS) {
            this.currentProgress = MAX_PROGRESS;
         }
      }
   }

   public void setTeam(String team) {
      this.teamOwner = team;
      this.setChanged();
      if (this.level != null) {
         this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
      }
   }

   public String getTeam() {
      return this.teamOwner;
   }

   public float getPercentage() {
      boolean constructed = this.level != null && this.level.getBlockState(this.worldPosition).hasProperty(MainSupplyBlock.CONSTRUCTED)
         && (Boolean)this.level.getBlockState(this.worldPosition).getValue(MainSupplyBlock.CONSTRUCTED);
      if (constructed) {
         return 1.0F;
      }
      return this.currentProgress / (float)MAX_PROGRESS;
   }

   // РўРёРє СЃРЅР°Р±Р¶РµРЅРёСЏ: Р»РµС‡РµРЅРёРµ РёРіСЂРѕРєРѕРІ, РїРѕРїРѕР»РЅРµРЅРёРµ РіСЂСѓР·РѕРІРёРєРѕРІ, СЂРµРјРѕРЅС‚ С‚РµС…РЅРёРєРё
   public static void tick(Level level, BlockPos pos, BlockState state, MainSupplyBlockEntity entity) {
      if (!level.isClientSide) {
         if (!(Boolean)state.getValue(MainSupplyBlock.CONSTRUCTED)) {
            entity.tickConstruction(level, pos, state, entity);
            return;
         }

         entity.checkTimer++;
         if (entity.checkTimer >= 20) {
            entity.checkTimer = 0;
            if ((Boolean)WarfareConfig.MAIN_SUPPLY_HEALING.get()) {
               int healRad = (Integer)WarfareConfig.MAIN_SUPPLY_HEAL_RADIUS.get();
               AABB healArea = new AABB(pos).inflate(healRad);

               for (Player p : level.getEntitiesOfClass(Player.class, healArea)) {
                  if (p.isAlive() && !p.isSpectator()) {
                     p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, false, false, true));
                  }
               }
            }

            AABB searchArea = new AABB(pos).inflate(15.0);
            List<Entity> nearbyEntities = level.getEntitiesOfClass(Entity.class, searchArea);
            nearbyEntities.sort((e1, e2) -> {
               boolean p1HasPlayer = e1.getPassengers().stream().anyMatch(px -> px instanceof Player);
               boolean p2HasPlayer = e2.getPassengers().stream().anyMatch(px -> px instanceof Player);
               if (p1HasPlayer && !p2HasPlayer) {
                  return -1;
               } else {
                  return !p1HasPlayer && p2HasPlayer ? 1 : 0;
               }
            });
            long currentTime = level.getGameTime();

            for (Entity vehicle : nearbyEntities) {
               StringBuilder statusMessage = new StringBuilder();
               boolean showActionBar = false;
               long lastSeenTime = vehicle.getPersistentData().getLong("WARFARE_LastSupplyTime");
               if (vehicle.getPersistentData().contains("WARFARE_VehicleMaxMats")) {
                  int maxMats = vehicle.getPersistentData().getInt("WARFARE_VehicleMaxMats");
                  vehicle.getPersistentData().putInt("WARFARE_VehicleMats", maxMats);
               }

               if (currentTime - lastSeenTime > 200L) {
                  vehicle.getPersistentData().putInt("WARFARE_RepairTimer", 0);
                  vehicle.getPersistentData().putInt("WARFARE_TruckReloadTimer", 0);
               }

               vehicle.getPersistentData().putLong("WARFARE_LastSupplyTime", currentTime);
               if (vehicle.getPersistentData().getBoolean("WARFARE_IsSupplyTruck")) {
                  int currentAmmo = vehicle.getPersistentData().getInt("WARFARE_SupplyAmmo");
                  int maxCrates = (Integer)WarfareConfig.SUPPLY_TRUCK_CRATES.get();
                  if (currentAmmo < maxCrates) {
                     int supplyTimer = vehicle.getPersistentData().getInt("WARFARE_TruckReloadTimer");
                     if (++supplyTimer >= 15) {
                        vehicle.getPersistentData().putInt("WARFARE_SupplyAmmo", currentAmmo + 1);
                        vehicle.getPersistentData().putInt("WARFARE_TruckReloadTimer", 0);
                        sendChatMessageToPassengers(vehicle, "[Supply] +1 Crate Loaded (" + (currentAmmo + 1) + "/" + maxCrates + ")", ChatFormatting.GOLD);
                        spawnEffects(level, vehicle);
                     } else {
                        vehicle.getPersistentData().putInt("WARFARE_TruckReloadTimer", supplyTimer);
                        statusMessage.append(ChatFormatting.YELLOW).append("Loading Crate: ").append(15 - supplyTimer).append("s  ");
                        showActionBar = true;
                     }
                  } else {
                     vehicle.getPersistentData().putInt("WARFARE_TruckReloadTimer", 0);
                  }
               }

               if (vehicle.getPersistentData().contains("WARFARE_SpawnerPos")) {
                  long nextSupplyTime = vehicle.getPersistentData().getLong("WARFARE_NextSupplyTime");
                  if (currentTime < nextSupplyTime) {
                     long secondsLeft = (nextSupplyTime - currentTime) / 20L;
                     vehicle.getPersistentData().putInt("WARFARE_RepairTimer", 0);
                     statusMessage.append(ChatFormatting.RED).append("Rearm Cooldown: ").append(secondsLeft).append("s");
                     showActionBar = true;
                  } else {
                     int repairTimer = vehicle.getPersistentData().getInt("WARFARE_RepairTimer");
                     if (++repairTimer >= 30) {
                        if (vehicle instanceof LivingEntity living) {
                           living.setHealth(living.getMaxHealth());
                        }

                        long spawnerPosLong = vehicle.getPersistentData().getLong("WARFARE_SpawnerPos");
                        BlockPos spawnerPos = BlockPos.of(spawnerPosLong);
                        if (level.isLoaded(spawnerPos) && level.getBlockEntity(spawnerPos) instanceof VehicleSpawnerBlockEntity spawner) {
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

                        vehicle.getPersistentData().putInt("WARFARE_RepairTimer", 0);
                        vehicle.getPersistentData().putLong("WARFARE_NextSupplyTime", currentTime + 600L);
                        sendChatMessageToPassengers(vehicle, "[Base] Vehicle Fully Rearmed & Repaired!", ChatFormatting.GREEN);
                        spawnEffects(level, vehicle);
                     } else {
                        vehicle.getPersistentData().putInt("WARFARE_RepairTimer", repairTimer);
                        statusMessage.append(ChatFormatting.AQUA).append("Rearming: ").append(30 - repairTimer).append("s");
                        showActionBar = true;
                     }
                  }
               }

               if (showActionBar && statusMessage.length() > 0) {
                  for (Entity passenger : vehicle.getPassengers()) {
                     if (passenger instanceof Player player) {
                        player.displayClientMessage(Component.literal(statusMessage.toString()), true);
                     }
                  }
               }
            }
         }
      }
   }

   private static void insertItem(IItemHandler handler, int slot, ItemStack stack) {
      if (handler instanceof IItemHandlerModifiable modifiable) {
         modifiable.setStackInSlot(slot, stack);
      } else {
         handler.insertItem(slot, stack, false);
      }
   }

   private static void sendActionBarToPassengers(Entity vehicle, String msg, ChatFormatting color) {
      for (Entity passenger : vehicle.getPassengers()) {
         if (passenger instanceof Player player) {
            player.displayClientMessage(Component.literal(msg).withStyle(color), true);
         }
      }
   }

   // РЎС‚СЂРѕРёС‚РµР»СЊСЃС‚РІРѕ СЃС‚Р°РЅС†РёРё: РїСЂРѕРіСЂРµСЃСЃ РєРѕРїР°РЅРёСЏ Р»РѕРїР°С‚РѕР№ СЃРѕ СЃС‚Р°РґРёСЏРјРё
   private static void tickConstruction(Level level, BlockPos pos, BlockState state, MainSupplyBlockEntity entity) {
      if (state.getValue(MainSupplyBlock.BUILD_STAGE) == 0) {
         level.setBlock(pos, (BlockState)state.setValue(MainSupplyBlock.BUILD_STAGE, 1), 3);
      }

      if (entity.activeDiggers > 0 || entity.currentProgress > 0) {
         if (entity.activeDiggers > 0) {
            float speed = entity.activeDiggers == 1 ? 1.0F : (entity.activeDiggers == 2 ? 1.34F : (entity.activeDiggers == 3 ? 2.0F : 4.0F));
            float multiplier = ((Double)WarfareConfig.DIGGING_SPEED_MULTIPLIER.get()).floatValue();
            speed *= multiplier;
            entity.currentProgress = entity.currentProgress + (int)Math.ceil(speed);
         }

         if (entity.currentProgress >= MAX_PROGRESS) {
            entity.currentProgress = MAX_PROGRESS;
            level.setBlock(pos, (BlockState)((BlockState)state.setValue(MainSupplyBlock.CONSTRUCTED, true)).setValue(MainSupplyBlock.BUILD_STAGE, 2), 3);
            if (!level.isClientSide) {
               ((ServerLevel)level).sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 40, 1.0, 0.5, 1.0, 0.05);
            }
         } else {
            int newStage = entity.currentProgress >= MAX_PROGRESS / 2 ? 2 : 1;
            if (state.getValue(MainSupplyBlock.BUILD_STAGE) != newStage) {
               level.setBlock(pos, (BlockState)state.setValue(MainSupplyBlock.BUILD_STAGE, newStage), 3);
            }
         }

         if (level.getGameTime() % 5L == 0L || entity.currentProgress >= MAX_PROGRESS) {
            level.sendBlockUpdated(pos, state, state, 3);
         }
      }

      entity.activeDiggers = 0;
   }

   private static void sendChatMessageToPassengers(Entity vehicle, String msg, ChatFormatting color) {
      for (Entity passenger : vehicle.getPassengers()) {
         if (passenger instanceof Player player) {
            player.displayClientMessage(Component.literal(msg).withStyle(color), false);
         }
      }
   }

   // РЎРїР°СѓРЅРёС‚ С‡Р°СЃС‚РёС†С‹ СЌС„С„РµРєС‚Р° РїРѕРїРѕР»РЅРµРЅРёСЏ
   private static void spawnEffects(Level level, Entity vehicle) {
      if (level instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, vehicle.getX(), vehicle.getY() + 1.5, vehicle.getZ(), 10, 1.0, 1.0, 1.0, 0.1);
      }
   }

   protected void saveAdditional(CompoundTag tag) {
      super.saveAdditional(tag);
      tag.putInt("BuildProgress", this.currentProgress);
      tag.putString("TeamOwner", this.teamOwner);
   }

   public void load(CompoundTag tag) {
      super.load(tag);
      this.currentProgress = tag.getInt("BuildProgress");
      if (tag.contains("TeamOwner")) {
         this.teamOwner = tag.getString("TeamOwner");
      }
   }

   public CompoundTag getUpdateTag() {
      CompoundTag tag = new CompoundTag();
      this.saveAdditional(tag);
      return tag;
   }

   public Packet<ClientGamePacketListener> getUpdatePacket() {
      return ClientboundBlockEntityDataPacket.create(this);
   }
}
