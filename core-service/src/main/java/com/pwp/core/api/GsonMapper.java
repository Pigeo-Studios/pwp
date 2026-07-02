package com.pwp.core.api;

import com.google.gson.Gson;
import io.javalin.json.JsonMapper;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.lang.reflect.Type;

public class GsonMapper implements JsonMapper {

    private final Gson gson = new Gson();

    @NotNull
    @Override
    public <T> T fromJsonStream(@NotNull InputStream json, @NotNull Type targetType) {
        return gson.fromJson(new java.io.InputStreamReader(json, java.nio.charset.StandardCharsets.UTF_8), targetType);
    }

    @NotNull
    @Override
    public <T> T fromJsonString(@NotNull String json, @NotNull Type targetType) {
        return gson.fromJson(json, targetType);
    }

    @NotNull
    @Override
    public String toJsonString(@NotNull Object obj, @NotNull Type targetType) {
        return gson.toJson(obj, targetType);
    }

    public static void apply(io.javalin.config.JavalinConfig config) {
        config.jsonMapper(new GsonMapper());
    }
}
