package com.pwp.coreclient.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pwp.coreserver.CoreServerApi;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.network.PacketDistributor;

public class PacketDataRequest {
    private static final Gson GSON = new Gson();
    private final String type;
    private final String paramsJson;

    public PacketDataRequest(String type, String paramsJson) {
        this.type = type;
        this.paramsJson = paramsJson;
    }

    public static void encode(PacketDataRequest msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.type);
        buf.writeUtf(msg.paramsJson);
    }

    public static PacketDataRequest decode(FriendlyByteBuf buf) {
        return new PacketDataRequest(buf.readUtf(), buf.readUtf());
    }

    public static void handle(PacketDataRequest msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            String result = fetchData(msg.type, msg.paramsJson, player);

            PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketDataResponse(msg.type, result));
        });
        ctx.get().setPacketHandled(true);
    }

    private static String fetchData(String type, String params, ServerPlayer player) {
        try {
            switch (type) {
                case "profile": {
                    JsonObject data = CoreServerApi.loadPlayer(player.getStringUUID());
                    return data != null ? data.toString() : "{}";
                }
                case "skins": {
                    JsonObject data = CoreServerApi.getSkins();
                    return data != null ? data.toString() : "{}";
                }
                case "leaderboard": {
                    JsonObject p = GSON.fromJson(params, JsonObject.class);
                    String orderBy = p.has("orderBy") ? p.get("orderBy").getAsString() : "kills";
                    int page = p.has("page") ? p.get("page").getAsInt() : 1;
                    JsonObject data = CoreServerApi.getLeaderboard(orderBy, page, 20);
                    return data != null ? data.toString() : "{}";
                }
                case "factions": {
                    JsonObject data = CoreServerApi.getFactions();
                    return data != null ? data.toString() : "{}";
                }
                case "factionVehicles": {
                    JsonObject p = GSON.fromJson(params, JsonObject.class);
                    String faction = p.has("faction") ? p.get("faction").getAsString() : "";
                    if (!faction.isEmpty()) {
                        JsonObject data = CoreServerApi.getFactionVehicles(faction);
                        return data != null ? data.toString() : "{}";
                    }
                    return "{}";
                }
                case "factionVehicle": {
                    JsonObject p = GSON.fromJson(params, JsonObject.class);
                    String faction = p.has("faction") ? p.get("faction").getAsString() : "";
                    String vehicle = p.has("vehicle") ? p.get("vehicle").getAsString() : "";
                    if (!faction.isEmpty() && !vehicle.isEmpty()) {
                        JsonObject data = CoreServerApi.getFactionVehicle(faction, vehicle);
                        return data != null ? data.toString() : "{}";
                    }
                    return "{}";
                }
                default:
                    return "{}";
            }
        } catch (Exception e) {
            return "{}";
        }
    }
}
