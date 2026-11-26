package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.match.TacticalRose;
import com.pigeostudios.pwp.warfare.util.PlayerFeedback;
import com.pigeostudios.pwp.warfare.world.MapMarker;
import com.pwp.api.net.Net;
import com.pwp.api.time.GameClock;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Действия тактической радиалки для любого бойца (этап 17d, как в Squad): «враг здесь» — метка врага команде в
 * точке взгляда (точку считает сервер, клиент шлёт только намерение) и запросы отряду: медик, патроны,
 * транспорт. Кулдауны — {@link TacticalRose}, абсолютная метка «до» на игрока и вид действия.
 */
public final class TacticalActions {
    private static final Map<String, Long> UNTIL = new ConcurrentHashMap<>();

    private TacticalActions() {}

    static String team(ServerPlayer p) {
        return p.getTeam() == null ? "" : p.getTeam().getName();
    }

    /** true — можно (и кулдаун взведён); false — игроку уже сказано «подождите». */
    static boolean cooldown(ServerPlayer p, String kind, int ticks) {
        long now = GameClock.serverTicks(p.server);
        String key = p.getStringUUID() + "|" + kind;
        Long until = UNTIL.get(key);
        if (until != null && until > now) {
            PlayerFeedback.deny(p, "pwpwarfare.tactical.error.cooldown", (until - now + 19) / 20);
            return false;
        }
        UNTIL.put(key, now + ticks);
        return true;
    }

    public static void forget(ServerPlayer p) {
        String prefix = p.getStringUUID() + "|";
        UNTIL.keySet().removeIf(k -> k.startsWith(prefix));
    }

    static void spot(ServerPlayer p) {
        String team = team(p);
        if (!team.equalsIgnoreCase("BLUE") && !team.equalsIgnoreCase("RED")) {
            PlayerFeedback.deny(p, "pwpwarfare.msg.marker.no_team");
            return;
        }
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(TacticalRose.SPOT_RANGE));
        BlockHitResult hit =
                p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.BLOCK) {
            PlayerFeedback.deny(p, "pwpwarfare.tactical.error.no_target");
            return;
        }
        if (!cooldown(p, "spot", TacticalRose.SPOT_COOLDOWN_TICKS)) return;
        BlockPos pos = hit.getBlockPos().relative(hit.getDirection());
        // тот же вид, что «враг» с карты (SquadMapScreen.place): видимость enemy, категория enemy, пехота
        MapMarker m = new MapMarker(
                UUID.randomUUID(), "enemy", "enemy", "infantry", pos, p.getUUID(), GameClock.serverTicks(p.server));
        MarkerManager.add(m, team);
        MarkerManager.broadcastToTeam(p, m);
        PlayerFeedback.info(p, "pwpwarfare.tactical.spotted");
    }

    static void request(ServerPlayer p, TacticalRose.Request r) {
        int squad = WarfareData.of(p).getInt(WarfareKeys.SQUAD_ID);
        if (squad == 0) {
            PlayerFeedback.deny(p, "pwpwarfare.msg.squad.not_in_squad");
            return;
        }
        if (!cooldown(p, "req_" + r.id(), TacticalRose.REQUEST_COOLDOWN_TICKS)) return;
        String team = team(p);
        String name = p.getGameProfile().getName();
        int sent = 0;
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            if (o == p || !team(o).equalsIgnoreCase(team)) continue;
            if (WarfareData.of(o).getInt(WarfareKeys.SQUAD_ID) != squad) continue;
            int dist = (int) Math.round(o.distanceTo(p));
            Component msg = Component.translatable("pwpwarfare.tactical.request." + r.id(), name, dist)
                    .withStyle(ChatFormatting.GOLD);
            o.sendSystemMessage(msg);
            o.displayClientMessage(msg, true);
            sent++;
        }
        PlayerFeedback.info(p, sent > 0 ? "pwpwarfare.tactical.request_sent" : "pwpwarfare.tactical.request_nobody");
    }

    /** C2S: действие радиалки. {@code kind}: spot | medic | ammo | ride. Только намерение. */
    public record Act(String kind) implements Net.C2S {
        public static void encode(Act m, FriendlyByteBuf b) {
            b.writeUtf(m.kind, 16);
        }

        public static Act decode(FriendlyByteBuf b) {
            return new Act(b.readUtf(16));
        }

        @Override
        public String validate() {
            if ("spot".equals(kind) || TacticalRose.Request.parse(kind) != null) return null;
            return "kind: " + kind;
        }

        @Override
        public void handle(ServerPlayer sender) {
            if ("spot".equals(kind)) spot(sender);
            else request(sender, TacticalRose.Request.parse(kind.toLowerCase(Locale.ROOT)));
        }
    }

    /** Протокол 11 — в самый конец канала (после действий с техникой). */
    public static int register(net.minecraftforge.network.simple.SimpleChannel ch, int id) {
        return new Net.Registrar(ch, id)
                .c2s(Act.class, Act::encode, Act::decode)
                .nextId();
    }
}
