package com.pigeostudios.pwp.warfare.server;

import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.entity.SupplyCrateEntity;
import com.pigeostudios.pwp.warfare.match.CommanderRules;
import com.pigeostudios.pwp.warfare.network.MapPlayerInfo;
import com.pigeostudios.pwp.warfare.util.PlayerFeedback;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.time.GameClock;
import com.pwp.api.util.LogOnce;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Способности командира (этап 15 ROADMAP): вызывает лидер командного отряда из меню карты (ПКМ → «Командир»).
 * Правила и кулдауны — {@link CommanderRules}; кулдаун на команду, в памяти сервера (матч = новый сервер).
 *
 * <ul>
 *   <li>UAV — враги в радиусе видны на карте команды 30 с (добавляются в рассылку игроков карты только этой
 *       команде — {@link #revealedFor});
 *   <li>Сброс снабжения — ящик снабжения ({@link SupplyCrateEntity}) падает в точку;
 *   <li>CAS — через 8 с подлёта серия разрывов вдоль линии взгляда командира (ландшафт не ломается).
 * </ul>
 */
public final class CommanderAbilities {
    private static final Logger LOG = LoggerFactory.getLogger(CommanderAbilities.class);
    private static final CommanderRules.Cooldowns COOLDOWNS = new CommanderRules.Cooldowns();
    private static final float CAS_POWER = 5.5F;
    private static final int CAS_SHELLS = 6;
    private static final int CAS_INTERVAL = 6;

    private record Reveal(String team, double x, double z, int radius, long until) {}

    private record Blast(ServerLevel level, double x, double z, long at) {}

    private static final List<Reveal> REVEALS = new ArrayList<>();
    private static final List<Blast> BLASTS = new ArrayList<>();

    private CommanderAbilities() {}

    public static void init() {
        MinecraftForge.EVENT_BUS.addListener(CommanderAbilities::onTick);
    }

    /** Враги, раскрытые разведкой команды {@code team}, из полного списка игроков карты. */
    public static synchronized List<MapPlayerInfo> revealedFor(String team, List<MapPlayerInfo> all, long now) {
        List<MapPlayerInfo> out = new ArrayList<>();
        for (Reveal r : REVEALS) {
            if (!r.team.equalsIgnoreCase(team) || r.until < now) continue;
            for (MapPlayerInfo p : all) {
                if (p.team == null || p.team.equalsIgnoreCase(team) || out.contains(p)) continue;
                if (CommanderRules.revealed(p.x, p.z, r.x, r.z, r.radius)) out.add(p);
            }
        }
        return out;
    }

    private static boolean isCommander(ServerPlayer p, WarfareWorldData data, String team) {
        int mySquad = WarfareData.of(p).getInt(WarfareKeys.SQUAD_ID);
        boolean sl = WarfareData.of(p).getBoolean(WarfareKeys.IS_SQUAD_LEADER);
        int cmd = team.equals("BLUE") ? data.squadRegistry.blueCMDId : data.squadRegistry.redCMDId;
        return cmd != -1 && mySquad == cmd && sl;
    }

    static synchronized String use(ServerPlayer p, CommanderRules.Ability a, int x, int z) {
        String team = p.getTeam() == null ? "" : p.getTeam().getName().toUpperCase(java.util.Locale.ROOT);
        if (!team.equals("BLUE") && !team.equals("RED")) return "pwpwarfare.cmd.error.no_team";
        ServerLevel level = p.serverLevel();
        WarfareWorldData data = WarfareWorldData.get(level);
        if (!data.match.isGameStarted) return "pwpwarfare.cmd.error.not_started";
        if (!isCommander(p, data, team)) return "pwpwarfare.cmd.error.not_commander";
        long now = GameClock.serverTicks(p.server);
        long left = COOLDOWNS.remaining(team, a, now);
        if (left > 0) {
            PlayerFeedback.deny(p, "pwpwarfare.cmd.error.cooldown", (left + 19) / 20);
            return "";
        }
        BlockPos column = new BlockPos(x, 0, z);
        if (!level.isLoaded(column)) return "pwpwarfare.cmd.error.far";
        COOLDOWNS.tryUse(team, a, now);
        // квадрат карты считает клиент (знает начало сетки) — в сообщении координаты
        String kp = x + ", " + z;
        switch (a) {
            case UAV -> REVEALS.add(new Reveal(team, x + 0.5, z + 0.5, a.radius, now + a.durationTicks));
            case SUPPLY -> {
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                level.addFreshEntity(new SupplyCrateEntity(level, x + 0.5, y + 12, z + 0.5, team, p.getUUID()));
            }
            case CAS -> {
                double[][] pts = CommanderRules.casLine(x + 0.5, z + 0.5, p.getYRot() + 180F, a.radius, CAS_SHELLS);
                for (int i = 0; i < pts.length; i++) {
                    BLASTS.add(new Blast(level, pts[i][0], pts[i][1], now + a.durationTicks + (long) i * CAS_INTERVAL));
                }
            }
        }
        Component msg = Component.translatable("pwpwarfare.cmd.used." + a.name().toLowerCase(java.util.Locale.ROOT), kp)
                .withStyle(ChatFormatting.GOLD);
        for (ServerPlayer o : p.server.getPlayerList().getPlayers()) {
            if (o.getTeam() != null && o.getTeam().getName().equalsIgnoreCase(team)) o.sendSystemMessage(msg);
        }
        LOG.info("Командир {} ({}) применил {} в {}", p.getGameProfile().getName(), team, a, kp);
        return null;
    }

    private static void onTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        MinecraftServer server = e.getServer();
        long now = GameClock.serverTicks(server);
        synchronized (CommanderAbilities.class) {
            REVEALS.removeIf(r -> r.until < now);
            for (Iterator<Blast> it = BLASTS.iterator(); it.hasNext(); ) {
                Blast b = it.next();
                if (b.at > now) continue;
                it.remove();
                int bx = (int) Math.floor(b.x);
                int bz = (int) Math.floor(b.z);
                if (!b.level.isLoaded(new BlockPos(bx, 0, bz))) continue;
                try {
                    int y = b.level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
                    b.level.explode(null, b.x, y + 0.5, b.z, CAS_POWER, Level.ExplosionInteraction.NONE);
                } catch (RuntimeException ex) {
                    LogOnce.warn(LOG, "cas-blast", "Разрыв авиаудара не удался", ex);
                }
            }
        }
    }

    /** C2S: способность командира в точке карты. */
    public record Use(String ability, int x, int z) implements Net.C2S {
        public static void encode(Use m, FriendlyByteBuf b) {
            b.writeUtf(m.ability, 16);
            b.writeVarInt(m.x);
            b.writeVarInt(m.z);
        }

        public static Use decode(FriendlyByteBuf b) {
            return new Use(b.readUtf(16), b.readVarInt(), b.readVarInt());
        }

        @Override
        public String validate() {
            if (CommanderRules.Ability.parse(ability) == null) return "ability: " + ability;
            return Validate.first(
                    Validate.range("x", x, -30_000_000, 30_000_000), Validate.range("z", z, -30_000_000, 30_000_000));
        }

        @Override
        public void handle(ServerPlayer sender) {
            String err = use(sender, CommanderRules.Ability.parse(ability), x, z);
            if (err != null && !err.isEmpty()) PlayerFeedback.deny(sender, err);
        }
    }

    /** Протокол 9 — в самый конец канала (после редактора дивизий). */
    public static int register(net.minecraftforge.network.simple.SimpleChannel ch, int id) {
        return new Net.Registrar(ch, id)
                .c2s(Use.class, Use::encode, Use::decode)
                .nextId();
    }
}
