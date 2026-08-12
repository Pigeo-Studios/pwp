package com.pigeostudios.pwp.limit.network;

import com.pigeostudios.pwp.limit.ModConfig;
import com.pigeostudios.pwp.limit.client.LimitsConfigCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

// Пакет синхронизации конфига лимитов: сервер -> клиент.
// Единый источник истины на клиенте — LimitsConfigCache, заполняемый этим пакетом
// при входе игрока и при перезагрузке серверного конфига.
public class LimitsConfigPacket {
    public final boolean jumpCooldownEnabled;
    public final double jumpCooldownSeconds;

    public LimitsConfigPacket(boolean jumpCooldownEnabled, double jumpCooldownSeconds) {
        this.jumpCooldownEnabled = jumpCooldownEnabled;
        this.jumpCooldownSeconds = jumpCooldownSeconds;
    }

    public static void encode(LimitsConfigPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.jumpCooldownEnabled);
        buf.writeDouble(msg.jumpCooldownSeconds);
    }

    public static LimitsConfigPacket decode(FriendlyByteBuf buf) {
        return new LimitsConfigPacket(buf.readBoolean(), buf.readDouble());
    }

    public static void handle(LimitsConfigPacket msg, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            ctx.get().enqueueWork(() -> LimitsConfigCache.apply(msg.jumpCooldownEnabled, msg.jumpCooldownSeconds));
        }
        ctx.get().setPacketHandled(true);
    }

    // Отправка текущих значений серверного конфига конкретному игроку (при входе)
    public static void sendTo(ServerPlayer player) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player),
                new LimitsConfigPacket(ModConfig.ENABLE_JUMP_COOLDOWN.get(), ModConfig.JUMP_COOLDOWN_SECONDS.get()));
    }

    // Рассылка всем игрокам (при перезагрузке конфига).
    // Только на выделенном сервере: на клиенте ModConfigEvent.Reloading тоже
    // срабатывает при приёме синхронизированного конфига в login-хендшейке
    // (ConfigSync), где PacketDistributor.getServer() == null — NPE убил бы
    // хендшейк и клиент получил бы «lost connection: Disconnected».
    public static void broadcast() {
        if (FMLEnvironment.dist != Dist.DEDICATED_SERVER) return;
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new LimitsConfigPacket(ModConfig.ENABLE_JUMP_COOLDOWN.get(), ModConfig.JUMP_COOLDOWN_SECONDS.get()));
    }
}
