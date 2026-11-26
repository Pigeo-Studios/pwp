package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.util.PlayerFeedback;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.time.GameClock;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketVehicleDriveRequest {
    private final int vehicleId;

    public PacketVehicleDriveRequest(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public static void encode(PacketVehicleDriveRequest msg, FriendlyByteBuf buf) {
        buf.writeInt(msg.vehicleId);
    }

    public static PacketVehicleDriveRequest decode(FriendlyByteBuf buf) {
        return new PacketVehicleDriveRequest(buf.readInt());
    }

    public static void handle(PacketVehicleDriveRequest msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                handleRequest(player, msg.vehicleId);
            }
        });
        ctx.get().setPacketHandled(true);
    }

    public static void handleRequest(ServerPlayer player, int vehicleId) {
        ServerLevel level = player.serverLevel();
        Entity vehicle = level.getEntity(vehicleId);
        if (vehicle == null) return;
        if (!WarfareData.of(vehicle).contains(WarfareKeys.VEHICLE_TEAM)) return;

        String vType = WarfareData.of(vehicle).getString(WarfareKeys.VEHICLE_TYPE);
        if (!com.pigeostudios.pwp.warfare.vehicles.CrewRules.isSpecialist(vType)) return;

        String pKit = com.pigeostudios.pwp.warfare.events.KitUtil.getEffectiveKit(player);
        if (!com.pigeostudios.pwp.warfare.vehicles.CrewRules.hasCorrectKit(vType, pKit)) {
            PlayerFeedback.deny(player, "pwpwarfare.msg.vehicle.crew_kit_required_this");
            return;
        }

        int squadId = WarfareData.of(player).getInt(WarfareKeys.SQUAD_ID);
        if (squadId == 0) {
            PlayerFeedback.deny(player, "pwpwarfare.msg.squad.not_in_squad");
            return;
        }

        if (!WarfareData.of(vehicle).getBoolean(WarfareKeys.FRESH_VEHICLE)) {
            // машина уже «разблокирована» — разрешение лидера не нужно
            PlayerFeedback.info(player, "pwpwarfare.msg.drive.not_needed");
            return;
        }

        long cooldownUntil = WarfareData.of(player).getLong(WarfareKeys.driveRequestCooldown(vehicleId));
        if (cooldownUntil > GameClock.worldTicks(level)) {
            PlayerFeedback.deny(
                    player,
                    "pwpwarfare.msg.vehicle.request_cooldown",
                    (cooldownUntil - GameClock.worldTicks(level)) / 20L);
            return;
        }

        WarfareWorldData data = WarfareWorldData.get(level);
        String pName = player.getScoreboardName();
        String team = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";

        WarfareWorldData.Squad playerSquad = data.squadRegistry.squads.stream()
                .filter(s -> s.id == squadId && s.team.equalsIgnoreCase(team))
                .findFirst()
                .orElse(null);
        if (playerSquad == null) {
            PlayerFeedback.deny(player, "pwpwarfare.msg.squad.not_in_squad");
            return;
        }

        ServerPlayer slPlayer = level.getServer().getPlayerList().getPlayerByName(playerSquad.leader);
        if (slPlayer == null) {
            PlayerFeedback.deny(player, "pwpwarfare.msg.drive.leader_offline");
            return;
        }

        Component approveCmd = Component.translatable("pwpwarfare.msg.drive.approve_button")
                .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND,
                                "/vanswer approve " + vehicleId + " " + player.getUUID()))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("pwpwarfare.msg.drive.approve_hover"))));
        Component denyCmd = Component.translatable("pwpwarfare.msg.drive.deny_button")
                .withStyle(style -> style.withColor(ChatFormatting.RED)
                        .withClickEvent(new ClickEvent(
                                ClickEvent.Action.RUN_COMMAND, "/vanswer deny " + vehicleId + " " + player.getUUID()))
                        .withHoverEvent(new HoverEvent(
                                HoverEvent.Action.SHOW_TEXT,
                                Component.translatable("pwpwarfare.msg.drive.deny_hover"))));

        slPlayer.sendSystemMessage(Component.translatable("pwpwarfare.msg.drive.request", pName, vType)
                .append(approveCmd)
                .append(" ")
                .append(denyCmd));

        player.displayClientMessage(
                Component.translatable("pwpwarfare.msg.drive.request_sent").withStyle(ChatFormatting.GREEN),
                true); // запрос виден лидеру и в радиалке техники (этап 17a)
        com.pigeostudios.pwp.warfare.server.VehicleActionService.noteRequest(vehicle, player);
    }

    /** Нужна ли машине разрешение лидера на вождение (машина специалиста и правило включено в конфиге). */
    public static boolean needsLeaderPermission(Entity vehicle) {
        return (Boolean) com.pigeostudios.pwp.warfare.config.WarfareConfig.REQUIRE_SL_PERMISSION_TO_DRIVE.get()
                && com.pigeostudios.pwp.warfare.vehicles.CrewRules.isSpecialist(
                        WarfareData.of(vehicle).getString(WarfareKeys.VEHICLE_TYPE));
    }
}
