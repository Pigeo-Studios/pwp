package com.pwp.coreclient.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ClientResponseCache {
    public static volatile JsonArray skinsData = null;
    public static volatile JsonArray factionsData = null;
    public static volatile JsonArray factionVehiclesData = null;
    public static volatile JsonObject factionVehicleDetail = null;
    public static volatile JsonObject leaderboardData = null;
}
