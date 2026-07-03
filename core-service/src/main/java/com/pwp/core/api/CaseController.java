package com.pwp.core.api;

import com.pwp.core.db.CaseRepository;
import com.pwp.core.db.CosmeticsRepository;
import com.pwp.core.db.CurrencyRepository;
import com.pwp.core.db.LogRepository;
import com.pwp.core.db.SkinRepository;
import com.pwp.core.model.*;
import io.javalin.Javalin;

public class CaseController {

    public CaseController(Javalin app) {
        app.get("/api/v1/cases", ctx -> {
            ctx.json(ApiResponse.ok(CaseRepository.getAllEnabled()));
        });

        app.get("/api/v1/cases/{caseId}", ctx -> {
            String caseId = ctx.pathParam("caseId");
            CaseDefinition cd = CaseRepository.findById(caseId);
            if (cd == null) {
                ctx.json(ApiResponse.error("case not found"));
                return;
            }
            ctx.json(ApiResponse.ok(cd));
        });

        app.post("/api/v1/cases/save", ctx -> {
            CaseDefinition cd = ctx.bodyAsClass(CaseDefinition.class);
            CaseRepository.saveCase(cd);
            ctx.json(ApiResponse.ok("saved"));
        });

        app.post("/api/v1/cases/open", ctx -> {
            OpenRequest req = ctx.bodyAsClass(OpenRequest.class);

            CaseDefinition cd = CaseRepository.findById(req.caseId);
            if (cd == null) {
                ctx.json(ApiResponse.error("case not found"));
                return;
            }

            // Check balance
            long balance = CurrencyRepository.getBalance(req.uuid);
            if (balance < cd.priceCoins) {
                ctx.json(ApiResponse.error("insufficient funds"));
                return;
            }

            // Spend coins
            boolean spent = CurrencyRepository.spend(req.uuid, cd.priceCoins);
            if (!spent) {
                ctx.json(ApiResponse.error("insufficient funds"));
                return;
            }

            // Roll for skin
            String skinId = CaseRepository.openCase(req.caseId);
            if (skinId == null) {
                CurrencyRepository.add(req.uuid, cd.priceCoins); // refund
                ctx.json(ApiResponse.error("case is empty"));
                return;
            }

            // Get skin details
            SkinDefinition skin = SkinRepository.findById(skinId);
            if (skin == null) {
                CurrencyRepository.add(req.uuid, cd.priceCoins); // refund
                ctx.json(ApiResponse.error("skin not found"));
                return;
            }

            // Grant skin to player
            CosmeticItem item = CosmeticsRepository.grantItem(req.uuid, skinId, skin.slotType, skin.rarity, "CASE");
            LogRepository.log(req.uuid, "CASE_OPEN", -cd.priceCoins,
                    "{\"case_id\":\"" + req.caseId + "\",\"skin_id\":\"" + skinId + "\"}");

            OpenResult result = new OpenResult();
            result.skinId = skinId;
            result.skinName = skin.name;
            result.slotType = skin.slotType;
            result.rarity = skin.rarity;
            result.itemUuid = item.itemUuid;
            ctx.json(ApiResponse.ok(result));
        });
    }

    private static class OpenRequest {
        public String uuid;
        public String caseId;
    }

    private static class OpenResult {
        public String skinId;
        public String skinName;
        public String slotType;
        public String rarity;
        public String itemUuid;
    }
}
