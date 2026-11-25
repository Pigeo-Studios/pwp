package com.pigeostudios.pwp.limit.network;

import com.pigeostudios.pwp.limit.ModConfig;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * Значения лимитов сервера → клиенту. Клиент раньше читал SERVER-конфиг напрямую в кадре: до его
 * загрузки (главное меню, первые тики входа) это бросало IllegalStateException. Теперь клиент
 * читает только {@code LimitsConfigCache}, который заполняет этот пакет (при входе и при перезагрузке конфига).
 */
public final class LimitsConfigPacket {
    public final boolean jumpCooldownEnabled;
    public final double jumpCooldownSeconds;

    public LimitsConfigPacket(boolean jumpCooldownEnabled, double jumpCooldownSeconds) {
        this.jumpCooldownEnabled = jumpCooldownEnabled;
        this.jumpCooldownSeconds = jumpCooldownSeconds;
    }

    public static void encode(LimitsConfigPacket m, FriendlyByteBuf b) {
        b.writeBoolean(m.jumpCooldownEnabled);
        b.writeDouble(m.jumpCooldownSeconds);
    }

    public static LimitsConfigPacket decode(FriendlyByteBuf b) {
        return new LimitsConfigPacket(b.readBoolean(), b.readDouble());
    }

    public static void handle(LimitsConfigPacket m, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        if (c.getDirection().getReceptionSide().isClient()) {
            double secs =
                    Double.isFinite(m.jumpCooldownSeconds) ? Math.max(0, Math.min(60, m.jumpCooldownSeconds)) : 1.0;
            c.enqueueWork(() -> com.pigeostudios.pwp.limit.client.LimitsConfigCache.apply(m.jumpCooldownEnabled, secs));
        }
        c.setPacketHandled(true);
    }

    private static LimitsConfigPacket current() {
        return new LimitsConfigPacket(ModConfig.ENABLE_JUMP_COOLDOWN.get(), ModConfig.JUMP_COOLDOWN_SECONDS.get());
    }

    public static void sendTo(ServerPlayer player) {
        NetworkHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), current());
    }

    /** Всем (после перезагрузки конфига). На клиенте без сервера — ничего. */
    public static void broadcast() {
        if (FMLEnvironment.dist != net.minecraftforge.api.distmarker.Dist.DEDICATED_SERVER
                && net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer() == null) return;
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), current());
    }
}
