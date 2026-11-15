package com.pwp.coreclient.network;

import com.pwp.api.net.Net;
import com.pwp.coreclient.CoreClientMod;
import com.pwp.coreclient.network.data.AdminAction;
import com.pwp.coreclient.network.data.DataRequest;
import com.pwp.coreclient.network.data.DataResponse;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {

    // 2 — этап 1: PacketAction вместо PacketApiAction, новый формат LobbyStatePacket.
    // 3 — этап 2: сеть v2 (DataRequest/DataResponse, AdminAction.*), удалены мёртвые Vote*/JoinMatchPacket
    private static final String PROTOCOL_VERSION = "3";
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
                        AdminAction.DeleteFactionVehicle::decode);
    }
}
