package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent.Context;

// Пакет сброса ящика снабжения из грузовика снабжения
// Отправляется водителем грузовика при нажатии кнопки сброса
public class PacketDropCrate {
    public static void encode(PacketDropCrate msg, FriendlyByteBuf buf) {}

    public static PacketDropCrate decode(FriendlyByteBuf buf) {
        return new PacketDropCrate();
    }

    // Проверяет, находится ли игрок в грузовике снабжения, и создаёт ящик
    public static void handle(PacketDropCrate msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                Entity vehicle = player.getVehicle();
                if (vehicle != null && WarfareData.of(vehicle).getBoolean(WarfareKeys.IS_SUPPLY_TRUCK)) {
                    if (vehicle.getFirstPassenger() != player) {
                        player.displayClientMessage(
                                Component.translatable("pwpwarfare.msg.crate.driver_only")
                                        .withStyle(ChatFormatting.RED),
                                true);
                        return;
                    }

                    int ammo = WarfareData.of(vehicle).getInt(WarfareKeys.SUPPLY_AMMO);
                    if (ammo > 0) {
                        WarfareData.of(vehicle).putInt(WarfareKeys.SUPPLY_AMMO, ammo - 1);
                        String team = WarfareData.of(vehicle).getString(WarfareKeys.VEHICLE_TEAM);
                        if (team.isEmpty()) {
                            team = "NEUTRAL";
                        }

                        double yawRad = Math.toRadians(vehicle.getYRot());
                        double x = vehicle.getX() + Math.sin(yawRad) * 2.5;
                        double z = vehicle.getZ() - Math.cos(yawRad) * 2.5;
                        double y = vehicle.getY() + 1.5;
                        SupplyCrateEntity crate =
                                new SupplyCrateEntity(player.level(), x, y, z, team, player.getUUID());
                        player.level().addFreshEntity(crate);
                        player.displayClientMessage(
                                Component.translatable("pwpwarfare.msg.crate.dropped")
                                        .withStyle(ChatFormatting.YELLOW),
                                true);
                    } else {
                        player.displayClientMessage(
                                Component.translatable("pwpwarfare.msg.crate.empty")
                                        .withStyle(ChatFormatting.RED),
                                true);
                    }
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
