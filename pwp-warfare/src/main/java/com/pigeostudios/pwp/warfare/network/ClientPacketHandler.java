package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.client.ClientData;

// Обработчик входящих сетевых пакетов на стороне клиента
// Содержит методы для обработки данных, полученных от сервера
public class ClientPacketHandler {
   // Обрабатывает пакет синхронизации списка игроков на карте
   // Очищает текущий список и заполняет его новыми данными
   public static void handleSyncMap(PacketSyncMapPlayers msg) {
      ClientData.mapPlayers.clear();

      for (MapPlayerInfo info : msg.getPlayers()) {
         ClientData.mapPlayers.put(info.name, info);
      }
   }
}
