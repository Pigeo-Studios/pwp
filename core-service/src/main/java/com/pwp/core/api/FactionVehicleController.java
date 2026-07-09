package com.pwp.core.api;

import com.pwp.core.db.FactionVehicleRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.FactionVehicleDefinition;
import io.javalin.Javalin;
import java.util.List;

public class FactionVehicleController {

    public FactionVehicleController(Javalin app) {
        app.get("/api/v1/factions/{faction}/vehicles", ctx -> {
            String faction = ctx.pathParam("faction").toLowerCase();
            List<FactionVehicleDefinition> vehicles = FactionVehicleRepository.getByFaction(faction);
            ctx.json(ApiResponse.ok(vehicles));
        });

        app.get("/api/v1/factions/{faction}/vehicles/{vehicleName}", ctx -> {
            String faction = ctx.pathParam("faction").toLowerCase();
            String vehicleName = ctx.pathParam("vehicleName");
            FactionVehicleDefinition v = FactionVehicleRepository.get(faction, vehicleName);
            if (v == null) {
                ctx.json(ApiResponse.error("vehicle not found"));
                return;
            }
            ctx.json(ApiResponse.ok(v));
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
}
