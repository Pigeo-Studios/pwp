/*
 * Decompiled with CFR 0.152.
 */
package com.example.aas.network;

import com.example.aas.client.ClientData;
import com.example.aas.network.MapPlayerInfo;
import com.example.aas.network.PacketSyncMapPlayers;

public class ClientPacketHandler {
    public static void handleSyncMap(PacketSyncMapPlayers msg) {
        ClientData.mapPlayers.clear();
        for (MapPlayerInfo info : msg.getPlayers()) {
            ClientData.mapPlayers.put(info.name, info);
        }
    }
}

