package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreserver.CoreServerApi;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketRequestData {
    private static final Gson GSON = new Gson();
    private final String dataType;
    private final String params;

    public PacketRequestData(String dataType, String params) {
        this.dataType = dataType;
        this.params = params;
    }

    public static void encode(PacketRequestData msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.dataType);
        buf.writeUtf(msg.params);
    }

    public static PacketRequestData decode(FriendlyByteBuf buf) {
        return new PacketRequestData(buf.readUtf(), buf.readUtf());
    }

     public static void handle(PacketRequestData msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            String jsonResult = "{}";
            try {
               switch (msg.dataType) {
                  case "profile": {
                      JsonObject profile = CoreServerApi.loadPlayer(player.getStringUUID());
                      jsonResult = profile != null ? profile.toString() : "{}";
                      break;
                  }
                  case "leaderboard": {
                      JsonObject params = parseJson(msg.params);
                      String orderBy = params.has("orderBy") ? params.get("orderBy").getAsString() : "kills";
                      int page = params.has("page") ? params.get("page").getAsInt() : 1;
                      JsonObject result = CoreServerApi.getLeaderboard(orderBy, page, 20);
                      jsonResult = result != null ? result.toString() : "{}";
                      break;
                  }
                  case "skins": {
                      JsonObject skins = CoreServerApi.getSkins();
                      jsonResult = skins != null ? skins.toString() : "{\"data\":[]}";
                      break;
                  }
                  case "factions": {
                      JsonObject factions = CoreServerApi.getFactions();
                      jsonResult = factions != null ? factions.toString() : "{\"data\":[]}";
                      break;
                  }
                  case "factionVehicles": {
                      JsonObject params = parseJson(msg.params);
                      String faction = params.has("faction") ? params.get("faction").getAsString() : "";
                      if (!faction.isEmpty()) {
                          JsonObject vehicles = CoreServerApi.getFactionVehicles(faction);
                          jsonResult = vehicles != null ? vehicles.toString() : "{\"data\":[]}";
                      }
                      break;
                  }
                  case "factionVehicle": {
                      JsonObject params = parseJson(msg.params);
                      String faction = params.has("faction") ? params.get("faction").getAsString() : "";
                      String vehicle = params.has("vehicle") ? params.get("vehicle").getAsString() : "";
                      if (!faction.isEmpty() && !vehicle.isEmpty()) {
                          JsonObject result = CoreServerApi.getFactionVehicle(faction, vehicle);
                          jsonResult = result != null ? result.toString() : "{\"data\":{}}";
                      }
                      break;
                  }
                   case "factionKits": {
                       JsonObject params = parseJson(msg.params);
                       String faction = params.has("faction") ? params.get("faction").getAsString() : "";
                       if (!faction.isEmpty()) {
                           WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                           JsonArray arr = new JsonArray();
                           Map<String, WarfareWorldData.KitInfo> target =
                               faction.equalsIgnoreCase(data.blueFaction) ? data.blueKits : data.redKits;
                           if (target != null) {
                               for (WarfareWorldData.KitInfo k : target.values()) {
                                   JsonObject entry = new JsonObject();
                                   entry.addProperty("kitName", k.name);
                                   entry.addProperty("category", k.category);
                                   entry.addProperty("description", k.description);
                                   arr.add(entry);
                               }
                           }
                           jsonResult = "{\"data\":" + arr.toString() + "}";
                       }
                       break;
                   }
               }
            } catch (Exception e) {
               jsonResult = "{\"data\":[]}";
            }

            PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketSendData(msg.dataType, jsonResult)
            );
        });
        ctx.get().setPacketHandled(true);
    }

    private static JsonObject parseJson(String s) {
        try { return GSON.fromJson(s, JsonObject.class); }
        catch (Exception e) { return new JsonObject(); }
    }
}
