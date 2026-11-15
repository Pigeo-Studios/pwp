package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет запроса на проведение голосования за командира
// Отправляется лидером отряда для выдвижения своей кандидатуры
public class PacketRequestCMD {
    public static void encode(PacketRequestCMD msg, FriendlyByteBuf buf) {}

    public static PacketRequestCMD decode(FriendlyByteBuf buf) {
        return new PacketRequestCMD();
    }

    // Запускает голосование за командира, если оно ещё не активно
    public static void handle(PacketRequestCMD msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.getTeam() != null) {
                WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                String pName = player.getScoreboardName();
                String team = player.getTeam().getName().toUpperCase();
                boolean isBlue = team.equals("BLUE");
                boolean alreadyVoting = isBlue ? data.blueCmdVoteActive : data.redCmdVoteActive;
                if (!alreadyVoting) {
                    int currentCMD = isBlue ? data.blueCMDId : data.redCMDId;
                    if (currentCMD == -1) {
                        if (WarfareData.of(player).getBoolean(WarfareKeys.IS_SQUAD_LEADER)) {
                            if (isBlue) {
                                data.blueCmdVoteActive = true;
                                data.blueCmdCandidateName = pName;
                                data.blueCmdCandidateId = WarfareData.of(player).getInt(WarfareKeys.SQUAD_ID);
                                data.blueCmdVoteTimer = 600;
                                data.blueCmdVotes.clear();
                            } else {
                                data.redCmdVoteActive = true;
                                data.redCmdCandidateName = pName;
                                data.redCmdCandidateId = WarfareData.of(player).getInt(WarfareKeys.SQUAD_ID);
                                data.redCmdVoteTimer = 600;
                                data.redCmdVotes.clear();
                            }

                            data.setDirty();
                            PacketHandler.sendToAllClients(player.serverLevel(), data);
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
