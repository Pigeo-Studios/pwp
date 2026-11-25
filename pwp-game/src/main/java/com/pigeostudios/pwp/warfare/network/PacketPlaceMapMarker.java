package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.time.GameClock;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет размещения маркера на карте
// Поддерживает типы: артиллерийский запрос, общий маркер и другие
public class PacketPlaceMapMarker {
    private final int x;
    private final int z;
    private final String type;

    public PacketPlaceMapMarker(int x, int z, String type) {
        this.x = x;
        this.z = z;
        this.type = type;
    }

    public static void encode(PacketPlaceMapMarker msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.x);
        buf.writeInt(msg.z);
        buf.writeUtf(msg.type);
    }

    public static PacketPlaceMapMarker decode(FriendlyByteBuf buf) {
        return new PacketPlaceMapMarker(buf.readInt(), buf.readInt(), buf.readUtf());
    }

    // Обрабатывает размещение маркера: проверяет права (для артиллерии),
    // добавляет маркер в мир и оповещает команду
    public static void handle(PacketPlaceMapMarker msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.getTeam() != null) {
                ServerLevel level = player.serverLevel();
                WarfareWorldData data = WarfareWorldData.get(level);
                String team = player.getTeam().getName().toUpperCase();
                String pName = player.getScoreboardName();
                if (msg.type.equals("Artillery Request")) {
                    if (!WarfareData.of(player).getBoolean(WarfareKeys.IS_SQUAD_LEADER)
                            && !PwpPermissions.isAdmin(player)) {
                        return;
                    }

                    WarfareWorldData.ArtStrikeRequest activeReq =
                            team.equals("BLUE") ? data.match.blueArtRequest : data.match.redArtRequest;
                    if (activeReq != null) {
                        return;
                    }

                    BlockPos strikePos = new BlockPos(msg.x, 64, msg.z);
                    if (team.equals("BLUE")) {
                        data.match.blueArtRequest = new WarfareWorldData.ArtStrikeRequest(pName, strikePos);
                    } else {
                        data.match.redArtRequest = new WarfareWorldData.ArtStrikeRequest(pName, strikePos);
                    }

                    data.markers.activeMarkers.add(new WarfareWorldData.MapMarker(
                            strikePos, "Artillery Request", team, GameClock.worldTicks(level) + 3600L));
                    int cmdId = team.equals("BLUE") ? data.squadRegistry.blueCMDId : data.squadRegistry.redCMDId;

                    for (ServerPlayer p : level.players()) {
                        if (p.getTeam() != null
                                && p.getTeam().getName().toUpperCase().equals(team)) {
                            int pSqId = WarfareData.of(p).getInt(WarfareKeys.SQUAD_ID);
                            if (WarfareData.of(p).getBoolean(WarfareKeys.IS_SQUAD_LEADER)
                                    || pSqId == cmdId && cmdId != -1) {
                                p.sendSystemMessage(Component.translatable(
                                                "pwpwarfare.msg.marker.artillery_requested", msg.x, msg.z)
                                        .withStyle(ChatFormatting.GOLD));
                            }
                        }
                    }

                    data.setDirty();
                    PacketHandler.sendToAllClients(level, data);
                } else {
                    data.markers.activeMarkers.add(new WarfareWorldData.MapMarker(
                            new BlockPos(msg.x, 64, msg.z), msg.type, team, GameClock.worldTicks(level) + 3600L));
                    data.setDirty();
                    PacketHandler.sendToAllClients(level, data);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
