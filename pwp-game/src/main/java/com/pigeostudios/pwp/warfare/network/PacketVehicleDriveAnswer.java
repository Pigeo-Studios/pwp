package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.util.PlayerFeedback;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketVehicleDriveAnswer {
    private final boolean accept;
    private final int vehicleId;
    private final UUID requesterUUID;

    public PacketVehicleDriveAnswer(boolean accept, int vehicleId, UUID requesterUUID) {
        this.accept = accept;
        this.vehicleId = vehicleId;
        this.requesterUUID = requesterUUID;
    }

    public static void encode(PacketVehicleDriveAnswer msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.accept);
        buf.writeInt(msg.vehicleId);
        buf.writeUUID(msg.requesterUUID);
    }

    public static PacketVehicleDriveAnswer decode(FriendlyByteBuf buf) {
        return new PacketVehicleDriveAnswer(buf.readBoolean(), buf.readInt(), buf.readUUID());
    }

    public static void handle(PacketVehicleDriveAnswer msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer slPlayer = ctx.get().getSender();
            if (slPlayer != null) {
                handleAnswer(slPlayer, msg.accept, msg.vehicleId, msg.requesterUUID);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleAnswer(ServerPlayer slPlayer, boolean accept, int vehicleId, UUID requesterUUID) {
        if (!WarfareData.of(slPlayer).getBoolean(WarfareKeys.IS_SQUAD_LEADER)) {
            PlayerFeedback.deny(slPlayer, "pwpwarfare.msg.drive.only_leader");
            return;
        }

        ServerLevel level = slPlayer.serverLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        Entity vehicle = level.getEntity(vehicleId);
        if (vehicle == null) {
            PlayerFeedback.deny(slPlayer, "pwpwarfare.msg.drive.vehicle_gone");
            return;
        }

        ServerPlayer requester =
                (ServerPlayer) level.getServer().getPlayerList().getPlayer(requesterUUID);
        if (requester == null) {
            PlayerFeedback.deny(slPlayer, "pwpwarfare.msg.drive.requester_offline");
            return;
        }

        // отвечать может только лидер отряда просившего (раньше — любой лидер любому игроку)
        String slTeam = slPlayer.getTeam() == null ? "" : slPlayer.getTeam().getName();
        String reqTeam = requester.getTeam() == null ? "" : requester.getTeam().getName();
        if (!slTeam.equalsIgnoreCase(reqTeam)
                || WarfareData.of(requester).getInt(WarfareKeys.SQUAD_ID)
                        != WarfareData.of(slPlayer).getInt(WarfareKeys.SQUAD_ID)) {
            PlayerFeedback.deny(slPlayer, "pwpwarfare.vehicle_menu.error.not_your_squad");
            return;
        }

        UUID vehicleUUID = vehicle.getUUID();
        com.pigeostudios.pwp.warfare.server.VehicleActionService.noteAnswer(vehicle, requesterUUID);

        if (accept) {
            data.addApprovedDriver(vehicleUUID, requesterUUID);
            WarfareData.of(vehicle).remove(WarfareKeys.FRESH_VEHICLE);
            requester.displayClientMessage(
                    Component.translatable("pwpwarfare.msg.drive.accepted").withStyle(ChatFormatting.GREEN), true);
        } else {
            WarfareData.of(requester)
                    .putLong(WarfareKeys.driveRequestCooldown(vehicleId), GameClock.worldTicks(level) + 6000);
            requester.displayClientMessage(
                    Component.translatable("pwpwarfare.msg.drive.denied").withStyle(ChatFormatting.RED), true);
        }
    }
}
