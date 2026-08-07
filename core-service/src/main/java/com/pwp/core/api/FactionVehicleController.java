package com.pwp.core.api;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pwp.core.db.FactionVehicleRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.FactionVehicleDefinition;
import io.javalin.Javalin;
import java.util.List;

public class FactionVehicleController {

    private static final Gson GSON = new Gson();

    public FactionVehicleController(Javalin app) {
        app.get("/api/v1/factions/{faction}/vehicles", ctx -> {
            String faction = ctx.pathParam("faction").toLowerCase();
            List<FactionVehicleDefinition> vehicles = FactionVehicleRepository.getByFaction(faction);
            JsonArray arr = new JsonArray();
            for (FactionVehicleDefinition v : vehicles) arr.add(toJson(v));
            ctx.json(ApiResponse.ok(arr));
        });

        app.get("/api/v1/factions/{faction}/vehicles/{vehicleName}", ctx -> {
            String faction = ctx.pathParam("faction").toLowerCase();
            String vehicleName = ctx.pathParam("vehicleName");
            FactionVehicleDefinition v = FactionVehicleRepository.get(faction, vehicleName);
            if (v == null) {
                ctx.json(ApiResponse.error("vehicle not found"));
                return;
            }
            ctx.json(ApiResponse.ok(toJson(v)));
        });

        app.put("/api/v1/factions/{faction}/vehicles/{vehicleName}", ctx -> {
            String faction = ctx.pathParam("faction").toLowerCase();
            String vehicleName = ctx.pathParam("vehicleName");
            FactionVehicleDefinition v = ctx.bodyAsClass(FactionVehicleDefinition.class);
            v.faction = faction;
            v.vehicleName = vehicleName;
            FactionVehicleRepository.save(v);
            ctx.json(ApiResponse.ok("vehicle saved"));
        });

        app.delete("/api/v1/factions/{faction}/vehicles/{vehicleName}", ctx -> {
            String faction = ctx.pathParam("faction").toLowerCase();
            String vehicleName = ctx.pathParam("vehicleName");
            FactionVehicleRepository.delete(faction, vehicleName);
            ctx.json(ApiResponse.ok("vehicle deleted"));
        });
    }

    private static JsonObject toJson(FactionVehicleDefinition v) {
        JsonObject obj = new JsonObject();
        obj.addProperty("faction", v.faction);
        obj.addProperty("vehicleName", v.vehicleName);
        obj.addProperty("displayName", v.displayName);
        obj.addProperty("vehicleId", v.vehicleId);
        obj.addProperty("yaw", v.yaw);
        obj.addProperty("respawnTime", v.respawnTime);
        obj.addProperty("initialTime", v.initialTime);
        obj.addProperty("category", v.category);
        if (v.inventory != null && !v.inventory.isEmpty()) {
            try {
                obj.add("inventory", GSON.fromJson(v.inventory, JsonArray.class));
            } catch (Exception e) {
                obj.addProperty("inventory", v.inventory);
            }
        } else {
            obj.add("inventory", new JsonArray());
        }
        return obj;
    }
}