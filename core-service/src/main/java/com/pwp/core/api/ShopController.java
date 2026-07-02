package com.pwp.core.api;

import com.pwp.core.db.ShopRepository;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;

public class ShopController {

    public ShopController(Javalin app) {
        app.get("/api/v1/shop", ctx -> {
            var items = ShopRepository.getAllEnabled();
            ctx.json(ApiResponse.ok(items));
        });
    }
}
