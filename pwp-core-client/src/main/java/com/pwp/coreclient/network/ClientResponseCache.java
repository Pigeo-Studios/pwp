package com.pwp.coreclient.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ClientResponseCache {
    public static volatile JsonArray skinsData = null;
    public static volatile JsonArray factionsData = null;
    public static volatile JsonArray factionVehiclesData = null;
    public static volatile JsonObject factionVehicleDetail = null;
    public static volatile JsonObject leaderboardData = null;
    /**
     * Слушатель ответов на админ-действия (PacketAction): получает {@code {action, success, error}}
     * на клиентском потоке. Ставит открытый экран-редактор, снимает при закрытии.
     */
    public static volatile java.util.function.Consumer<JsonObject> actionResultListener = null;
}
