/*
 * Decompiled with CFR 0.152.
 */
package com.example.aas.network;

import java.util.UUID;

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

    public MapPlayerInfo(String name, UUID uuid, double x, double z, float rot, int squadId, boolean isLeader, boolean isDowned, long lastShoutTime, boolean inVehicle, int vehicleId, int seatIndex, String team) {
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
}

