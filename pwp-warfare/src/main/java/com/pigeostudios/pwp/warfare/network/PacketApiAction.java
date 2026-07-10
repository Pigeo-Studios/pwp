package com.pigeostudios.pwp.warfare.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pwp.coreserver.CoreServerApi;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketApiAction {
    private static final Gson GSON = new Gson();
    private final String action;
    private final String paramsJson;

    public PacketApiAction(String action, String paramsJson) {
        this.action = action;
        this.paramsJson = paramsJson;
    }

    public static void encode(PacketApiAction msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.action);
        buf.writeUtf(msg.paramsJson);
    }

    public static PacketApiAction decode(FriendlyByteBuf buf) {
        return new PacketApiAction(buf.readUtf(), buf.readUtf());
    }

    public static void handle(PacketApiAction msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            switch (msg.action) {
                case "equipCosmetic" -> handleEquipCosmetic(player, msg.paramsJson);
                case "unequipCosmetic" -> handleUnequipCosmetic(player, msg.paramsJson);
                case "saveSkin" -> handleSaveSkin(player, msg.paramsJson);
                case "deleteSkin" -> handleDeleteSkin(player, msg.paramsJson);
                case "deleteFactionVehicle" -> handleDeleteFactionVehicle(player, msg.paramsJson);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static void handleEquipCosmetic(ServerPlayer player, String params) {
        try {
            JsonObject p = GSON.fromJson(params, JsonObject.class);
            String uuid = player.getStringUUID();
            String skinId = p.get("skinId").getAsString();
            String slotType = p.get("slotType").getAsString();
            String role = p.has("role") ? p.get("role").getAsString() : "ALL";

            CoreServerApi.grantItem(uuid, skinId, "menu");
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}

            JsonObject profile = CoreServerApi.loadPlayer(uuid);
            if (profile != null && profile.has("data")) {
                JsonObject data = profile.getAsJsonObject("data");
                if (data.has("cosmetics")) {
                    for (var e : data.getAsJsonArray("cosmetics")) {
                        JsonObject c = e.getAsJsonObject();
                        if (c.get("skinId").getAsString().equals(skinId)) {
                            CoreServerApi.equipItem(uuid, c.get("itemUuid").getAsString(), slotType, role);
                            break;
                        }
                    }
                }
            }

            profile = CoreServerApi.loadPlayer(uuid);
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new PacketSendData("profile", profile != null ? profile.toString() : "{}"));
        } catch (Exception e) {
            System.err.println("[PWP] equipCosmetic failed: " + e.getMessage());
        }
    }

    private static void handleUnequipCosmetic(ServerPlayer player, String params) {
        try {
            JsonObject p = GSON.fromJson(params, JsonObject.class);
            String uuid = player.getStringUUID();
            String slotType = p.get("slotType").getAsString();
            String role = p.has("role") ? p.get("role").getAsString() : "ALL";

            CoreServerApi.unequipItem(uuid, slotType, role);

            JsonObject profile = CoreServerApi.loadPlayer(uuid);
            PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new PacketSendData("profile", profile != null ? profile.toString() : "{}"));
        } catch (Exception e) {
            System.err.println("[PWP] unequipCosmetic failed: " + e.getMessage());
        }
    }

    private static void handleSaveSkin(ServerPlayer player, String params) {
        if (!player.isCreative()) return;
        try {
            JsonObject p = GSON.fromJson(params, JsonObject.class);
            CoreServerApi.saveSkin(
                p.get("skinId").getAsString(),
                p.get("name").getAsString(),
                p.has("description") ? p.get("description").getAsString() : "",
                p.get("slotType").getAsString(),
                p.has("weaponTag") ? p.get("weaponTag").getAsString() : "",
                p.get("rarity").getAsString(),
                p.get("modelPath").getAsString()
            );
        } catch (Exception e) {
            System.err.println("[PWP] saveSkin failed: " + e.getMessage());
        }
    }

    private static void handleDeleteSkin(ServerPlayer player, String params) {
        if (!player.isCreative()) return;
        try {
            JsonObject p = GSON.fromJson(params, JsonObject.class);
            CoreServerApi.deleteSkin(p.get("skinId").getAsString());
        } catch (Exception e) {
            System.err.println("[PWP] deleteSkin failed: " + e.getMessage());
        }
    }

    private static void handleDeleteFactionVehicle(ServerPlayer player, String params) {
        if (!player.isCreative()) return;
        try {
            JsonObject p = GSON.fromJson(params, JsonObject.class);
            CoreServerApi.deleteFactionVehicle(
                p.get("faction").getAsString(),
                p.get("vehicleName").getAsString()
            );
        } catch (Exception e) {
            System.err.println("[PWP] deleteFactionVehicle failed: " + e.getMessage());
        }
    }
}
