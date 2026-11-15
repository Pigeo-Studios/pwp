package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

public class PacketSelectKit {
    public final String kitName;
    public final Map<String, Integer> slotSelections;

    public PacketSelectKit(String kitName) {
        this(kitName, Map.of());
    }

    public PacketSelectKit(String kitName, Map<String, Integer> slotSelections) {
        this.kitName = kitName;
        this.slotSelections = slotSelections != null ? slotSelections : Map.of();
    }

    public static void encode(PacketSelectKit msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.kitName);
        buf.writeInt(msg.slotSelections.size());
        for (var e : msg.slotSelections.entrySet()) {
            buf.writeUtf(e.getKey());
            buf.writeInt(e.getValue());
        }
    }

    public static PacketSelectKit decode(FriendlyByteBuf buf) {
        String name = buf.readUtf();
        int size = buf.readInt();
        Map<String, Integer> sel = new HashMap<>();
        for (int i = 0; i < size; i++) {
            sel.put(buf.readUtf(), buf.readInt());
        }
        return new PacketSelectKit(name, sel);
    }

    public static void handle(PacketSelectKit msg, Supplier<Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
                String previous = WarfareData.of(player).getString(WarfareKeys.PENDING_KIT);
                WarfareData.of(player).putString(WarfareKeys.PENDING_KIT, msg.kitName);
                if (!previous.equals(msg.kitName)) {
                    // Ожидание деплоя (киты грузились) отменяется — игрок подтвердит заново
                    com.pigeostudios.pwp.warfare.server.FactionDataLoader.cancelDeploy(
                            player, "Выбор кита изменён — нажмите «Деплой» снова");
                }
                CompoundTag selTag = new CompoundTag();
                for (var e : msg.slotSelections.entrySet()) {
                    selTag.putInt(e.getKey(), e.getValue());
                }
                WarfareData.of(player).put(WarfareKeys.SLOT_SELECTIONS, selTag);
                // Синхронизируем выбранный кит клиенту (ClientData.myCurrentKit)
                com.pigeostudios.pwp.warfare.events.KitUtil.syncMyKit(player);
                PacketHandler.sendToAllClients(player.serverLevel(), data);
                String pTeam =
                        player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
                boolean isReserved = !data.match.isGameStarted
                        || (data.match.invasionSetupActive && !pTeam.equalsIgnoreCase(data.match.invasionDefender));
                if (isReserved) {
                    player.sendSystemMessage(Component.translatable("pwpwarfare.message.kit_reserved", msg.kitName)
                            .withStyle(ChatFormatting.YELLOW));
                } else {
                    player.sendSystemMessage(Component.translatable("pwpwarfare.message.kit_selected", msg.kitName)
                            .withStyle(ChatFormatting.YELLOW));
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
