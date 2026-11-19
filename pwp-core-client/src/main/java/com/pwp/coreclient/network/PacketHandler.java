package com.pwp.coreclient.network;

import com.pwp.api.net.Net;
import com.pwp.coreclient.CoreClientMod;
import com.pwp.coreclient.network.data.AdminAction;
import com.pwp.coreclient.network.data.DataRequest;
import com.pwp.coreclient.network.data.DataResponse;
import com.pwp.coreclient.network.data.ModerationAction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {

    // 2 — этап 1: PacketAction вместо PacketApiAction, новый формат LobbyStatePacket.
    // 3 — этап 2: сеть v2 (DataRequest/DataResponse, AdminAction.*), удалены мёртвые Vote*/JoinMatchPacket
    // 4: LobbyStatePacket.minPlayers (этап 4); 5: LobbyStatePacket.prepState/prepPercent/prepError (этап 5)
    private static final String PROTOCOL_VERSION = "7";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(CoreClientMod.MODID, "network"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals);

    private static int id = 0;

    public static void register() {
        // S→C: переход на другой сервер (лобби ↔ матч)
        INSTANCE.registerMessage(
                id++,
                ConnectToServerPacket.class,
                ConnectToServerPacket::encode,
                ConnectToServerPacket::decode,
                ConnectToServerPacket::handle);
        // S→C: авторитетное состояние лобби для GUI
        INSTANCE.registerMessage(
                id++,
                LobbyStatePacket.class,
                LobbyStatePacket::encode,
                LobbyStatePacket::decode,
                LobbyStatePacket::handle);
        // Сеть v2 (record-DTO, проверка на сервере). Новые пакеты — только в конец.
        new Net.Registrar(INSTANCE, id)
                .c2s(JoinMatchServerPacket.class, JoinMatchServerPacket::encode, JoinMatchServerPacket::decode)
                .c2s(DataRequest.class, DataRequest::encode, DataRequest::decode)
                .s2c(DataResponse.class, DataResponse::encode, DataResponse::decode)
                .c2s(AdminAction.SaveSkin.class, AdminAction.SaveSkin::encode, AdminAction.SaveSkin::decode)
                .c2s(AdminAction.DeleteSkin.class, AdminAction.DeleteSkin::encode, AdminAction.DeleteSkin::decode)
                .c2s(
                        AdminAction.DeleteFactionVehicle.class,
                        AdminAction.DeleteFactionVehicle::encode,
                        AdminAction.DeleteFactionVehicle::decode)
                // этап 2: админ-панель, вкладка «Лобби»
                .c2s(
                        AdminAction.LobbyStartVote.class,
                        AdminAction.LobbyStartVote::encode,
                        AdminAction.LobbyStartVote::decode)
                .c2s(
                        AdminAction.LobbyStopVote.class,
                        AdminAction.LobbyStopVote::encode,
                        AdminAction.LobbyStopVote::decode)
                .c2s(
                        AdminAction.LobbySaveSettings.class,
                        AdminAction.LobbySaveSettings::encode,
                        AdminAction.LobbySaveSettings::decode)
                // уровень доступа (AccessSync) и вкладка «Модерация»
                .s2c(AccessLevelPacket.class, AccessLevelPacket::encode, AccessLevelPacket::decode)
                .c2s(ModerationAction.Kick.class, ModerationAction.Kick::encode, ModerationAction.Kick::decode)
                .c2s(ModerationAction.Ban.class, ModerationAction.Ban::encode, ModerationAction.Ban::decode)
                .c2s(ModerationAction.Warn.class, ModerationAction.Warn::encode, ModerationAction.Warn::decode)
                .c2s(
                        ModerationAction.VoiceMute.class,
                        ModerationAction.VoiceMute::encode,
                        ModerationAction.VoiceMute::decode)
                .c2s(
                        ModerationAction.TeamJoin.class,
                        ModerationAction.TeamJoin::encode,
                        ModerationAction.TeamJoin::decode)
                .c2s(
                        ModerationAction.ClearTeamkill.class,
                        ModerationAction.ClearTeamkill::encode,
                        ModerationAction.ClearTeamkill::decode)
                // 19.11.2026 (протокол 7): игроки в админ-панели, сервер, объявления
                .c2s(
                        ModerationAction.Teleport.class,
                        ModerationAction.Teleport::encode,
                        ModerationAction.Teleport::decode)
                .c2s(
                        ModerationAction.Spectate.class,
                        ModerationAction.Spectate::encode,
                        ModerationAction.Spectate::decode);
    }
}
