package com.pwp.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.pwp.api.core.CoreApi;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.util.RequestGuard;
import com.pwp.api.util.ServerThread;
import com.pwp.cosmetics.network.PacketCosmeticEquipResult;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

    /** Антиспам экипировки: один запрос экип/снятие на игрока за раз, не чаще раза в 0.5 с. */
    private static final RequestGuard GUARD = new RequestGuard(500);

    private static final String GUARD_KEY = "equip";

    private CosmeticService() {}

    /** Экипировать скин, которым игрок владеет. */
    public static void equip(ServerPlayer player, String skinId, String role) {
        String uuid = player.getStringUUID();
        boolean admin = PwpPermissions.isAdmin(player);
        if (!ServerThread.guardedForPlayer(
                GUARD,
                GUARD_KEY,
                player,
                () -> SkinCatalog.defs().thenCompose(defs -> {
                    SkinCatalog.Def def = defs.get(skinId);
                    com.pwp.api.cosmetics.WeaponSkin w = def == null ? null : def.weapon();
                    // скин ствола: один на оружие (role = GunId), на всех ролях и китах сразу
                    String r = w != null ? w.gunId() : role;
                    return equipFlow(uuid, skinId, r, admin).thenApply(res -> new Pair(res, w));
                }),
                (online, pr) -> {
                    Result result = pr.result();
                    if (result.success) {
                        if (pr.weapon() != null) {
                            WeaponSkinService.put(online.getUUID(), pr.weapon());
                            WeaponSkinService.apply(online);
                        } else {
                            CosmeticManager.setEquipmentSingle(
                                    online.getUUID(),
                                    result.slotType,
                                    role,
                                    skinId,
                                    SkinCatalog.buildItem(skinId, result.modelPath));
                        }
                    }
                    reply(online, result);
                })) {
            reply(player, Result.fail(BUSY));
        }
    }

    /** Ключи lang: клиент показывает их через I18n (неизвестную строку старого сервера — как есть). */
    private static final String BUSY = "pwp_core.skin.equip.busy";

    private record Pair(Result result, com.pwp.api.cosmetics.WeaponSkin weapon) {}

    private static CompletableFuture<Result> equipFlow(String uuid, String skinId, String role, boolean admin) {
        return CoreApi.loadPlayer(uuid).thenCompose(profile -> {
            JsonObject owned = findOwned(profile, skinId);
            if (owned == null && admin) {
                // Админ (op 2+) может надеть любой скин: выдаём ему предмет, право проверено сервером
                return CoreApi.grantItem(uuid, skinId, "admin")
                        .thenCompose(granted -> CoreApi.loadPlayer(uuid))
                        .thenCompose(fresh -> equipOwned(uuid, skinId, role, findOwned(fresh, skinId)));
            }
            return equipOwned(uuid, skinId, role, owned);
        });
    }

    private static CompletableFuture<Result> equipOwned(String uuid, String skinId, String role, JsonObject owned) {
        if (owned == null) {
            return CompletableFuture.completedFuture(Result.fail("pwp_core.skin.equip.not_owned"));
        }
        String slotType = owned.get("slotType").getAsString();
        String itemUuid = owned.get("itemUuid").getAsString();
        return CoreApi.equipItem(uuid, itemUuid, slotType, role).thenCompose(resp -> {
            if (!CoreApi.isSuccess(resp)) {
                return CompletableFuture.completedFuture(Result.fail("pwp_core.skin.equip.core_rejected"));
            }
            return SkinCatalog.modelPaths()
                    .thenCombine(
                            CoreApi.loadPlayer(uuid), (paths, fresh) -> Result.ok(slotType, paths.get(skinId), fresh));
        });
    }

    /** Снять скин со слота. */
    public static void unequip(ServerPlayer player, String slotType, String role) {
        String uuid = player.getStringUUID();
        boolean started = ServerThread.guardedForPlayer(
                GUARD,
                GUARD_KEY,
                player,
                () -> CoreApi.unequipItem(uuid, slotType, role)
                        .thenCompose(resp -> CoreApi.loadPlayer(uuid))
                        .thenApply(fresh -> Result.ok(slotType, null, fresh)),
                (online, result) -> {
                    if (com.pwp.api.cosmetics.WeaponSkin.SLOT.equals(slotType)) {
                        WeaponSkinService.remove(online.getUUID(), role);
                        WeaponSkinService.apply(online);
                    } else {
                        CosmeticManager.removeEquipment(online.getUUID(), slotType, role);
                    }
                    reply(online, result);
                });
        if (!started) reply(player, Result.fail(BUSY));
    }

    /** Загрузка экипированных скинов игрока при входе на сервер. */
    public static void loadForPlayer(ServerPlayer player) {
        UUID playerUuid = player.getUUID();
        CompletableFuture<Loaded> flow = CoreApi.loadPlayer(playerUuid.toString())
                .thenCombine(SkinCatalog.defs(), (profile, defs) -> new Loaded(cosmeticsOf(profile), defs));
        // Игрок мог выйти, пока шёл HTTP: тогда при следующем входе загрузка повторится
        ServerThread.thenForPlayer(flow, player, (online, loaded) -> {
            if (loaded.cosmetics == null) return; // core-service недоступен — экипировку не трогаем
            CosmeticManager.clearPlayer(playerUuid);
            WeaponSkinService.clear(playerUuid);
            for (JsonElement e : loaded.cosmetics) {
                JsonObject c = e.getAsJsonObject();
                if (!c.has("equipped") || !c.get("equipped").getAsBoolean()) continue;
                String skinId = c.get("skinId").getAsString();
                String slotType = c.get("slotType").getAsString();
                SkinCatalog.Def def = loaded.defs.get(skinId);
                com.pwp.api.cosmetics.WeaponSkin w = def == null ? null : def.weapon();
                if (w != null) {
                    WeaponSkinService.put(playerUuid, w);
                    continue;
                }
                CosmeticManager.setEquipment(
                        playerUuid,
                        slotType,
                        "ALL",
                        skinId,
                        SkinCatalog.buildItem(skinId, def == null ? null : def.modelPath()));
            }
            WeaponSkinService.apply(online);
        });
    }

    private record Loaded(JsonArray cosmetics, java.util.Map<String, SkinCatalog.Def> defs) {}

    private static void reply(ServerPlayer player, Result result) {
        if (player.hasDisconnected()) return;
        CosmeticsMod.NETWORK.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PacketCosmeticEquipResult(
                        result.success, result.message, result.profile != null ? result.profile.toString() : ""));
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
            if (c.has("skinId")
                    && skinId.equals(c.get("skinId").getAsString())
                    && c.has("itemUuid")
                    && c.has("slotType")) {
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
