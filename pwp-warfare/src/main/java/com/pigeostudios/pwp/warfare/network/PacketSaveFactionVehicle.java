package com.pigeostudios.pwp.warfare.network;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.coreserver.CoreServerApi;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent.Context;
import net.minecraftforge.registries.ForgeRegistries;

public class PacketSaveFactionVehicle {
    private final String faction;
    private final String vehicleName;
    private final String displayName;
    private final String vehicleId;
    private final float yaw;
    private final int respawnTime;
    private final int initialTime;
    private final String category;
    private final ItemStack[] inventory;

    public PacketSaveFactionVehicle(String faction, String vehicleName, String displayName,
                                    String vehicleId, float yaw, int respawnTime, int initialTime,
                                    String category, ItemStack[] inventory) {
        this.faction = faction;
        this.vehicleName = vehicleName;
        this.displayName = displayName;
        this.vehicleId = vehicleId;
        this.yaw = yaw;
        this.respawnTime = respawnTime;
        this.initialTime = initialTime;
        this.category = category;
        this.inventory = inventory;
    }

    public static void encode(PacketSaveFactionVehicle msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.faction);
        buf.writeUtf(msg.vehicleName);
        buf.writeUtf(msg.displayName != null ? msg.displayName : "");
        buf.writeUtf(msg.vehicleId != null ? msg.vehicleId : "");
        buf.writeFloat(msg.yaw);
        buf.writeInt(msg.respawnTime);
        buf.writeInt(msg.initialTime);
        buf.writeUtf(msg.category != null ? msg.category : "");
            for (int i = 0; i < 33; i++) {
                buf.writeItem(msg.inventory[i]);
            }
    }

    public static PacketSaveFactionVehicle decode(FriendlyByteBuf buf) {
        String faction = buf.readUtf();
        String vehicleName = buf.readUtf();
        String displayName = buf.readUtf();
        String vehicleId = buf.readUtf();
        float yaw = buf.readFloat();
        int respawnTime = buf.readInt();
        int initialTime = buf.readInt();
        String category = buf.readUtf();
        ItemStack[] inv = new ItemStack[33];
        for (int i = 0; i < 33; i++) {
            inv[i] = buf.readItem();
        }
        return new PacketSaveFactionVehicle(faction, vehicleName, displayName, vehicleId, yaw, respawnTime, initialTime, category, inv);
    }

    public static void handle(PacketSaveFactionVehicle msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null && player.isCreative()) {
                JsonArray itemsArray = new JsonArray();
                for (int i = 0; i < 33; i++) {
                    ItemStack stack = msg.inventory[i];
                    if (!stack.isEmpty()) {
                        JsonObject itemJson = new JsonObject();
                        itemJson.addProperty("slot", i);
                        JsonObject itemData = new JsonObject();
                        ResourceLocation registryName = ForgeRegistries.ITEMS.getKey(stack.getItem());
                        itemData.addProperty("id", registryName != null ? registryName.toString() : "minecraft:air");
                        itemData.addProperty("Count", stack.getCount());
                        if (stack.hasTag()) {
                            try {
                                itemData.add("tag", PacketSaveFactionKit.nbtToJson(stack.getTag()));
                            } catch (Exception ignored) {}
                        }
                        itemJson.add("item", itemData);
                        itemsArray.add(itemJson);
                    }
                }

                JsonObject payload = new JsonObject();
                payload.addProperty("vehicleName", msg.vehicleName);
                payload.addProperty("displayName", msg.displayName);
                payload.addProperty("vehicleId", msg.vehicleId);
                payload.addProperty("yaw", msg.yaw);
                payload.addProperty("respawnTime", msg.respawnTime);
                payload.addProperty("initialTime", msg.initialTime);
                payload.addProperty("category", msg.category);
                payload.addProperty("inventory", itemsArray.toString());

                CoreServerApi.saveFactionVehicle(msg.faction, msg.vehicleName, payload);
                player.displayClientMessage(Component.translatable("gui.pwpwarfare.faction_vehicle.saved"), true);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
