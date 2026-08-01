package com.pigeostudios.pwp.warfare.voicechat;

import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRadioVoiceActivity;
import com.pigeostudios.pwp.warfare.network.PacketVoiceActivity;
import com.pigeostudios.pwp.warfare.network.PacketVoiceChannelState.Channel;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreserver.CoreServerApi;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerConnectedEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.packets.LocationalSoundPacket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
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

   // Смещение источника радио-аудио относительно слушателя («рация на плече»):
   // отрядный канал звучит слева, командирский — справа
   private static final double EAR_OFFSET_DISTANCE = 2.0D;
   private static final double EAR_OFFSET_HEIGHT = 1.35D;
   private static final AtomicBoolean CLEANUP_REGISTERED = new AtomicBoolean(false);

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
      ensureCleanupRegistered();
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

         // Эксклюзивная рация (как в Squad): при активном радио-канале голос идёт только по рации,
         // отмена исходной рассылки SVC глушит локальную трансляцию всем рядом
         if (channel != Channel.LOCAL) {
            event.cancel();
         }

         if (channel == Channel.SQUAD) {
            WarfareWorldData.Squad squad = getPlayerSquad(pName, data);
            if (squad != null) {
               // Отрядный канал: у слушателей звук из левого уха.
               // channelId = UUID говорящего (стабильный), иначе клиент создаёт новый аудио-канал на каждый пакет
               for (String memberName : squad.members) {
                  if (memberName.equals(pName)) continue;
                  ServerPlayer member = sender.server.getPlayerList().getPlayerByName(memberName);
                  if (member != null) {
                     VoicechatConnection conn = serverApi.getConnectionOf(member.getUUID());
                     if (conn != null) {
                        LocationalSoundPacket pkt = event.getPacket()
                           .locationalSoundPacketBuilder()
                           .channelId(playerUuid)
                           .position(earPosition(member, false))
                           .build();
                        serverApi.sendLocationalSoundPacketTo(conn, pkt);
                     }
                  }
               }
            }
         }

         if (channel == Channel.COMMAND) {
            WarfareWorldData.Squad squad = getPlayerSquad(pName, data);
            // Командный канал доступен лидерам отрядов независимо от того, заполнено ли поле leader
            boolean isLeader = squad != null
               && ((squad.leader != null && squad.leader.equals(pName))
                   || (squad.bravoLeader != null && squad.bravoLeader.equals(pName))
                   || (squad.charlieLeader != null && squad.charlieLeader.equals(pName)));
            if (isLeader) {
               // Командирский канал: у слушателей звук из правого уха
               for (WarfareWorldData.Squad s : data.squads) {
                  if (!s.team.equalsIgnoreCase(senderTeam)) continue;
                  if (s.leader != null && !s.leader.isEmpty()
                     && s.members.contains(s.leader)
                     && !s.leader.equals(pName)) {
                     ServerPlayer sl = sender.server.getPlayerList().getPlayerByName(s.leader);
                     if (sl != null) {
                        VoicechatConnection conn = serverApi.getConnectionOf(sl.getUUID());
                        if (conn != null) {
                           LocationalSoundPacket pkt = event.getPacket()
                              .locationalSoundPacketBuilder()
                              .channelId(playerUuid)
                              .position(earPosition(sl, true))
                              .build();
                           serverApi.sendLocationalSoundPacketTo(conn, pkt);
                        }
                     }
                  }
                  if (s.bravoLeader != null && !s.bravoLeader.isEmpty()
                     && !s.bravoLeader.equals(pName)) {
                     ServerPlayer bl = sender.server.getPlayerList().getPlayerByName(s.bravoLeader);
                     if (bl != null) {
                        VoicechatConnection conn = serverApi.getConnectionOf(bl.getUUID());
                        if (conn != null) {
                           LocationalSoundPacket pkt = event.getPacket()
                              .locationalSoundPacketBuilder()
                              .channelId(playerUuid)
                              .position(earPosition(bl, true))
                              .build();
                           serverApi.sendLocationalSoundPacketTo(conn, pkt);
                        }
                     }
                  }
                  if (s.charlieLeader != null && !s.charlieLeader.isEmpty()
                     && !s.charlieLeader.equals(pName)) {
                     ServerPlayer cl = sender.server.getPlayerList().getPlayerByName(s.charlieLeader);
                     if (cl != null) {
                        VoicechatConnection conn = serverApi.getConnectionOf(cl.getUUID());
                        if (conn != null) {
                           LocationalSoundPacket pkt = event.getPacket()
                              .locationalSoundPacketBuilder()
                              .channelId(playerUuid)
                              .position(earPosition(cl, true))
                              .build();
                           serverApi.sendLocationalSoundPacketTo(conn, pkt);
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
                  if (s.bravoLeader != null && !s.bravoLeader.isEmpty()) {
                     ServerPlayer bl = sender.server.getPlayerList().getPlayerByName(s.bravoLeader);
                     if (bl != null) {
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> bl), rpkt);
                     }
                  }
                  if (s.charlieLeader != null && !s.charlieLeader.isEmpty()) {
                     ServerPlayer cl = sender.server.getPlayerList().getPlayerByName(s.charlieLeader);
                     if (cl != null) {
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> cl), rpkt);
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

   // Позиция источника радио-аудио относительно слушателя: ухо на высоте плеча.
   // Позиция пересчитывается для каждого пакета по текущему положению и повороту слушателя,
   // поэтому звук «привязан» к правому/левому уху и не «уплывает» при повороте головы.
   private static Position earPosition(ServerPlayer listener, boolean rightEar) {
      Vec3 forward = listener.getLookAngle();
      Vec3 right = new Vec3(-forward.z, 0.0D, forward.x);
      double len = Math.sqrt(right.x * right.x + right.z * right.z);
      if (len < 1.0E-5D) {
         right = new Vec3(1.0D, 0.0D, 0.0D);
      } else {
         right = right.scale(1.0D / len);
      }
      Vec3 base = listener.position().add(0.0D, EAR_OFFSET_HEIGHT, 0.0D);
      Vec3 pos = rightEar
         ? base.add(right.scale(EAR_OFFSET_DISTANCE))
         : base.add(right.scale(-EAR_OFFSET_DISTANCE));
      return serverApi.createPosition(pos.x, pos.y, pos.z);
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

   private static void ensureCleanupRegistered() {
      if (CLEANUP_REGISTERED.compareAndSet(false, true)) {
         MinecraftForge.EVENT_BUS.addListener(WarfareVoicechatPlugin::onPlayerDisconnected);
      }
   }

   // Очистка состояния канала и кэшей при выходе игрока, чтобы игрок,
   // вышедший с зажатым PTT, после реконнекта не остался в радио-канале
   private static void onPlayerDisconnected(PlayerEvent.PlayerLoggedOutEvent event) {
      try {
         if (event.getEntity() instanceof ServerPlayer sp) {
            playerChannels.remove(sp.getUUID());
            lastVoiceActivity.remove(sp.getUUID());
            invalidateMuteCache(sp.getUUID());
         }
      } catch (Exception e) {
         System.err.println("[PWP Warfare] onPlayerDisconnected failed: " + e);
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
