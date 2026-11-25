package com.pigeostudios.pwp.warfare.network;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.events.KitUtil;
import com.pigeostudios.pwp.warfare.server.FactionDataLoader;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import io.netty.handler.codec.DecoderException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Выбор кита (сеть v2). Клиент присылает только имя кита и индексы альтернатив по слотам; сервер
 * проверяет, что кит есть у команды игрока и доступен ему ({@link KitAccess#unavailableReason}:
 * лимиты, лидерская роль). При отказе PENDING_KIT не меняется.
 */
public record PacketSelectKit(String kitName, Map<String, Integer> slotSelections) implements Net.C2S {
    public static final int MAX_KIT_NAME = 64;
    public static final int MAX_SELECTIONS = 16;
    public static final int MAX_SELECTION_KEY = 32;
    public static final int MAX_SELECTION_VALUE = 64;
    /** Явный мусор (больше любого разумного) — разрыв соединения ещё на декодировании. */
    private static final int DECODE_HARD_LIMIT = 256;

    public PacketSelectKit {
        slotSelections = slotSelections != null ? Map.copyOf(slotSelections) : Map.of();
    }

    public PacketSelectKit(String kitName) {
        this(kitName, Map.of());
    }

    public static void encode(PacketSelectKit msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.kitName, MAX_KIT_NAME);
        buf.writeVarInt(msg.slotSelections.size());
        for (var e : msg.slotSelections.entrySet()) {
            buf.writeUtf(e.getKey(), MAX_SELECTION_KEY);
            buf.writeVarInt(e.getValue());
        }
    }

    public static PacketSelectKit decode(FriendlyByteBuf buf) {
        String name = buf.readUtf(MAX_KIT_NAME);
        int size = buf.readVarInt();
        if (size < 0 || size > DECODE_HARD_LIMIT) throw new DecoderException("PacketSelectKit: записей " + size);
        Map<String, Integer> sel = new HashMap<>();
        for (int i = 0; i < size; i++) {
            sel.put(buf.readUtf(MAX_SELECTION_KEY), buf.readVarInt());
        }
        return new PacketSelectKit(name, sel);
    }

    @Override
    public String validate() {
        String err = Validate.first(
                Validate.label("kitName", kitName, MAX_KIT_NAME),
                Validate.range("slotSelections.size", slotSelections.size(), 0, MAX_SELECTIONS));
        if (err != null) return err;
        for (var e : slotSelections.entrySet()) {
            err = Validate.first(
                    Validate.id("slotSelections.key", e.getKey(), MAX_SELECTION_KEY),
                    Validate.range("slotSelections." + e.getKey(), e.getValue(), 0, MAX_SELECTION_VALUE));
            if (err != null) return err;
        }
        return null;
    }

    @Override
    public void handle(ServerPlayer player) {
        WarfareWorldData data = WarfareWorldData.get(player.serverLevel());
        Map<String, WarfareWorldData.KitInfo> kits = KitAccess.teamKits(player, data);
        WarfareWorldData.KitInfo kit = kits != null ? kits.get(kitName) : null;
        String reason = kit == null ? KitReasonText.NOT_FOUND : KitAccess.unavailableReason(player, data, kitName, kit);
        if (reason != null) {
            player.sendSystemMessage(
                    Component.translatable("pwpwarfare.message.kit_unavailable", kitName, KitReasonText.of(reason))
                            .withStyle(ChatFormatting.RED));
            if (kits != null) PacketRequestKitMenu.sendKitMenu(player, data);
            return;
        }
        String previous = WarfareData.of(player).getString(WarfareKeys.PENDING_KIT);
        WarfareData.of(player).putString(WarfareKeys.PENDING_KIT, kitName);
        if (!previous.equals(kitName)) {
            // Ожидание деплоя (киты грузились) отменяется — игрок подтвердит заново
            FactionDataLoader.cancelDeploy(player, "pwpwarfare.msg.deploy.cancel_kit");
        }
        CompoundTag selTag = new CompoundTag();
        for (var e : slotSelections.entrySet()) {
            selTag.putInt(e.getKey(), e.getValue());
        }
        WarfareData.of(player).put(WarfareKeys.SLOT_SELECTIONS, selTag);
        // Синхронизируем выбранный кит клиенту (ClientData.myCurrentKit)
        KitUtil.syncMyKit(player);
        // Стейджинг (решение 1): живой в своём мейне получает кит сразу, стрельба в мейне и так закрыта
        if (com.pigeostudios.pwp.warfare.staging.StagingService.issueOnSelect(player, data)) {
            player.sendSystemMessage(Component.translatable("pwpwarfare.message.kit_issued", kitName)
                    .withStyle(ChatFormatting.GREEN));
            return;
        }
        PacketHandler.sendToAllClients(player.serverLevel(), data);
        String pTeam = player.getTeam() != null ? player.getTeam().getName().toUpperCase() : "";
        boolean isReserved = !data.match.isGameStarted
                || (data.match.invasionSetupActive && !pTeam.equalsIgnoreCase(data.match.invasionDefender));
        String key = isReserved ? "pwpwarfare.message.kit_reserved" : "pwpwarfare.message.kit_selected";
        player.sendSystemMessage(Component.translatable(key, kitName).withStyle(ChatFormatting.YELLOW));
    }
}
