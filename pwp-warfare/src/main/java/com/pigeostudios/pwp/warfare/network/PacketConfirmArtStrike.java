package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет подтверждения или отклонения артиллерийского удара командиром
// Отправляется командиром в ответ на запрос артиллерии
public class PacketConfirmArtStrike {
    private final boolean accept;

    public PacketConfirmArtStrike(boolean accept) {
        this.accept = accept;
    }

    public static void encode(PacketConfirmArtStrike msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.accept);
    }

    public static PacketConfirmArtStrike decode(FriendlyByteBuf buf) {
        return new PacketConfirmArtStrike(buf.readBoolean());
    }

    // Обрабатывает решение командира: при подтверждении создаёт активный
    // артиллерийский удар в указанной позиции и оповещает всех игроков
    public static void handle(PacketConfirmArtStrike msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                ServerLevel level = player.serverLevel();
                WarfareWorldData data = WarfareWorldData.get(level);
                String team =
                        player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
                int mySquadId = player.getPersistentData().getInt("WARFARE_SquadID");
                boolean isSL = player.getPersistentData().getBoolean("WARFARE_IsSquadLeader");
                int teamCmdId = team.equals("BLUE") ? data.blueCMDId : data.redCMDId;
                if (mySquadId == teamCmdId && teamCmdId != -1 && isSL) {
                    WarfareWorldData.ArtStrikeRequest request =
                            team.equals("BLUE") ? data.blueArtRequest : data.redArtRequest;
                    if (request != null) {
                        if (msg.accept) {
                            data.activeStrikes.add(new WarfareWorldData.ActiveStrike(request.pos, team));
                            level.getServer()
                                    .getPlayerList()
                                    .broadcastSystemMessage(
                                            Component.literal(
                                                            "СТРАТЕГИЧЕСКИ: Артиллерийский удар подтверждён командиром!")
                                                    .withStyle(new ChatFormatting[] {
                                                        ChatFormatting.DARK_RED, ChatFormatting.BOLD
                                                    }),
                                            false);
                        }

                        if (team.equals("BLUE")) {
                            data.blueArtRequest = null;
                        } else {
                            data.redArtRequest = null;
                        }

                        data.activeMarkers.removeIf(m -> m.type.equals("Artillery Request") && m.team.equals(team));
                        data.setDirty();
                        PacketHandler.sendToAllClients(level, data);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
