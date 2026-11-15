package com.pwp.cosmetics.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.pwp.coreclient.PlayerData;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Клиентская сторона ответа на экипировку: обновляет профиль и уведомляет открытый экран. */
@OnlyIn(Dist.CLIENT)
public final class CosmeticClientState {
    private static final Logger LOG = LoggerFactory.getLogger(CosmeticClientState.class);

    /** Слушатель результата (экран инвентаря скинов); null, если экран закрыт. */
    private static Consumer<Result> listener;

    private CosmeticClientState() {}

    public record Result(boolean success, String message) {}

    public static void setListener(Consumer<Result> l) {
        listener = l;
    }

    public static void onEquipResult(boolean success, String message, String profileJson) {
        Minecraft mc = Minecraft.getInstance();
        if (!profileJson.isEmpty() && mc.player != null) {
            try {
                JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                if (profile.has("data") && profile.get("data").isJsonObject()) {
                    PlayerData.put(mc.player.getUUID(), profile.getAsJsonObject("data"));
                }
            } catch (JsonParseException | IllegalStateException e) {
                LOG.warn("Некорректный профиль в ответе экипировки", e);
            }
        }
        if (listener != null) listener.accept(new Result(success, message));
    }
}
