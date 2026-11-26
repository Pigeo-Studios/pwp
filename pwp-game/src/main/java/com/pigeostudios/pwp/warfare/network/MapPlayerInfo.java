package com.pigeostudios.pwp.warfare.network;

import java.util.UUID;

// Класс данных, содержащий информацию об игроке на карте
// Используется для синхронизации позиций и состояний игроков между клиентом и сервером
public class MapPlayerInfo {
    public String name;
    public UUID uuid;
    public double x;
    public double z;
    public float rot;
    public int squadId;
    public boolean isLeader;
    public boolean isDowned;
    public long lastShoutTime;
    public boolean inVehicle;
    public int vehicleId;
    public int seatIndex;
    public String team;

    // Конструктор, инициализирующий все поля информации об игроке
    public MapPlayerInfo(
            String name,
            UUID uuid,
            double x,
            double z,
            float rot,
            int squadId,
            boolean isLeader,
            boolean isDowned,
            long lastShoutTime,
            boolean inVehicle,
            int vehicleId,
            int seatIndex,
            String team) {
        this.name = name;
        this.uuid = uuid;
        this.x = x;
        this.z = z;
        this.rot = rot;
        this.squadId = squadId;
        this.isLeader = isLeader;
        this.isDowned = isDowned;
        this.lastShoutTime = lastShoutTime;
        this.inVehicle = inVehicle;
        this.vehicleId = vehicleId;
        this.seatIndex = seatIndex;
        this.team = team;
    }

    /**
     * Обезличенная копия врага для разведки командира: только позиция и команда. Ник, UUID, отряд, лидерство,
     * нок и техника врагу не уходят (клиент рисует красную точку; ник водителя он подписал бы у машины).
     * {@code index} даёт уникальный ключ {@code ?N} — клиент хранит игроков карты по имени.
     */
    public static MapPlayerInfo anonymous(MapPlayerInfo src, int index) {
        return new MapPlayerInfo(
                "?" + index, new UUID(0L, index + 1L), src.x, src.z, 0F, -1, false, false, 0L, false, -1, 0, src.team);
    }
}
