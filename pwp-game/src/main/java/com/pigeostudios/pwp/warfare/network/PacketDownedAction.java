package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.events.DownedHandler;
import com.pigeostudios.pwp.warfare.sound.ModSounds;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет действия в состоянии "ранен": сдаться или позвать медика
// Отправляется клиентом при нажатии соответствующих кнопок
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

    // Обрабатывает действие: 0 - крик о помощи, 1 - принудительная сдача
    public static void handle(PacketDownedAction msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = (ServerLevel) player.level();
                if (msg.action == 1) {
                    DownedHandler.forceGiveUp(player);
                } else if (msg.action == 0) {
                    long currentTime = GameClock.worldTicks(level);
                    long lastCall = WarfareData.of(player).getLong(WarfareKeys.LAST_MEDIC_SHOUT);
                    if (currentTime - lastCall >= 300L) {
                        WarfareData.of(player).putLong(WarfareKeys.LAST_MEDIC_SHOUT, currentTime);
                        WarfareData.of(player)
                                .putLong(WarfareKeys.LAST_MEDIC_SHOUT_TIME_MS, GameClock.worldTicks(level));
                        WarfareWorldData data = WarfareWorldData.get(level);
                        String faction = "none";
                        if (player.getTeam() != null) {
                            String teamName = player.getTeam().getName();
                            faction =
                                    teamName.equalsIgnoreCase("Blue") ? data.match.blueFaction : data.match.redFaction;
                        }

                        SoundEvent finalSound;
                        if (faction != null && !faction.isEmpty() && !faction.equalsIgnoreCase("none")) {
                            if (ModSounds.FACTION_SCREAMS.containsKey(faction.toLowerCase())) {
                                int randomIndex = player.getRandom().nextInt(3);
                                finalSound = (SoundEvent) ModSounds.FACTION_SCREAMS
                                        .get(faction.toLowerCase())
                                        .get(randomIndex)
                                        .get();
                            } else {
                                finalSound = (SoundEvent) ModSounds.HELP_SCREAM.get();
                            }
                        } else {
                            finalSound = (SoundEvent) ModSounds.HELP_SCREAM.get();
                        }

                        level.playSound(
                                null,
                                player.getX(),
                                player.getY(),
                                player.getZ(),
                                finalSound,
                                SoundSource.PLAYERS,
                                2.0F,
                                1.0F);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
