package com.pwp.coreclient.network.data;

import com.google.gson.JsonObject;
import com.pwp.api.core.CoreApi;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.security.PwpPermissions;
import com.pwp.api.util.RequestGuard;
import com.pwp.api.util.ServerThread;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Админ-действия редакторов (op 2+) — по пакету на действие, поля типизированы и проверяются
 * сервером. Раньше был один PacketAction(action, paramsJson) с разбором JSON на сервере.
 * Ответ — {@link DataResponse} типа {@link DataRequests#ACTION_RESULT}.
 */
public final class AdminAction {
    private static final Logger LOG = LoggerFactory.getLogger(AdminAction.class);
    /** Один запрос на действие за раз, не чаще раза в секунду. */
    private static final RequestGuard GUARD = new RequestGuard(1000);

    public static final String SAVE_SKIN = "saveSkin";
    public static final String DELETE_SKIN = "deleteSkin";
    public static final String DELETE_FACTION_VEHICLE = "deleteFactionVehicle";

    private AdminAction() {}

    /** Сохранить/создать скин в каталоге. */
    public record SaveSkin(
            String skinId,
            String name,
            String description,
            String slotType,
            String weaponTag,
            String rarity,
            String modelPath)
            implements Net.C2S {
        public static void encode(SaveSkin m, FriendlyByteBuf b) {
            b.writeUtf(m.skinId, 64);
            b.writeUtf(m.name, 64);
            b.writeUtf(m.description, 512);
            b.writeUtf(m.slotType, 32);
            b.writeUtf(m.weaponTag, 128);
            b.writeUtf(m.rarity, 32);
            b.writeUtf(m.modelPath, 256);
        }

        public static SaveSkin decode(FriendlyByteBuf b) {
            return new SaveSkin(
                    b.readUtf(64),
                    b.readUtf(64),
                    b.readUtf(512),
                    b.readUtf(32),
                    b.readUtf(128),
                    b.readUtf(32),
                    b.readUtf(256));
        }

        @Override
        public String validate() {
            return Validate.first(
                    Validate.id("skinId", skinId, 64),
                    Validate.text("name", name, 64),
                    Validate.optionalText("description", description, 512),
                    Validate.id("slotType", slotType, 32),
                    Validate.optionalText("weaponTag", weaponTag, 128),
                    Validate.id("rarity", rarity, 32),
                    Validate.id("modelPath", modelPath, 256));
        }

        @Override
        public void handle(ServerPlayer sender) {
            run(
                    sender,
                    SAVE_SKIN,
                    () -> CoreApi.saveSkin(skinId, name, description, slotType, weaponTag, rarity, modelPath));
        }
    }

    /** Удалить скин из каталога. */
    public record DeleteSkin(String skinId) implements Net.C2S {
        public static void encode(DeleteSkin m, FriendlyByteBuf b) {
            b.writeUtf(m.skinId, 64);
        }

        public static DeleteSkin decode(FriendlyByteBuf b) {
            return new DeleteSkin(b.readUtf(64));
        }

        @Override
        public String validate() {
            return Validate.id("skinId", skinId, 64);
        }

        @Override
        public void handle(ServerPlayer sender) {
            run(sender, DELETE_SKIN, () -> CoreApi.deleteSkin(skinId));
        }
    }

    /** Удалить технику из пула фракции. */
    public record DeleteFactionVehicle(String faction, String vehicleName) implements Net.C2S {
        public static void encode(DeleteFactionVehicle m, FriendlyByteBuf b) {
            b.writeUtf(m.faction, 64);
            b.writeUtf(m.vehicleName, 128);
        }

        public static DeleteFactionVehicle decode(FriendlyByteBuf b) {
            return new DeleteFactionVehicle(b.readUtf(64), b.readUtf(128));
        }

        @Override
        public String validate() {
            return Validate.first(Validate.id("faction", faction, 64), Validate.text("vehicleName", vehicleName, 128));
        }

        @Override
        public void handle(ServerPlayer sender) {
            run(sender, DELETE_FACTION_VEHICLE, () -> CoreApi.deleteFactionVehicle(faction, vehicleName));
        }
    }

    /** Общий путь: право op 2+ на сервере → антиспам → HTTP в фоне → ответ игроку (по UUID). */
    private static void run(ServerPlayer player, String action, Supplier<CompletableFuture<JsonObject>> call) {
        if (!PwpPermissions.isAdmin(player)) {
            LOG.warn(
                    "Игрок {} без прав прислал админ-действие {}",
                    player.getGameProfile().getName(),
                    action);
            reply(player, action, false, "Недостаточно прав");
            return;
        }
        boolean started = ServerThread.guardedForPlayer(GUARD, action, player, call, (online, resp) -> {
            boolean ok = CoreApi.isSuccess(resp);
            String error =
                    resp != null && resp.has("error") && !resp.get("error").isJsonNull()
                            ? resp.get("error").getAsString()
                            : "core-service недоступен";
            reply(online, action, ok, ok ? "" : error);
        });
        if (!started) reply(player, action, false, "Подождите, предыдущий запрос ещё выполняется");
    }

    private static void reply(ServerPlayer player, String action, boolean success, String error) {
        JsonObject result = new JsonObject();
        result.addProperty("action", action);
        result.addProperty("success", success);
        result.addProperty("error", error);
        DataRequests.send(player, DataRequests.ACTION_RESULT, result.toString());
    }
}
