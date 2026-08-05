package com.pigeostudios.pwp.warfare.client;

// Клиентское зеркало состояния «игрок в мейн-зоне». Обновляется пакетом
// PacketMainZoneState (сервер авторитетен). Используется для мгновенной
// отмены локальных попыток выстрела (TACZ, FCL-трубы), чтобы игрок не видел
// ложную анимацию выстрела. Даже если флаг устарел — сервер всё равно
// заблокирует выстрел.
public class ClientSafetyState {
   public static boolean inMainZone = false;

   private ClientSafetyState() {}
}
