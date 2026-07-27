package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
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
        if (!slPlayer.getPersistentData().getBoolean("WARFARE_IsSquadLeader")) return;

        ServerLevel level = slPlayer.serverLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        Entity vehicle = level.getEntity(vehicleId);
        if (vehicle == null) return;

        ServerPlayer requester = (ServerPlayer) level.getServer().getPlayerList().getPlayer(requesterUUID);
        if (requester == null) return;

        UUID vehicleUUID = vehicle.getUUID();

        if (accept) {
            data.addApprovedDriver(vehicleUUID, requesterUUID);
            vehicle.getPersistentData().remove("WARFARE_FreshVehicle");
            requester.displayClientMessage(Component.literal("\u2705 Ваша заявка на управление техникой принята!").withStyle(ChatFormatting.GREEN), true);
        } else {
            requester.getPersistentData().putLong("WARFARE_DriveRequestCooldown_" + vehicleId, level.getGameTime() + 6000);
            requester.displayClientMessage(Component.literal("\u274C Заявка отклонена. Повтор через 5 минут.").withStyle(ChatFormatting.RED), true);
        }
    }
}