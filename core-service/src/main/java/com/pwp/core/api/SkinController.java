package com.pwp.core.api;

import com.pwp.core.db.SkinRepository;
import com.pwp.core.model.ApiResponse;
import com.pwp.core.model.SkinDefinition;
import io.javalin.Javalin;

public class SkinController {

    public SkinController(Javalin app) {
        app.get("/api/v1/skins", ctx -> {
            ctx.json(ApiResponse.ok(SkinRepository.getAllEnabled()));
        });

        app.get("/api/v1/skins/slot/{slotType}", ctx -> {
            String slotType = ctx.pathParam("slotType");
            ctx.json(ApiResponse.ok(SkinRepository.getBySlotType(slotType)));
        });

        app.get("/api/v1/skins/weapon/{weaponTag}", ctx -> {
            String weaponTag = ctx.pathParam("weaponTag");
            ctx.json(ApiResponse.ok(SkinRepository.getByWeaponTag(weaponTag)));
        });

        app.post("/api/v1/skins/save", ctx -> {
            SkinDefinition skin = ctx.bodyAsClass(SkinDefinition.class);
            SkinRepository.save(skin);
            ctx.json(ApiResponse.ok("saved"));
        });

        app.post("/api/v1/skins/delete", ctx -> {
            DeleteRequest req = ctx.bodyAsClass(DeleteRequest.class);
            SkinRepository.delete(req.skinId);
            ctx.json(ApiResponse.ok("deleted"));
        });
    }

    private static class DeleteRequest {
        public String skinId;
    }
}
