package com.pigeostudios.pwp.warfare.voicechat;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRadioVoiceActivity;
import com.pigeostudios.pwp.warfare.network.PacketVoiceActivity;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.Group.Type;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

@ForgeVoicechatPlugin
// РџР»Р°РіРёРЅ РіРѕР»РѕСЃРѕРІРѕРіРѕ С‡Р°С‚Р° РґР»СЏ Simple Voice Chat
// РРЅС‚РµРіСЂР°С†РёСЏ РѕС‚СЂСЏРґРЅРѕР№ СЃРІСЏР·Рё Рё СЂР°С†РёР№ С‡РµСЂРµР· РіРѕР»РѕСЃРѕРІРѕР№ С‡Р°С‚
public class WarfareVoicechatPlugin implements VoicechatPlugin {
   private static VoicechatServerApi serverApi;
   private static final Map<Integer, Group> squadToGroupMap = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastVoiceActivity = new ConcurrentHashMap<>();
   private static final String WALKIETALKIE_NAMESPACE = "walkietalkie";
   private static final String TAG_ACTIVATE = "walkietalkie.activate";
   private static final String TAG_CANAL = "walkietalkie.canal";
   private static final String TAG_MUTE = "walkietalkie.mute";

   public String getPluginId() {
      return "WARFARE_voicechat";
   }

   public void initialize(VoicechatApi api) {
   }

   public void registerEvents(EventRegistration registration) {
      registration.registerEvent(VoicechatServerStartedEvent.class, this::onServerStarted);
      registration.registerEvent(MicrophonePacketEvent.class, this::onMicPacket);
      registration.registerEvent(PlayerConnectedEvent.class, this::onPlayerConnectedVoice);
   }

   private void onServerStarted(VoicechatServerStartedEvent event) {
      serverApi = event.getVoicechat();
   }

   private void onMicPacket(MicrophonePacketEvent event) {
      try {
         if (serverApi == null || ServerLifecycleHooks.getCurrentServer() == null) {
            return;
         }

         UUID playerUuid = event.getSenderConnection().getPlayer().getUuid();
         long now = System.currentTimeMillis();
         if (now - lastVoiceActivity.getOrDefault(playerUuid, 0L) > 150L) {
            lastVoiceActivity.put(playerUuid, now);
            ServerPlayer sender = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUuid);
            if (sender == null) {
               return;
            }

            String pName = sender.getScoreboardName();
            Integer talkerCanal = this.getActiveWalkieTalkieCanal(sender);
            if (talkerCanal != null) {
               PacketRadioVoiceActivity radioPacket = new PacketRadioVoiceActivity(pName);

               for (ServerPlayer p : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                  if (!p.getUUID().equals(sender.getUUID()) && this.playerHasWalkieTalkieOnCanal(p, talkerCanal)) {
                     PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), radioPacket);
                  }
               }
            }

            WarfareWorldData data = WarfareWorldData.get(sender.serverLevel());

            for (WarfareWorldData.Squad s : data.squads) {
               if (s.members.contains(pName)) {
                  PacketVoiceActivity packet = new PacketVoiceActivity(pName);

                  for (String member : s.members) {
                     ServerPlayer p = sender.server.getPlayerList().getPlayerByName(member);
                     if (p != null) {
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), packet);
                     }
                  }
                  break;
               }
            }
         }
      } catch (Exception e) {
         System.err.println("[PWP Warfare] onMicPacket failed: " + e);
      }
   }

   private Integer getActiveWalkieTalkieCanal(ServerPlayer player) {
      try {
         Integer canal = this.getActiveCanalFromStack(player.getMainHandItem());
         return canal != null ? canal : this.getActiveCanalFromStack(player.getOffhandItem());
      } catch (Exception e) {
         return null;
      }
   }

   private Integer getActiveCanalFromStack(ItemStack stack) {
      if (stack == null || stack.isEmpty()) {
         return null;
      }

      if (!this.isWalkieTalkieStack(stack)) {
         return null;
      }

      CompoundTag tag = stack.getTag();
      if (tag == null) {
         return null;
      }

      boolean activated = tag.getBoolean("walkietalkie.activate");
      boolean muted = tag.getBoolean("walkietalkie.mute");
      return activated && !muted ? tag.getInt("walkietalkie.canal") : null;
   }

   private boolean playerHasWalkieTalkieOnCanal(ServerPlayer player, int canal) {
      try {
         for (ItemStack stack : player.getInventory().items) {
            if (this.matchesCanal(stack, canal)) {
               return true;
            }
         }

         return this.matchesCanal(player.getOffhandItem(), canal);
      } catch (Exception e) {
         return false;
      }
   }

   private boolean matchesCanal(ItemStack stack, int canal) {
      if (stack != null && !stack.isEmpty() && this.isWalkieTalkieStack(stack)) {
         CompoundTag tag = stack.getTag();
         return tag == null ? false : tag.getInt("walkietalkie.canal") == canal;
      } else {
         return false;
      }
   }

   private boolean isWalkieTalkieStack(ItemStack stack) {
      if (stack != null && !stack.isEmpty()) {
         ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
         return id != null && id.getNamespace().equals("walkietalkie");
      } else {
         return false;
      }
   }

   private void onPlayerConnectedVoice(PlayerConnectedEvent event) {
      try {
         if (serverApi == null || ServerLifecycleHooks.getCurrentServer() == null) {
            return;
         }

         UUID playerUuid = event.getConnection().getPlayer().getUuid();
         ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUuid);
         if (player == null) {
            return;
         }

         int squadId = player.getPersistentData().getInt("WARFARE_SquadID");
         if (squadId > 0) {
            joinGroup(player, squadId);
         }
      } catch (Exception e) {
         System.err.println("[PWP Warfare] onPlayerConnectedVoice failed: " + e);
      }
   }

   // РЎРѕР·РґР°РЅРёРµ РіСЂСѓРїРїС‹ РіРѕР»РѕСЃРѕРІРѕРіРѕ С‡Р°С‚Р° РґР»СЏ РѕС‚СЂСЏРґР° Рё РїСЂРёСЃРѕРµРґРёРЅРµРЅРёРµ РёРіСЂРѕРєР°
   public static void createAndJoinGroup(ServerPlayer player, int squadId, String squadName) {
      if (serverApi != null) {
         try {
            String password = UUID.randomUUID().toString().substring(0, 8);
            UUID groupId = UUID.randomUUID();
            Group group = serverApi.groupBuilder()
               .setId(groupId)
               .setName("Squad: " + squadName)
               .setPassword(password)
               .setType(Type.OPEN)
               .setPersistent(false)
               .setHidden(true)
               .build();
            squadToGroupMap.put(squadId, group);
            VoicechatConnection conn = serverApi.getConnectionOf(player.getUUID());
            if (conn != null) {
               conn.setGroup(group);
            }
         } catch (Exception e) {
            System.err.println("[PWP Warfare] createAndJoinGroup failed for squad " + squadId + ": " + e);
         }
      }
   }

   // РџСЂРёСЃРѕРµРґРёРЅРµРЅРёРµ РёРіСЂРѕРєР° Рє СЃСѓС‰РµСЃС‚РІСѓСЋС‰РµР№ РіСЂСѓРїРїРµ РѕС‚СЂСЏРґР°
   public static void joinGroup(ServerPlayer player, int squadId) {
      if (serverApi != null) {
         try {
            Group group = squadToGroupMap.get(squadId);
            if (group != null) {
               VoicechatConnection conn = serverApi.getConnectionOf(player.getUUID());
               if (conn != null) {
                  conn.setGroup(group);
               }
            }
         } catch (Exception e) {
            System.err.println("[PWP Warfare] joinGroup failed for squad " + squadId + ": " + e);
         }
      }
   }

   // Р’С‹С…РѕРґ РёРіСЂРѕРєР° РёР· РіРѕР»РѕСЃРѕРІРѕР№ РіСЂСѓРїРїС‹ РѕС‚СЂСЏРґР°
   public static void leaveGroup(ServerPlayer player) {
      if (serverApi != null) {
         try {
            VoicechatConnection conn = serverApi.getConnectionOf(player.getUUID());
            if (conn != null) {
               conn.setGroup(null);
            }
         } catch (Exception e) {
            System.err.println("[PWP Warfare] leaveGroup failed: " + e);
         }
      }
   }
}
