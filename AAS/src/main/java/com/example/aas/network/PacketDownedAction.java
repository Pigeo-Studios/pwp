/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.network.FriendlyByteBuf
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.server.level.ServerPlayer
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundSource
 *  net.minecraftforge.network.NetworkEvent$Context
 */
package com.example.aas.network;

import com.example.aas.events.DownedHandler;
import com.example.aas.sound.ModSounds;
import com.example.aas.world.AASWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;

public class PacketDownedAction {
    private final int action;

    public PacketDownedAction(int action) {
        this.action = action;
    }

    public static void encode(PacketDownedAction msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.action);
    }

    public static PacketDownedAction decode(FriendlyByteBuf buf) {
        return new PacketDownedAction(buf.readInt());
    }

    public static void handle(PacketDownedAction msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            long lastCall;
            long currentTime;
            ServerPlayer player = ((NetworkEvent.Context)ctx.get()).getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = (ServerLevel)player.level();
            if (msg.action == 1) {
                DownedHandler.forceGiveUp(player);
            } else if (msg.action == 0 && (currentTime = level.getGameTime()) - (lastCall = player.getPersistentData().getLong("AAS_LastMedicShout")) >= 300L) {
                SoundEvent finalSound;
                player.getPersistentData().putLong("AAS_LastMedicShout", currentTime);
                player.getPersistentData().putLong("AAS_LastMedicShoutTimeMS", level.getGameTime());
                AASWorldData data = AASWorldData.get(level);
                String faction = "none";
                if (player.getTeam() != null) {
                    String teamName = player.getTeam().getName();
                    String string = faction = teamName.equalsIgnoreCase("Blue") ? data.blueFaction : data.redFaction;
                }
                if (faction == null || faction.isEmpty() || faction.equalsIgnoreCase("none")) {
                    finalSound = (SoundEvent)ModSounds.HELP_SCREAM.get();
                } else if (ModSounds.FACTION_SCREAMS.containsKey(faction.toLowerCase())) {
                    int randomIndex = player.getRandom().nextInt(3);
                    finalSound = (SoundEvent)ModSounds.FACTION_SCREAMS.get(faction.toLowerCase()).get(randomIndex).get();
                } else {
                    finalSound = (SoundEvent)ModSounds.HELP_SCREAM.get();
                }
                level.playSound(null, player.getX(), player.getY(), player.getZ(), finalSound, SoundSource.PLAYERS, 2.0f, 1.0f);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}

