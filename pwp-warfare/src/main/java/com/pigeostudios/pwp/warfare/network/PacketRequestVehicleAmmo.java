package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет запроса пополнения кита из транспортного средства
// Использует материалы транспортного средства для оплаты
public class PacketRequestVehicleAmmo {
    private final int entityId;
    private final int type;

    public PacketRequestVehicleAmmo(int entityId, int type) {
        this.entityId = entityId;
        this.type = type;
    }

    public static void encode(PacketRequestVehicleAmmo msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.entityId);
        buf.writeInt(msg.type);
    }

    public static PacketRequestVehicleAmmo decode(FriendlyByteBuf buf) {
        return new PacketRequestVehicleAmmo(buf.readInt(), buf.readInt());
    }

    // Проверяет материалы транспорта и пополняет кит игрока
    public static void handle(PacketRequestVehicleAmmo msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Entity vehicle = player.level().getEntity(msg.entityId);
                if (vehicle != null && !(player.distanceToSqr(vehicle) > 64.0)) {
                    int currentMats = WarfareData.of(vehicle).getInt(WarfareKeys.VEHICLE_MATS);
                    int cost = (Integer) WarfareConfig.HUB_RESUPPLY_COST.get();
                    if (!player.isCreative() && currentMats < cost) {
                        player.displayClientMessage(
                                Component.literal("Недостаточно материалов в технике! (" + currentMats + ")")
                                        .withStyle(ChatFormatting.RED),
                                true);
                    } else {
                        String kitName = WarfareData.of(player).getString(WarfareKeys.CURRENT_KIT);
                        if (!kitName.isEmpty() && !kitName.equals("Unassigned")) {
                            WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                            String t = player.getTeam() != null
                                    ? player.getTeam().getName().toUpperCase()
                                    : "NEUTRAL";
                            WarfareWorldData.KitInfo kit =
                                    t.equals("BLUE") ? data.blueKits.get(kitName) : data.redKits.get(kitName);
                            if (kit != null) {
                                if (ResupplyHandler.resupplyPlayer(player, kit, false)) {
                                    int newMats = currentMats;
                                    if (!player.isCreative()) {
                                        newMats = currentMats - cost;
                                        WarfareData.of(vehicle).putInt(WarfareKeys.VEHICLE_MATS, newMats);
                                    }

                                    player.displayClientMessage(
                                            Component.literal("Кит пополнен! Материалы техники: " + newMats)
                                                    .withStyle(ChatFormatting.GREEN),
                                            true);
                                    player.level()
                                            .playSound(
                                                    null,
                                                    player.blockPosition(),
                                                    SoundEvents.ITEM_PICKUP,
                                                    SoundSource.PLAYERS,
                                                    1.0F,
                                                    1.0F);
                                } else {
                                    player.displayClientMessage(
                                            Component.literal("БК уже полон! Материалы техники: " + currentMats)
                                                    .withStyle(ChatFormatting.YELLOW),
                                            true);
                                }
                            }
                        } else {
                            player.displayClientMessage(
                                    Component.literal("Кит не надет!").withStyle(ChatFormatting.RED), true);
                        }
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
