package com.pwp.core.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.pwp.core.db.NewsRepository;
import com.pwp.core.model.ApiResponse;
import io.javalin.Javalin;
import io.javalin.http.BadRequestResponse;
import java.util.Set;

/**
 * Новости (этап 17b): одна лента для сайта, лаунчера и игры.
 *
 * <ul>
 *   <li>{@code GET /api/v1/public/news?limit=} — опубликованные, закреплённые сверху, без ключа;
 *   <li>{@code GET /api/v1/admin/news} — все (и скрытые) — веб-модерация;
 *   <li>{@code POST /api/v1/admin/news} — создать; {@code PUT /api/v1/admin/news/{id}} — изменить или скрыть
 *       ({@code published=false}). Удаления нет: скрытая новость остаётся в БД.
 * </ul>
 *
 * Права {@code /api/v1/admin/*} — общий {@code verifyAdmin} (CoreApplication). Тексты сайт выводит только через
 * {@code textContent}.
 */
public class NewsController {
    static final Set<String> TAGS = Set.of("news", "update", "event", "maintenance");
    static final int TITLE_MAX = 120;
    static final int BODY_MAX = 8000;
    static final int URL_MAX = 255;

    public NewsController(Javalin app) {
        app.get("/api/v1/public/news", ctx -> {
            int limit = 20;
            try {
                limit = Math.max(1, Math.min(50, Integer.parseInt(ctx.queryParam("limit"))));
            } catch (NumberFormatException e) {
                // нет параметра — 20
            }
            ctx.json(ApiResponse.ok(NewsRepository.list(limit, false)));
        });

        app.get("/api/v1/admin/news", ctx -> ctx.json(ApiResponse.ok(NewsRepository.list(200, true))));

        app.post("/api/v1/admin/news", ctx -> {
            NewsRepository.News n = parse(body(ctx.body()));
            long id = NewsRepository.create(n, author(ctx));
            ctx.json(ApiResponse.ok(id));
        });

        app.put("/api/v1/admin/news/{id}", ctx -> {
            long id;
            try {
                id = Long.parseLong(ctx.pathParam("id"));
            } catch (NumberFormatException e) {
                throw new BadRequestResponse("bad id");
            }
            NewsRepository.News n = parse(body(ctx.body()));
            if (!NewsRepository.update(id, n)) {
                ctx.status(404).json(ApiResponse.error("news not found"));
                return;
            }
            ctx.json(ApiResponse.ok(id));
        });
    }

    private static JsonObject body(String s) {
        try {
            return JsonParser.parseString(s).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new BadRequestResponse("bad json");
        }
    }

    private static String author(io.javalin.http.Context ctx) {
        Object who = ctx.attribute("adminName");
        return who == null ? "admin" : who.toString();
    }

    private static String str(JsonObject o, String k) {
        return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString().trim() : "";
    }

    /** Проверка ввода: заголовок и текст обязательны, тег из списка, картинка — только http(s). */
    static NewsRepository.News parse(JsonObject o) {
        String title = str(o, "title");
        String body = str(o, "body");
        String tag = str(o, "tag").isEmpty() ? "news" : str(o, "tag").toLowerCase(java.util.Locale.ROOT);
        String image = str(o, "imageUrl");
        if (title.isEmpty() || title.length() > TITLE_MAX) throw new BadRequestResponse("title: 1.." + TITLE_MAX);
        if (body.isEmpty() || body.length() > BODY_MAX) throw new BadRequestResponse("body: 1.." + BODY_MAX);
        if (!TAGS.contains(tag)) throw new BadRequestResponse("tag: " + TAGS);
        if (!image.isEmpty() && (image.length() > URL_MAX || !(image.startsWith("https://") || image.startsWith("http://")))) {
            throw new BadRequestResponse("imageUrl: http(s)://");
        }
        boolean pinned = o.has("pinned") && o.get("pinned").getAsBoolean();
        boolean published = !o.has("published") || o.get("published").getAsBoolean();
        return new NewsRepository.News(title, body, tag, image, pinned, published);
    }
}
