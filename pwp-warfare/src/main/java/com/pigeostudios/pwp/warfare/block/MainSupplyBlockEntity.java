package com.pigeostudios.pwp.warfare.block;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
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

// ╨а╨О╨б╤У╨бтА░╨а╨Е╨а╤Х╨б╨Г╨бтАЪ╨б╨К ╨а┬▒╨а┬╗╨а╤Х╨а╤Ф╨а┬░ ╨а╤Ц╨а┬╗╨а┬░╨а╨Ж╨а╨Е╨а╤Х╨а╤Ц╨а╤Х ╨б╨Г╨а╨Е╨а┬░╨а┬▒╨а┬╢╨а┬╡╨а╨Е╨а╤С╨б╨П
// ╨атА║╨а┬╡╨бтАб╨а╤С╨бтАЪ ╨а╤С╨а╤Ц╨б╨В╨а╤Х╨а╤Ф╨а╤Х╨а╨Ж, ╨а╤Ч╨а╤Х╨а╤Ч╨а╤Х╨а┬╗╨а╨Е╨б╨П╨а┬╡╨бтАЪ ╨а┬▒╨а╤Х╨а┬╡╨а╤Ч╨б╨В╨а╤С╨а╤Ч╨а┬░╨б╨Г╨бтА╣ ╨а╤Ц╨б╨В╨б╤У╨а┬╖╨а╤Х╨а╨Ж╨а╤С╨а╤Ф╨а╤Х╨а╨Ж ╨а╤С ╨а╤Ч╨а┬╡╨б╨В╨а┬╡╨а╨Ж╨а╤Х╨а╤Х╨б╨В╨б╤У╨а┬╢╨а┬░╨а┬╡╨бтАЪ/╨б╨В╨а┬╡╨а╤Ш╨а╤Х╨а╨Е╨бтАЪ╨а╤С╨б╨В╨б╤У╨а┬╡╨бтАЪ ╨бтАЪ╨а┬╡╨бтАж╨а╨Е╨а╤С╨а╤Ф╨б╤У
public class MainSupplyBlockEntity extends BlockEntity {
   // ╨а╤Ю╨а┬░╨атДЦ╨а╤Ш╨а┬╡╨б╨В ╨а╤Ч╨б╨В╨а╤Х╨а╨Ж╨а┬╡╨б╨В╨а╤Ф╨а╤С (╨а╤Ф╨а┬░╨а┬╢╨а╥С╨бтА╣╨а┬╡ 20 ╨бтАЪ╨а╤С╨а╤Ф╨а╤Х╨а╨Ж)
   private int checkTimer = 0;

   public MainSupplyBlockEntity(BlockPos pos, BlockState state) {
      super((BlockEntityType)ModBlocks.MAIN_SUPPLY_BE.get(), pos, state);
   }

   // ╨а╤Ю╨а╤С╨а╤Ф ╨б╨Г╨а╨Е╨а┬░╨а┬▒╨а┬╢╨а┬╡╨а╨Е╨а╤С╨б╨П: ╨а┬╗╨а┬╡╨бтАб╨а┬╡╨а╨Е╨а╤С╨а┬╡ ╨а╤С╨а╤Ц╨б╨В╨а╤Х╨а╤Ф╨а╤Х╨а╨Ж, ╨а╤Ч╨а╤Х╨а╤Ч╨а╤Х╨а┬╗╨а╨Е╨а┬╡╨а╨Е╨а╤С╨а┬╡ ╨а╤Ц╨б╨В╨б╤У╨а┬╖╨а╤Х╨а╨Ж╨а╤С╨а╤Ф╨а╤Х╨а╨Ж, ╨б╨В╨а┬╡╨а╤Ш╨а╤Х╨а╨Е╨бтАЪ ╨бтАЪ╨а┬╡╨бтАж╨а╨Е╨а╤С╨а╤Ф╨а╤С
   public static void tick(Level level, BlockPos pos, BlockState state, MainSupplyBlockEntity entity) {
      if (!level.isClientSide) {
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

   private static void sendChatMessageToPassengers(Entity vehicle, String msg, ChatFormatting color) {
      for (Entity passenger : vehicle.getPassengers()) {
         if (passenger instanceof Player player) {
            player.displayClientMessage(Component.literal(msg).withStyle(color), false);
         }
      }
   }

   // ╨а╨О╨а╤Ч╨а┬░╨б╤У╨а╨Е╨а╤С╨бтАЪ ╨бтАб╨а┬░╨б╨Г╨бтАЪ╨а╤С╨бтАа╨бтА╣ ╨б╨М╨бтАЮ╨бтАЮ╨а┬╡╨а╤Ф╨бтАЪ╨а┬░ ╨а╤Ч╨а╤Х╨а╤Ч╨а╤Х╨а┬╗╨а╨Е╨а┬╡╨а╨Е╨а╤С╨б╨П
   private static void spawnEffects(Level level, Entity vehicle) {
      if (level instanceof ServerLevel serverLevel) {
         serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, vehicle.getX(), vehicle.getY() + 1.5, vehicle.getZ(), 10, 1.0, 1.0, 1.0, 0.1);
      }
   }
}
