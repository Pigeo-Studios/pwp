package com.pwp.core.api;

import com.pwp.core.db.KitRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.KitDefinition;
import io.javalin.Javalin;
import java.util.List;

public class KitController {

    public KitController(Javalin app) {
        app.get("/api/v1/kits/factions", ctx -> {
            List<String> factions = KitRepository.getFactions();
            ctx.json(ApiResponse.ok(factions));
        });

        app.get("/api/v1/kits/faction/{faction}", ctx -> {
            String faction = ctx.pathParam("faction").toLowerCase();
            List<KitDefinition> kits = KitRepository.getByFaction(faction);
            ctx.json(ApiResponse.ok(kits));
        });

        app.get("/api/v1/kits/faction/{faction}/{kitName}", ctx -> {
            String faction = ctx.pathParam("faction").toLowerCase();
            String kitName = ctx.pathParam("kitName");
            KitDefinition kit = KitRepository.get(faction, kitName);
            if (kit == null) {
                ctx.json(ApiResponse.error("kit not found"));
                return;
            }
            ctx.json(ApiResponse.ok(kit));
        });

        app.put("/api/v1/kits/faction/{faction}/{kitName}", ctx -> {
            String faction = ctx.pathParam("faction").toLowerCase();
            String kitName = ctx.pathParam("kitName");
            KitDefinition kit = ctx.bodyAsClass(KitDefinition.class);
            kit.faction = faction;
            kit.kitName = kitName;
            KitRepository.save(kit);
            ctx.json(ApiResponse.ok("kit saved"));
        });

        app.delete("/api/v1/kits/faction/{faction}/{kitName}", ctx -> {
            String faction = ctx.pathParam("faction").toLowerCase();
            String kitName = ctx.pathParam("kitName");
            KitRepository.delete(faction, kitName);
            ctx.json(ApiResponse.ok("kit deleted"));
        });

        app.post("/api/v1/kits/faction/{faction}/bulk", ctx -> {
            String faction = ctx.pathParam("faction").toLowerCase();
            KitDefinition[] kits = ctx.bodyAsClass(KitDefinition[].class);
            for (KitDefinition k : kits) {
                k.faction = faction;
                KitRepository.save(k);
            }
            ctx.json(ApiResponse.ok(kits.length + " kits saved for faction " + faction));
        });
    }
}
