package com.pwp.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pwp.api.core.CoreApi;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.util.ServerThread;
import com.pwp.cosmetics.network.PacketCosmeticEquipResult;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Серверная логика косметики: экипировка/снятие по запросу игрока и загрузка экипировки при входе.
 *
 * <p>Правила: игрок экипирует только то, чем владеет (ищем предмет в его инвентаре
 * core-service; core-service дополнительно проверяет владение itemUuid). Ничего не выдаётся
 * по запросу клиента. Предмет скина собирает сервер ({@link SkinCatalog}).
 * Весь HTTP — асинхронно, изменение {@link CosmeticManager} — на главном потоке.
 */
public final class CosmeticService {
    private static final Logger LOG = LoggerFactory.getLogger(CosmeticService.class);

    private CosmeticService() {}

    /** Экипировать скин, которым игрок владеет. */
    public static void equip(ServerPlayer player, String skinId, String role) {
        String uuid = player.getStringUUID();
        MinecraftServer server = player.server;
        boolean admin = PwpPermissions.isAdmin(player);
        CompletableFuture<Result> flow = CoreApi.loadPlayer(uuid).thenCompose(profile -> {
            JsonObject owned = findOwned(profile, skinId);
            if (owned == null && admin) {
                // Админ (op 2+) может надеть любой скин: выдаём ему предмет, право проверено сервером
                return CoreApi.grantItem(uuid, skinId, "admin")
                        .thenCompose(granted -> CoreApi.loadPlayer(uuid))
                        .thenCompose(fresh -> equipOwned(uuid, skinId, role, findOwned(fresh, skinId)));
            }
            return equipOwned(uuid, skinId, role, owned);
        });
        ServerThread.thenOnServer(flow, server, result -> {
            if (result.success) {
                CosmeticManager.setEquipmentSingle(player.getUUID(), result.slotType, role, skinId,
                        SkinCatalog.buildItem(skinId, result.modelPath));
            }
            reply(player, result);
        });
    }

    private static CompletableFuture<Result> equipOwned(String uuid, String skinId, String role, JsonObject owned) {
        if (owned == null) {
            return CompletableFuture.completedFuture(Result.fail("Этот скин вам не принадлежит"));
        }
        String slotType = owned.get("slotType").getAsString();
        String itemUuid = owned.get("itemUuid").getAsString();
        return CoreApi.equipItem(uuid, itemUuid, slotType, role)
                .thenCompose(resp -> {
                    if (!CoreApi.isSuccess(resp)) {
                        return CompletableFuture.completedFuture(Result.fail("core-service отклонил экипировку"));
                    }
                    return SkinCatalog.modelPaths().thenCombine(CoreApi.loadPlayer(uuid),
                            (paths, fresh) -> Result.ok(slotType, paths.get(skinId), fresh));
                });
    }

    /** Снять скин со слота. */
    public static void unequip(ServerPlayer player, String slotType, String role) {
        String uuid = player.getStringUUID();
        CompletableFuture<Result> flow = CoreApi.unequipItem(uuid, slotType, role)
                .thenCompose(resp -> CoreApi.loadPlayer(uuid))
                .thenApply(fresh -> Result.ok(slotType, null, fresh));
        ServerThread.thenOnServer(flow, player.server, result -> {
            CosmeticManager.removeEquipment(player.getUUID(), slotType, role);
            reply(player, result);
        });
    }

    /** Загрузка экипированных скинов игрока при входе на сервер. */
    public static void loadForPlayer(ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        CompletableFuture<Loaded> flow = CoreApi.loadPlayer(playerUuid.toString())
                .thenCombine(SkinCatalog.modelPaths(), (profile, paths) -> new Loaded(cosmeticsOf(profile), paths));
        ServerThread.thenOnServer(flow, player.server, loaded -> {
            if (loaded.cosmetics == null) return; // core-service недоступен — экипировку не трогаем
            CosmeticManager.clearPlayer(playerUuid);
            for (JsonElement e : loaded.cosmetics) {
                JsonObject c = e.getAsJsonObject();
                if (!c.has("equipped") || !c.get("equipped").getAsBoolean()) continue;
                String skinId = c.get("skinId").getAsString();
                String slotType = c.get("slotType").getAsString();
                CosmeticManager.setEquipment(playerUuid, slotType, "ALL", skinId,
                        SkinCatalog.buildItem(skinId, loaded.modelPaths.get(skinId)));
            }
        });
    }

    private record Loaded(JsonArray cosmetics, java.util.Map<String, String> modelPaths) {}

    private static void reply(ServerPlayer player, Result result) {
        if (player.hasDisconnected()) return;
        CosmeticsMod.NETWORK.send(PacketDistributor.PLAYER.with(() -> player),
                new PacketCosmeticEquipResult(result.success, result.message,
                        result.profile != null ? result.profile.toString() : ""));
    }

    private static JsonArray cosmeticsOf(JsonObject profile) {
        JsonObject data = CoreApi.dataObject(profile);
        if (data == null || !data.has("cosmetics") || !data.get("cosmetics").isJsonArray()) return null;
        return data.getAsJsonArray("cosmetics");
    }

    /** Предмет инвентаря игрока с данным skinId или null, если не владеет. */
    static JsonObject findOwned(JsonObject profile, String skinId) {
        JsonArray cosmetics = cosmeticsOf(profile);
        if (cosmetics == null || skinId == null) return null;
        for (JsonElement e : cosmetics) {
            JsonObject c = e.getAsJsonObject();
            if (c.has("skinId") && skinId.equals(c.get("skinId").getAsString())
                    && c.has("itemUuid") && c.has("slotType")) {
                return c;
            }
        }
        return null;
    }

    private record Result(boolean success, String message, String slotType, String modelPath, JsonObject profile) {
        static Result ok(String slotType, String modelPath, JsonObject profile) {
            return new Result(true, "", slotType, modelPath, profile);
        }

        static Result fail(String message) {
            LOG.debug("Экипировка отклонена: {}", message);
            return new Result(false, message, null, null, null);
        }
    }
}
