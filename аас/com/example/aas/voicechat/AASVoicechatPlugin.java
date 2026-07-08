/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  de.maxhenkel.voicechat.api.ForgeVoicechatPlugin
 *  de.maxhenkel.voicechat.api.Group
 *  de.maxhenkel.voicechat.api.Group$Type
 *  de.maxhenkel.voicechat.api.VoicechatApi
 *  de.maxhenkel.voicechat.api.VoicechatConnection
 *  de.maxhenkel.voicechat.api.VoicechatPlugin
 *  de.maxhenkel.voicechat.api.VoicechatServerApi
 *  de.maxhenkel.voicechat.api.events.EventRegistration
 *  de.maxhenkel.voicechat.api.events.MicrophonePacketEvent
 *  de.maxhenkel.voicechat.api.events.PlayerConnectedEvent
 *  de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.world.item.ItemStack
 *  net.minecraftforge.network.PacketDistributor
 *  net.minecraftforge.registries.ForgeRegistries
 *  net.minecraftforge.server.ServerLifecycleHooks
 */
package com.example.aas.voicechat;

import com.example.aas.network.PacketHandler;
import com.example.aas.network.PacketRadioVoiceActivity;
import com.example.aas.network.PacketVoiceActivity;
import com.example.aas.world.AASWorldData;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
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
public class AASVoicechatPlugin
implements VoicechatPlugin {
    private static VoicechatServerApi serverApi;
    private static final Map<Integer, Group> squadToGroupMap;
    private static final Map<UUID, Long> lastVoiceActivity;
    private static final String WALKIETALKIE_NAMESPACE = "walkietalkie";
    private static final String TAG_ACTIVATE = "walkietalkie.activate";
    private static final String TAG_CANAL = "walkietalkie.canal";
    private static final String TAG_MUTE = "walkietalkie.mute";

    public String getPluginId() {
        return "aas_voicechat";
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
        block8: {
            try {
                if (serverApi == null || ServerLifecycleHooks.getCurrentServer() == null) {
                    return;
                }
                UUID playerUuid = event.getSenderConnection().getPlayer().getUuid();
                long now = System.currentTimeMillis();
                if (now - lastVoiceActivity.getOrDefault(playerUuid, 0L) <= 150L) break block8;
                lastVoiceActivity.put(playerUuid, now);
                ServerPlayer sender = ServerLifecycleHooks.getCurrentServer().m_6846_().m_11259_(playerUuid);
                if (sender == null) {
                    return;
                }
                String pName = sender.m_6302_();
                Integer talkerCanal = this.getActiveWalkieTalkieCanal(sender);
                if (talkerCanal != null) {
                    PacketRadioVoiceActivity radioPacket = new PacketRadioVoiceActivity(pName);
                    for (ServerPlayer p : ServerLifecycleHooks.getCurrentServer().m_6846_().m_11314_()) {
                        if (p.m_20148_().equals(sender.m_20148_()) || !this.playerHasWalkieTalkieOnCanal(p, talkerCanal)) continue;
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), (Object)radioPacket);
                    }
                }
                AASWorldData data = AASWorldData.get(sender.m_284548_());
                for (AASWorldData.Squad s : data.squads) {
                    if (!s.members.contains(pName)) continue;
                    PacketVoiceActivity packet = new PacketVoiceActivity(pName);
                    for (String member : s.members) {
                        ServerPlayer p = sender.f_8924_.m_6846_().m_11255_(member);
                        if (p == null) continue;
                        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> p), (Object)packet);
                    }
                    break;
                }
            }
            catch (Exception e) {
                System.err.println("[AAS Voicechat] onMicPacket failed: " + String.valueOf(e));
            }
        }
    }

    private Integer getActiveWalkieTalkieCanal(ServerPlayer player) {
        try {
            Integer canal = this.getActiveCanalFromStack(player.m_21205_());
            if (canal != null) {
                return canal;
            }
            return this.getActiveCanalFromStack(player.m_21206_());
        }
        catch (Exception e) {
            return null;
        }
    }

    private Integer getActiveCanalFromStack(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return null;
        }
        if (!this.isWalkieTalkieStack(stack)) {
            return null;
        }
        CompoundTag tag = stack.m_41783_();
        if (tag == null) {
            return null;
        }
        boolean activated = tag.m_128471_(TAG_ACTIVATE);
        boolean muted = tag.m_128471_(TAG_MUTE);
        if (!activated || muted) {
            return null;
        }
        return tag.m_128451_(TAG_CANAL);
    }

    private boolean playerHasWalkieTalkieOnCanal(ServerPlayer player, int canal) {
        try {
            for (ItemStack stack : player.m_150109_().f_35974_) {
                if (!this.matchesCanal(stack, canal)) continue;
                return true;
            }
            return this.matchesCanal(player.m_21206_(), canal);
        }
        catch (Exception e) {
            return false;
        }
    }

    private boolean matchesCanal(ItemStack stack, int canal) {
        if (stack == null || stack.m_41619_() || !this.isWalkieTalkieStack(stack)) {
            return false;
        }
        CompoundTag tag = stack.m_41783_();
        if (tag == null) {
            return false;
        }
        return tag.m_128451_(TAG_CANAL) == canal;
    }

    private boolean isWalkieTalkieStack(ItemStack stack) {
        if (stack == null || stack.m_41619_()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey((Object)stack.m_41720_());
        return id != null && id.m_135827_().equals(WALKIETALKIE_NAMESPACE);
    }

    private void onPlayerConnectedVoice(PlayerConnectedEvent event) {
        try {
            if (serverApi == null || ServerLifecycleHooks.getCurrentServer() == null) {
                return;
            }
            UUID playerUuid = event.getConnection().getPlayer().getUuid();
            ServerPlayer player = ServerLifecycleHooks.getCurrentServer().m_6846_().m_11259_(playerUuid);
            if (player == null) {
                return;
            }
            int squadId = player.getPersistentData().m_128451_("AAS_SquadID");
            if (squadId > 0) {
                AASVoicechatPlugin.joinGroup(player, squadId);
            }
        }
        catch (Exception e) {
            System.err.println("[AAS Voicechat] onPlayerConnectedVoice failed: " + String.valueOf(e));
        }
    }

    public static void createAndJoinGroup(ServerPlayer player, int squadId, String squadName) {
        if (serverApi == null) {
            return;
        }
        try {
            String password = UUID.randomUUID().toString().substring(0, 8);
            UUID groupId = UUID.randomUUID();
            Group group = serverApi.groupBuilder().setId(groupId).setName("Squad: " + squadName).setPassword(password).setType(Group.Type.OPEN).setPersistent(false).setHidden(true).build();
            squadToGroupMap.put(squadId, group);
            VoicechatConnection conn = serverApi.getConnectionOf(player.m_20148_());
            if (conn != null) {
                conn.setGroup(group);
            }
        }
        catch (Exception e) {
            System.err.println("[AAS Voicechat] createAndJoinGroup failed for squad " + squadId + ": " + String.valueOf(e));
        }
    }

    public static void joinGroup(ServerPlayer player, int squadId) {
        if (serverApi == null) {
            return;
        }
        try {
            VoicechatConnection conn;
            Group group = squadToGroupMap.get(squadId);
            if (group != null && (conn = serverApi.getConnectionOf(player.m_20148_())) != null) {
                conn.setGroup(group);
            }
        }
        catch (Exception e) {
            System.err.println("[AAS Voicechat] joinGroup failed for squad " + squadId + ": " + String.valueOf(e));
        }
    }

    public static void leaveGroup(ServerPlayer player) {
        if (serverApi == null) {
            return;
        }
        try {
            VoicechatConnection conn = serverApi.getConnectionOf(player.m_20148_());
            if (conn != null) {
                conn.setGroup(null);
            }
        }
        catch (Exception e) {
            System.err.println("[AAS Voicechat] leaveGroup failed: " + String.valueOf(e));
        }
    }

    static {
        squadToGroupMap = new ConcurrentHashMap<Integer, Group>();
        lastVoiceActivity = new ConcurrentHashMap<UUID, Long>();
    }
}

