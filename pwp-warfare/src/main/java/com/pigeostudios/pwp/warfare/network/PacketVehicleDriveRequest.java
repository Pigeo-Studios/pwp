package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.UUID;
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
        if (!vehicle.getPersistentData().contains("WARFARE_VehicleTeam")) return;

        String vType = vehicle.getPersistentData().getString("WARFARE_VehicleType");
        if (!isSpecialistVehicle(vType)) return;

        String pKit = player.getPersistentData().getString("WARFARE_CurrentKit");
        if (!hasCorrectKit(vType, pKit)) return;

        int squadId = player.getPersistentData().getInt("WARFARE_SquadID");
        if (squadId == 0) return;

        if (!vehicle.getPersistentData().getBoolean("WARFARE_FreshVehicle")) return;

        long cooldownUntil = player.getPersistentData().getLong("WARFARE_DriveRequestCooldown_" + vehicleId);
        if (cooldownUntil > level.getGameTime()) return;

        WarfareWorldData data = WarfareWorldData.get(level);
        String pName = player.getScoreboardName();
        String team = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";

        WarfareWorldData.Squad playerSquad = data.squads.stream()
            .filter(s -> s.id == squadId && s.team.equalsIgnoreCase(team))
            .findFirst().orElse(null);
        if (playerSquad == null) return;

        ServerPlayer slPlayer = level.getServer().getPlayerList().getPlayerByName(playerSquad.leader);
        if (slPlayer == null) return;

        Component approveCmd = Component.literal("[ПРИНЯТЬ \u2705]")
            .withStyle(style -> style.withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/vanswer approve " + vehicleId + " " + player.getUUID()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Разрешить управление"))));
        Component denyCmd = Component.literal("[ОТКЛОНИТЬ \u274C]")
            .withStyle(style -> style.withColor(ChatFormatting.RED)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    "/vanswer deny " + vehicleId + " " + player.getUUID()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal("Отклонить запрос"))));

        slPlayer.sendSystemMessage(Component.literal("\u26A1 " + pName + " запрашивает управление " + vType + " ")
            .append(approveCmd).append(" ").append(denyCmd));

        player.displayClientMessage(Component.literal("Запрос отправлен командиру отряда").withStyle(ChatFormatting.GREEN), true);
    }

    private static boolean isSpecialistVehicle(String vType) {
        if (vType.equalsIgnoreCase("TANK")) return true;
        if (vType.equalsIgnoreCase("APC")) return true;
        if (vType.equalsIgnoreCase("Mobile ZU")) return true;
        if (vType.equalsIgnoreCase("HELICOPTER")) return true;
        if (vType.toUpperCase().contains("CAS")) return true;
        if (vType.toUpperCase().contains("SUPPLY HELICOPTER")) return true;
        return false;
    }

    private static boolean hasCorrectKit(String vType, String kit) {
        if (vType.equalsIgnoreCase("HELICOPTER") || vType.toUpperCase().contains("CAS") || vType.toUpperCase().contains("SUPPLY HELICOPTER")) {
            return kit.equals("Pilot") || kit.equals("Pilot Officer");
        }
        if (vType.equalsIgnoreCase("TANK") || vType.equalsIgnoreCase("APC") || vType.equalsIgnoreCase("Mobile ZU")) {
            return kit.equals("Mechanic") || kit.equals("Mechanic Officer");
        }
        return true;
    }
}