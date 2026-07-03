package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.block.AGSConstructionBlock;
import com.pigeostudios.pwp.warfare.block.AGSConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.HubBlockEntity;
import com.pigeostudios.pwp.warfare.block.M2ConstructionBlock;
import com.pigeostudios.pwp.warfare.block.M2ConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.ModBlocks;
import com.pigeostudios.pwp.warfare.block.MortarConstructionBlock;
import com.pigeostudios.pwp.warfare.block.MortarConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.block.RallyPointBlockEntity;
import com.pigeostudios.pwp.warfare.block.TOWConstructionBlock;
import com.pigeostudios.pwp.warfare.block.TOWConstructionBlockEntity;
import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

// РџР°РєРµС‚ РґРµР№СЃС‚РІРёР№ СЃ СЂР°РґРёРѕСЃС‚Р°РЅС†РёРµР№: СѓСЃС‚Р°РЅРѕРІРєР° С‚РѕС‡РєРё СЃР±РѕСЂР°, СЃС‚СЂРѕРёС‚РµР»СЊСЃС‚РІРѕ С…Р°Р±РѕРІ Рё С‚РµС…РЅРёРєРё
// РћС‚РїСЂР°РІР»СЏРµС‚СЃСЏ Р»РёРґРµСЂР°РјРё РѕС‚СЂСЏРґРѕРІ РїСЂРё РёСЃРїРѕР»СЊР·РѕРІР°РЅРёРё СЂР°РґРёРѕ
public class PacketRadioAction {
   private final int actionId;

   public PacketRadioAction(int actionId) {
      this.actionId = actionId;
   }

   public static void encode(PacketRadioAction msg, FriendlyByteBuf buf) {
      buf.writeInt(msg.actionId);
   }

   public static PacketRadioAction decode(FriendlyByteBuf buf) {
      return new PacketRadioAction(buf.readInt());
   }

   // РћР±СЂР°Р±Р°С‚С‹РІР°РµС‚ РґРµР№СЃС‚РІРёРµ СЂР°РґРёРѕ: actionId 0 - С‚РѕС‡РєР° СЃР±РѕСЂР°, РѕСЃС‚Р°Р»СЊРЅС‹Рµ - СЃС‚СЂРѕРёС‚РµР»СЊСЃС‚РІРѕ
   public static void handle(PacketRadioAction msg, Supplier<Context> ctx) {
      ctx.get().enqueueWork(() -> {
         ServerPlayer player = ctx.get().getSender();
         if (player != null) {
            ServerLevel currentLevel = player.serverLevel();
            WarfareWorldData data = WarfareWorldData.get(currentLevel);
            String pName = player.getScoreboardName();
            WarfareWorldData.Squad playerSquad = null;
            boolean isLeader = false;

            for (WarfareWorldData.Squad s : data.squads) {
               if (s.members.contains(pName)) {
                  playerSquad = s;
                  if (s.leader.equals(pName)) {
                     isLeader = true;
                  }
                  break;
               }
            }

            if (!player.isCreative()) {
               if (player.getTeam() == null) {
                  player.sendSystemMessage(Component.literal("Access Denied: You must be in a TEAM!").withStyle(ChatFormatting.RED));
                  return;
               }

               if (playerSquad == null) {
                  player.sendSystemMessage(Component.literal("Access Denied: You must be in a SQUAD in this world!").withStyle(ChatFormatting.RED));
                  return;
               }

               if (!isLeader) {
                  player.sendSystemMessage(Component.literal("Access Denied: You must be a Squad Leader!").withStyle(ChatFormatting.RED));
                  return;
               }
            }

            ItemStack stack = player.getMainHandItem();
            long currentGameTime = currentLevel.getGameTime();
            if (msg.actionId == 0) {
               long lastUse = playerSquad.nextRallyAvailableTick;
               if (currentGameTime < lastUse && !player.isCreative()) {
                  long timeLeft = (lastUse - currentGameTime) / 20L;
                  player.sendSystemMessage(Component.literal("Rally Point Cooldown: " + timeLeft + "s").withStyle(ChatFormatting.RED));
                  return;
               }

               String team = "NEUTRAL";
               if (player.getTeam() != null) {
                  String rawTeamName = player.getTeam().getName();
                  if (rawTeamName.equalsIgnoreCase("Blue")) {
                     team = "BLUE";
                  } else if (rawTeamName.equalsIgnoreCase("Red")) {
                     team = "RED";
                  }
               }

               if (team.equals("NEUTRAL") && player.isCreative()) {
                  team = "BLUE";
               }

               if (trySpawnRally(player, currentLevel, team, playerSquad, data)) {
                  playerSquad.nextRallyAvailableTick = currentGameTime + 6000L;
                  player.sendSystemMessage(Component.literal("Squad Rally Point Deployed!").withStyle(ChatFormatting.GREEN));
               } else {
                  playerSquad.nextRallyAvailableTick = currentGameTime + 300L;
               }

               data.setDirty();
               PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(player.level()::dimension), new PacketSyncSquads(data.squads));
            } else {
               handleConstructionLogic(msg.actionId, player, currentLevel, data);
            }
         }
      });
      ctx.get().setPacketHandled(true);
   }

   // РћР±СЂР°Р±РѕС‚РєР° СЃС‚СЂРѕРёС‚РµР»СЊСЃС‚РІР°: С…Р°Р± (actionId=14) Рё С‡РµСЂС‚РµР¶Рё С‚РµС…РЅРёРєРё (20-23)
   private static void handleConstructionLogic(int actionId, ServerPlayer player, ServerLevel level, WarfareWorldData data) {
      BlockPos targetPos = player.blockPosition();
      if (!level.getBlockState(targetPos).canBeReplaced() && actionId != 14) {
         targetPos = targetPos.relative(player.getDirection());
      }

      if (actionId == 14) {
         String playerTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "NEUTRAL";
         long hubCount = data.hubs.stream().filter(h -> h.team.equalsIgnoreCase(playerTeam)).count();
         if (hubCount >= ((Integer)WarfareConfig.MAX_HUBS_PER_TEAM.get()).intValue() && !player.isCreative()) {
            player.sendSystemMessage(Component.literal("FOB Limit Reached for this world!").withStyle(ChatFormatting.RED));
            return;
         }

         for (WarfareWorldData.HubInfo existingHub : data.hubs) {
            if (existingHub.team.equalsIgnoreCase(playerTeam)
               && existingHub.pos.distSqr(targetPos) < (Integer)WarfareConfig.MIN_HUB_DISTANCE.get() * (Integer)WarfareConfig.MIN_HUB_DISTANCE.get()) {
               player.sendSystemMessage(Component.literal("Too close to friendly FOB!").withStyle(ChatFormatting.RED));
               return;
            }
         }

         if ((Boolean)WarfareConfig.HUB_PLACEMENT_REQUIRES_CRATE.get() && !player.isCreative()) {
            double crateCheckRad = 50.0;
            AABB area = new AABB(targetPos).inflate(crateCheckRad);
            List<SupplyCrateEntity> crates = level.getEntitiesOfClass(SupplyCrateEntity.class, area);
            SupplyCrateEntity targetCrate = crates.stream()
               .filter(c -> c.getTeamOwner().equals("NEUTRAL") || c.getTeamOwner().equalsIgnoreCase(playerTeam))
               .findFirst()
               .orElse(null);
            if (targetCrate == null) {
               player.sendSystemMessage(Component.literal("FOB placement requires a Supply Crate within 50 blocks!").withStyle(ChatFormatting.RED));
               return;
            }

            targetCrate.discard();
            player.sendSystemMessage(Component.literal("Supply Crate consumed for FOB placement.").withStyle(ChatFormatting.YELLOW));
         }

         level.setBlock(targetPos, ((Block)ModBlocks.HUB_BLOCK.get()).defaultBlockState(), 3);
         if (level.getBlockEntity(targetPos) instanceof HubBlockEntity hubEntity) {
            hubEntity.setTeam(playerTeam);
            level.sendBlockUpdated(targetPos, level.getBlockState(targetPos), level.getBlockState(targetPos), 3);
         }

         data.hubs.add(new WarfareWorldData.HubInfo(targetPos, playerTeam, false, level.dimension().location().toString()));
         data.setDirty();
         PacketHandler.sendToAllClients(level, data);
         player.sendSystemMessage(Component.literal("FOB Blueprint placed!").withStyle(ChatFormatting.GREEN));
      } else if (actionId == 22) {
         placeBlueprint(
            level,
            targetPos,
            player,
            (BlockState)((Block)ModBlocks.MORTAR_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue(MortarConstructionBlock.FACING, player.getDirection().getOpposite()),
            "Mortar"
         );
      } else if (actionId == 23) {
         placeBlueprint(
            level,
            targetPos,
            player,
            (BlockState)((Block)ModBlocks.TOW_CONSTRUCTION_BLOCK.get()).defaultBlockState().setValue(TOWConstructionBlock.FACING, player.getDirection().getOpposite()),
            "TOW"
         );
      }
   }

   private static void placeBlueprint(ServerLevel level, BlockPos pos, ServerPlayer player, BlockState state, String name) {
      if (level.getBlockState(pos).isAir() || level.getBlockState(pos).canBeReplaced()) {
         level.setBlock(pos, state, 3);
         BlockEntity be = level.getBlockEntity(pos);
         String pTeam = player.getTeam() != null ? player.getTeam().getName() : "NEUTRAL";
         if (be instanceof M2ConstructionBlockEntity m2) {
            m2.setTeam(pTeam);
         } else if (be instanceof AGSConstructionBlockEntity ags) {
            ags.setTeam(pTeam);
         } else if (be instanceof MortarConstructionBlockEntity mortar) {
            mortar.setTeam(pTeam);
         } else if (be instanceof TOWConstructionBlockEntity tow) {
            tow.setTeam(pTeam);
         }

         player.sendSystemMessage(Component.literal(name + " Blueprint placed.").withStyle(ChatFormatting.GREEN));
      }
   }

   // РџС‹С‚Р°РµС‚СЃСЏ СЂР°Р·РјРµСЃС‚РёС‚СЊ С‚РѕС‡РєСѓ СЃР±РѕСЂР° РѕС‚СЂСЏРґР°: РїСЂРѕРІРµСЂСЏРµС‚ РґРёСЃС‚Р°РЅС†РёСЋ РґРѕ С‚РѕС‡РµРє,
   // РЅР°Р»РёС‡РёРµ РІСЂР°РіРѕРІ Рё СЃРѕСЋР·РЅРёРєРѕРІ РїРѕР±Р»РёР·РѕСЃС‚Рё
   private static boolean trySpawnRally(ServerPlayer player, ServerLevel level, String team, WarfareWorldData.Squad squad, WarfareWorldData data) {
      BlockPos pos = player.blockPosition();

      for (WarfareWorldData.CapturePoint point : data.capturePoints) {
         Vec3 center = point.area.getCenter();
         if (pos.distToCenterSqr(center.x, center.y, center.z)
            < (Integer)WarfareConfig.MIN_RALLY_POINT_DISTANCE.get() * (Integer)WarfareConfig.MIN_RALLY_POINT_DISTANCE.get()) {
            player.sendSystemMessage(Component.literal("Too close to Capture Point!").withStyle(ChatFormatting.RED));
            return false;
         }
      }

      int checkRadius = (Integer)WarfareConfig.RALLY_BLOCK_RADIUS.get();
      AABB enemyBox = new AABB(pos).inflate(checkRadius);

      for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, enemyBox)) {
         if (!p.isSpectator() && p.getTeam() != null && !p.getTeam().getName().equalsIgnoreCase(team)) {
            player.sendSystemMessage(Component.literal("Enemies nearby! Cannot deploy Rally.").withStyle(ChatFormatting.RED));
            return false;
         }
      }

      int playerSquadId = player.getPersistentData().getInt("WARFARE_SquadID");
      AABB squadBox = new AABB(pos).inflate(5.0);
      int squadMatesNearby = 0;

      for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, squadBox)) {
         if (p != player && !p.isSpectator()) {
            int otherSquadId = p.getPersistentData().getInt("WARFARE_SquadID");
            if (playerSquadId != 0 && otherSquadId == playerSquadId) {
               squadMatesNearby++;
            }
         }
      }

      if (squadMatesNearby < 1 && !player.isCreative()) {
         player.sendSystemMessage(Component.literal("Need at least 1 SQUAD MEMBER nearby!").withStyle(ChatFormatting.RED));
         return false;
      }

      if (squad.rallyPos != null && level.isLoaded(squad.rallyPos)) {
         if (level.getBlockEntity(squad.rallyPos) instanceof RallyPointBlockEntity rbe) {
            rbe.isDecay = true;
         }

         level.removeBlock(squad.rallyPos, false);
      }

      BlockState rallyState = team.equals("BLUE") ? ((Block)ModBlocks.BLUE_RALLY_BLOCK.get()).defaultBlockState() : ((Block)ModBlocks.RED_RALLY_BLOCK.get()).defaultBlockState();
      level.setBlock(pos, rallyState, 3);
      BlockEntity be = level.getBlockEntity(pos);
      if (be instanceof RallyPointBlockEntity rbe) {
         rbe.setSquadId(squad.id);
      }

      squad.rallyPos = pos;
      squad.rallyDimension = level.dimension().location().toString();
      squad.rallyExpiryTick = level.getGameTime() + 12000L;
      if (be instanceof RallyPointBlockEntity rbe) {
         rbe.setExpiryTick(squad.rallyExpiryTick);
      }

      player.sendSystemMessage(Component.literal("Squad Rally Point Deployed!").withStyle(ChatFormatting.GREEN));
      if (team.equals("BLUE")) {
         data.blueRallies.add(pos);
      } else {
         data.redRallies.add(pos);
      }

      data.setDirty();
      PacketHandler.sendToAllClients(level, data);
      PacketHandler.INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), new PacketSyncSquads(data.squads));
      return true;
   }
}
