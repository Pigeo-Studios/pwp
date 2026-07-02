package com.pwp.core.api;

import com.pwp.core.db.CosmeticsRepository;
import com.pwp.core.db.LogRepository;
import com.pwp.core.db.ShopRepository;
import com.pwp.core.model.*;
import io.javalin.Javalin;
import io.javalin.http.NotFoundResponse;

import java.util.List;

public class CosmeticsController {

    public CosmeticsController(Javalin app) {
        app.get("/api/v1/cosmetics/{uuid}", ctx -> {
            String uuid = ctx.pathParam("uuid");
            List<CosmeticItem> items = CosmeticsRepository.getInventory(uuid);
            ctx.json(ApiResponse.ok(items));
        });

        app.post("/api/v1/cosmetics/equip", ctx -> {
            EquipRequest req = ctx.bodyAsClass(EquipRequest.class);
            boolean ok = CosmeticsRepository.equipItem(req.uuid, req.itemUuid, req.slotType, req.role);
            if (ok) {
                LogRepository.log(req.uuid, "COSMETIC_EQUIP", 0,
                        "{\"item_uuid\":\"" + req.itemUuid + "\",\"slot\":\"" + req.slotType + "\"}");
                ctx.json(ApiResponse.ok("equipped"));
            } else {
                ctx.json(ApiResponse.error("failed to equip"));
            }
        });

        app.post("/api/v1/cosmetics/grant", ctx -> {
            GrantRequest req = ctx.bodyAsClass(GrantRequest.class);

            ShopItem shopItem = ShopRepository.findBySkinId(req.skinId);
            if (shopItem == null) {
                ctx.json(ApiResponse.error("skin not found in shop"));
                return;
            }

            CosmeticItem item = CosmeticsRepository.grantItem(
                    req.uuid, req.skinId, shopItem.slotType, shopItem.rarity, req.source
            );
            LogRepository.log(req.uuid, "COSMETIC_GRANT", 0,
                    "{\"skin_id\":\"" + req.skinId + "\",\"source\":\"" + req.source + "\"}");
            ctx.json(ApiResponse.ok(item));
        });

        app.get("/api/v1/equipment/{uuid}", ctx -> {
            String uuid = ctx.pathParam("uuid");
            List<EquipmentSlot> equipment = CosmeticsRepository.getEquipment(uuid);
            ctx.json(ApiResponse.ok(equipment));
        });
    }

    private static class EquipRequest { public String uuid; public String itemUuid; public String slotType; public String role; }
    private static class GrantRequest { public String uuid; public String skinId; public String source; }
}
