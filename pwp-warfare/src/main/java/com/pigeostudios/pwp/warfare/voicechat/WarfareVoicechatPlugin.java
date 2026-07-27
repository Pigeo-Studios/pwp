package com.pigeostudios.pwp.warfare.voicechat;

import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRadioVoiceActivity;
import com.pigeostudios.pwp.warfare.network.PacketVoiceActivity;
import com.pigeostudios.pwp.warfare.network.PacketVoiceChannelState.Channel;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import com.pwp.coreserver.CoreServerApi;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

@ForgeVoicechatPlugin
public class WarfareVoicechatPlugin implements VoicechatPlugin {
   private static VoicechatServerApi serverApi;
   private static final Map<UUID, Channel> playerChannels = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> lastVoiceActivity = new ConcurrentHashMap<>();
   private static final Map<UUID, Boolean> mutedCache = new ConcurrentHashMap<>();
   private static final Map<UUID, Long> mutedCacheTimestamp = new ConcurrentHashMap<>();
   private static final long MUTE_CACHE_TTL_MS = 5000L;

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

   public static void setPlayerChannel(UUID uuid, Channel channel) {
      if (channel == Channel.LOCAL) {
         playerChannels.remove(uuid);
      } else {
         playerChannels.put(uuid, channel);
      }
   }

   private void onMicPacket(MicrophonePacketEvent event) {
      try {
         if (serverApi == null || ServerLifecycleHooks.getCurrentServer() == null) {
            return;
         }

         VoicechatConnection senderConn = event.getSenderConnection();
         if (senderConn == null || senderConn.getPlayer() == null) {
            return;
         }

         UUID playerUuid = senderConn.getPlayer().getUuid();
         if (isPlayerVoiceMuted(playerUuid)) {
            event.cancel();
            return;
         }

         ServerPlayer sender = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerUuid);
         if (sender == null) {
            return;
         }

         String pName = sender.getScoreboardName();
         String senderTeam = sender.getTeam() != null ? sender.getTeam().getName() : null;
         if (senderTeam == null) {
            return;
         }

         Channel channel = playerChannels.getOrDefault(playerUuid, Channel.LOCAL);
         WarfareWorldData data = WarfareWorldData.get(sender.serverLevel());

         if (channel == Channel.SQUAD) {
            WarfareWorldData.Squad squad = getPlayerSquad(pName, data);
            if (squad != null) {
               StaticSoundPacket pkt = event.getPacket()
                  .staticSoundPacketBuilder()
                  .channelId(UUID.randomUUID())
                  .build();
               for (String memberName : squad.members) {
                  if (memberName.equals(pName)) continue;
                  ServerPlayer member = sender.server.getPlayerList().getPlayerByName(memberName);
                  if (member != null) {
                     VoicechatConnection conn = serverApi.getConnectionOf(member.getUUID());
                     if (conn != null) {
                        serverApi.sendStaticSoundPacketTo(conn, pkt);
                     }
                  }
               }
            }
         }

         if (channel == Channel.COMMAND) {
            WarfareWorldData.Squad squad = getPlayerSquad(pName, data);
            if (squad != null && squad.leader != null && squad.leader.equals(pName)) {
               int teamCMDId = senderTeam.equalsIgnoreCase("BLUE")
                  ? data.blueCMDId : data.redCMDId;

               StaticSoundPacket pkt = event.getPacket()
                  .staticSoundPacketBuilder()
                  .channelId(UUID.randomUUID())
                  .build();

               for (WarfareWorldData.Squad s : data.squads) {
                  if (!s.team.equalsIgnoreCase(senderTeam)) continue;
                  if (s.leader != null && !s.leader.isEmpty()
                     && s.members.contains(s.leader)
                     && !s.leader.equals(pName)) {
                     ServerPlayer sl = sender.server.getPlayerList().getPlayerByName(s.leader);
                     if (sl != null) {
                        VoicechatConnection conn = serverApi.getConnectionOf(sl.getUUID());
                        if (conn != null) {
                           serverApi.sendStaticSoundPacketTo(conn, pkt);
                        }
                     }
                  }
                  if (s.id == teamCMDId) {
                     for (String m : s.members) {
                        if (m.equals(pName)) continue;
                        ServerPlayer cmdMember = sender.server.getPlayerList().getPlayerByName(m);
                        if (cmdMember != null) {
                           VoicechatConnection conn = serverApi.getConnectionOf(cmdMember.getUUID());
                           if (conn != null) {
                              serverApi.sendStaticSoundPacketTo(conn, pkt);
                           }
                        }
                     }
                  }
               }
            }
         }

         long now = System.currentTimeMillis();
         if (now - lastVoiceActivity.getOrDefault(playerUuid, 0L) > 150L) {
            lastVoiceActivity.put(playerUuid, now);

            if (channel == Channel.SQUAD) {
               WarfareWorldData.Squad squad = getPlayerSquad(pName, data);
                if (squad != null) {
                   PacketVoiceActivity vpkt = new PacketVoiceActivity(pName);
                   for (String memberName : squad.members) {
                      ServerPlayer p = sender.server.getPlayerList().getPlayerByName(memberName);
                      if (p != null) {
                         PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), vpkt);
                      }
                   }
                   PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sender), vpkt);
                }
            }

            if (channel == Channel.COMMAND) {
                PacketRadioVoiceActivity rpkt = new PacketRadioVoiceActivity(pName);
                for (WarfareWorldData.Squad s : data.squads) {
                   if (!s.team.equalsIgnoreCase(senderTeam)) continue;
                   if (s.leader != null && !s.leader.isEmpty()
                      && s.members.contains(s.leader)) {
                      ServerPlayer p = sender.server.getPlayerList().getPlayerByName(s.leader);
                      if (p != null) {
                         PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), rpkt);
                      }
                   }
                }
                PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> sender), rpkt);
             }
         }
      } catch (Exception e) {
         System.err.println("[PWP Warfare] onMicPacket failed: " + e);
      }
   }

   private void onPlayerConnectedVoice(PlayerConnectedEvent event) {
      try {
         if (serverApi == null || ServerLifecycleHooks.getCurrentServer() == null) {
            return;
         }
      } catch (Exception e) {
         System.err.println("[PWP Warfare] onPlayerConnectedVoice failed: " + e);
      }
   }

   private static boolean isPlayerVoiceMuted(UUID playerUuid) {
      long now = System.currentTimeMillis();
      Long lastCheck = mutedCacheTimestamp.get(playerUuid);
      if (lastCheck != null && now - lastCheck < MUTE_CACHE_TTL_MS) {
         return mutedCache.getOrDefault(playerUuid, false);
      }
      try {
          JsonObject result = CoreServerApi.getVoiceMute(playerUuid.toString());
         if (result != null && result.has("success") && result.get("success").getAsBoolean()
                 && result.has("data") && !result.get("data").isJsonNull()) {
            JsonObject data = result.getAsJsonObject("data");
            boolean muted = data.has("muted") && data.get("muted").getAsBoolean();
            mutedCache.put(playerUuid, muted);
            mutedCacheTimestamp.put(playerUuid, now);
            return muted;
         }
      } catch (Exception e) {
         System.err.println("[PWP Warfare] Voice mute check failed for " + playerUuid + ": " + e);
      }
      mutedCache.put(playerUuid, false);
      mutedCacheTimestamp.put(playerUuid, now);
      return false;
   }

   public static void invalidateMuteCache(UUID playerUuid) {
      mutedCache.remove(playerUuid);
      mutedCacheTimestamp.remove(playerUuid);
   }

   public static WarfareWorldData.Squad getPlayerSquad(String playerName, WarfareWorldData data) {
      for (WarfareWorldData.Squad s : data.squads) {
         if (s.members.contains(playerName)) {
            return s;
         }
      }
      return null;
   }
}
