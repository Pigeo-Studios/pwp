package com.pigeostudios.pwp.warfare.client;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.logging.LogUtils;
import com.pigeostudios.pwp.warfare.network.PacketSyncPlayerSkin;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Team;
import org.slf4j.Logger;

public class ClientSkinManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<UUID, PlayerSkinAssignment> skinAssignments = new HashMap<>();

    private static class PlayerSkinAssignment {
        final String faction;
        final String kitName;

        PlayerSkinAssignment(String faction, String kitName) {
            this.faction = faction;
            this.kitName = kitName;
        }

        ResourceLocation getTexturePath() {
            String factionLower = faction.toLowerCase();
            String kitFileName = kitName == null || kitName.isEmpty() || kitName.equals("Unassigned")
                    ? "base"
                    : kitName.toLowerCase().replace(" ", "_").replace("-", "_");
            return new ResourceLocation("pwpwarfare", "textures/skins/" + factionLower + "/" + kitFileName + ".png");
        }
    }

    public static void handlePacket(PacketSyncPlayerSkin msg) {
        setSkin(msg.playerUUID, msg.faction, msg.kitName);
    }

    public static void setSkin(UUID playerUUID, String faction, String kitName) {
        if (faction == null || faction.isEmpty() || faction.equals("none") || kitName == null) {
            skinAssignments.remove(playerUUID);
            return;
        }
        skinAssignments.put(playerUUID, new PlayerSkinAssignment(faction, kitName));
        applySkinNow(playerUUID);
    }

    public static void clearPlayer(UUID playerUUID) {
        skinAssignments.remove(playerUUID);
    }

    public static void applySkinNow(UUID playerUUID) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Player player = mc.level.getPlayerByUUID(playerUUID);
        if (!(player instanceof AbstractClientPlayer clientPlayer)) return;

        PlayerSkinAssignment assignment = skinAssignments.get(playerUUID);
        if (assignment == null) return;

        ResourceLocation tex = assignment.getTexturePath();

        try {
            PlayerInfo info = getPlayerInfoField(clientPlayer);
            if (info == null) return;

            Map<MinecraftProfileTexture.Type, ResourceLocation> texMap = getTexturesField(info);
            if (texMap == null) return;

            texMap.put(MinecraftProfileTexture.Type.SKIN, tex);
        } catch (Exception e) {
            LOGGER.warn("applySkinNow: error for player {}", playerUUID, e);
        }
    }

    public static void applyAllSkins() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        for (AbstractClientPlayer player : mc.level.players()) {
            UUID uuid = player.getUUID();
            PlayerSkinAssignment assignment = skinAssignments.get(uuid);
            if (assignment == null) {
                applyDefaultSkin(player);
                continue;
            }

            ResourceLocation tex = assignment.getTexturePath();
            try {
                PlayerInfo info = getPlayerInfoField(player);
                if (info == null) continue;

                Map<MinecraftProfileTexture.Type, ResourceLocation> texMap = getTexturesField(info);
                if (texMap == null) continue;

                texMap.put(MinecraftProfileTexture.Type.SKIN, tex);
            } catch (Exception ignored) {}
        }
    }

    private static void applyDefaultSkin(AbstractClientPlayer player) {
        Team team = player.getTeam();
        if (team == null) return;

        String faction;
        if (team.getName().equalsIgnoreCase("Blue")) {
            faction = ClientData.BLUE_FACTION;
        } else if (team.getName().equalsIgnoreCase("Red")) {
            faction = ClientData.RED_FACTION;
        } else {
            return;
        }

        if (faction == null || faction.equals("none")) return;

        String kit = ClientData.playerKits.getOrDefault(player.getScoreboardName(), "Unassigned");
        ResourceLocation tex = new ResourceLocation("pwpwarfare",
                "textures/skins/" + faction.toLowerCase() + "/" +
                (kit.equals("Unassigned") || kit.isEmpty() ? "base" : kit.toLowerCase().replace(" ", "_").replace("-", "_")) + ".png");

        try {
            PlayerInfo info = getPlayerInfoField(player);
            if (info == null) return;

            Map<MinecraftProfileTexture.Type, ResourceLocation> texMap = getTexturesField(info);
            if (texMap == null) return;

            texMap.put(MinecraftProfileTexture.Type.SKIN, tex);
        } catch (Exception ignored) {}
    }

    private static PlayerInfo getPlayerInfoField(AbstractClientPlayer player) {
        try {
            Field f = AbstractClientPlayer.class.getDeclaredField("playerInfo");
            f.setAccessible(true);
            return (PlayerInfo) f.get(player);
        } catch (Exception e1) {
            try {
                Field f = AbstractClientPlayer.class.getDeclaredField("f_108546_");
                f.setAccessible(true);
                return (PlayerInfo) f.get(player);
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private static Map<MinecraftProfileTexture.Type, ResourceLocation> getTexturesField(PlayerInfo info) {
        try {
            Field f = PlayerInfo.class.getDeclaredField("textures");
            f.setAccessible(true);
            return (Map<MinecraftProfileTexture.Type, ResourceLocation>) f.get(info);
        } catch (Exception e1) {
            try {
                Field f = PlayerInfo.class.getDeclaredField("f_105299_");
                f.setAccessible(true);
                return (Map<MinecraftProfileTexture.Type, ResourceLocation>) f.get(info);
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
