/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraftforge.fml.ModList
 */
package com.example.aas.voicechat;

import com.example.aas.voicechat.AASVoicechatPlugin;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

public class VoicechatCompat {
    static private volatile boolean broken = false;

    public static boolean isLoaded() {
        if (broken) {
            return false;
        }
        try {
            return ModList.get().isLoaded("voicechat");
        }
        catch (Throwable t) {
            System.err.println("[AAS Voicechat] Failed to check ModList: " + String.valueOf(t));
            broken = true;
            return false;
        }
    }

    public static void createAndJoinGroup(ServerPlayer player, int squadId, String squadName) {
        if (!VoicechatCompat.isLoaded()) {
            return;
        }
        try {
            AASVoicechatPlugin.createAndJoinGroup(player, squadId, squadName);
        }
        catch (Throwable t) {
            System.err.println("[AAS Voicechat] createAndJoinGroup crashed, disabling voicechat integration: " + String.valueOf(t));
            broken = true;
        }
    }

    public static void joinGroup(ServerPlayer player, int squadId) {
        if (!VoicechatCompat.isLoaded()) {
            return;
        }
        try {
            AASVoicechatPlugin.joinGroup(player, squadId);
        }
        catch (Throwable t) {
            System.err.println("[AAS Voicechat] joinGroup crashed, disabling voicechat integration: " + String.valueOf(t));
            broken = true;
        }
    }

    public static void leaveGroup(ServerPlayer player) {
        if (!VoicechatCompat.isLoaded()) {
            return;
        }
        try {
            AASVoicechatPlugin.leaveGroup(player);
        }
        catch (Throwable t) {
            System.err.println("[AAS Voicechat] leaveGroup crashed, disabling voicechat integration: " + String.valueOf(t));
            broken = true;
        }
    }
}

