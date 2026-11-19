package com.pigeostudios.pwp.warfare.admin;

import com.pigeostudios.pwp.warfare.config.WarfareConfig;
import com.pigeostudios.pwp.warfare.data.state.WarfareData;
import com.pigeostudios.pwp.warfare.data.state.WarfareKeys;
import com.pigeostudios.pwp.warfare.events.KitUtil;
import com.pigeostudios.pwp.warfare.network.KitAccess;
import com.pigeostudios.pwp.warfare.network.PacketHandler;
import com.pigeostudios.pwp.warfare.network.PacketRequestKitMenu;
import com.pigeostudios.pwp.warfare.network.ResupplyHandler;
import com.pigeostudios.pwp.warfare.squad.SquadActions;
import com.pigeostudios.pwp.warfare.world.WarfareWorldData;
import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import com.pwp.api.security.AccessLevel;
import com.pwp.api.security.ModerationInput;
import com.pwp.coreclient.network.data.AdminAction;
import com.pwp.coreclient.network.data.ModerationAction;
import java.util.function.Function;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Действия админ-панели над игроком матча (вкладка «Игроки», кнопки warfare): сбросить кит, сбросить
 * кулдауны, выдать/снять лидерство отряда. Уровень ADMIN — на сервере ({@link AdminAction#runLocal}),
 * ошибки — ключами. Сеть v2, регистрация в конце канала warfare. Также дописывает в {@code adminPlayers}
 * отряд/кит/лидера и объявляет свои кнопки ({@link ModerationAction#declareAction}).
 */
public final class PlayerAdminPackets {
    public static final String RESET_KIT = "player.resetKit";
    public static final String RESET_COOLDOWNS = "player.resetCooldowns";
    public static final String GIVE_LEADER = "player.giveLeader";
    public static final String TAKE_LEADER = "player.takeLeader";

    static final String E = "pwpwarfare.admin.player.error.";
    static final String ERR_OFFLINE = "pwp_core.admin.mod.error.offline";
    static final String ERR_NO_SQUAD = E + "no_squad";
    static final String ERR_ALREADY_LEADER = E + "already_leader";
    static final String ERR_NOT_LEADER = E + "not_leader";
    static final String ERR_ALONE = E + "alone";

    private PlayerAdminPackets() {}

    /** commonSetup warfare: поля игрока и объявление кнопок. */
    public static void registerData() {
        for (String id : new String[] {RESET_KIT, RESET_COOLDOWNS, GIVE_LEADER, TAKE_LEADER}) {
            ModerationAction.declareAction(id);
        }
        ModerationAction.addPlayerInfo((pl, o) -> {
            WarfareWorldData data = WarfareWorldData.get(pl.serverLevel());
            WarfareWorldData.Squad s = SquadActions.getPlayerSquad(pl.getScoreboardName(), data);
            o.addProperty("squad", s == null ? "" : s.name);
            o.addProperty("leader", s != null && s.leader.equals(pl.getScoreboardName()));
            String kit = KitUtil.getEffectiveKit(pl);
            o.addProperty("kit", kit == null || kit.equals(KitAccess.UNASSIGNED) ? "" : kit);
        });
    }

    private static void run(ServerPlayer s, String action, String target, Function<ServerPlayer, String> body) {
        AdminAction.runLocal(s, action, AccessLevel.ADMIN, target, () -> {
            if (!ModerationInput.validName(target)) return ERR_OFFLINE;
            ServerPlayer t = s.server.getPlayerList().getPlayerByName(target);
            return t == null ? ERR_OFFLINE : body.apply(t);
        });
    }

    // ---------- логика ----------

    /**
     * Сбросить кит: роль освобождается (лимиты ролей снова считают её свободной), снаряжение и curios
     * снимаются (иначе с «пустым» китом остаётся оружие роли), игроку открывается выбор кита.
     */
    static String resetKit(ServerPlayer admin, ServerPlayer t) {
        WarfareData d = WarfareData.of(t);
        d.putString(WarfareKeys.CURRENT_KIT, KitAccess.UNASSIGNED);
        d.remove(WarfareKeys.PENDING_KIT);
        if (!t.isCreative()) {
            t.getInventory().clearContent();
            ResupplyHandler.clearCurios(t);
            t.inventoryMenu.broadcastChanges();
        }
        KitUtil.syncMyKit(t);
        WarfareWorldData data = WarfareWorldData.get(t.serverLevel());
        PacketRequestKitMenu.sendKitMenu(t, data);
        PacketHandler.sendToAllClients(t.serverLevel(), data);
        notifyTarget(t, "pwpwarfare.admin.player.kit_reset_you");
        return null;
    }

    /** Сбросить кулдауны: склад/ФОБ-ресапплай, ящик снабжения, респавн-команда, деплой дрона. */
    static String resetCooldowns(ServerPlayer admin, ServerPlayer t) {
        WarfareData d = WarfareData.of(t);
        d.remove(WarfareKeys.LAST_MAIN_RESUPPLY);
        d.remove(WarfareKeys.LAST_FOB_RESUPPLY);
        d.remove(WarfareKeys.LAST_SUPPLY_TIME);
        d.remove(WarfareKeys.NEXT_SUPPLY_TIME);
        d.remove(WarfareKeys.LAST_RESPAWN_COMMAND);
        d.remove(WarfareKeys.DRONE_DEPLOY_CD);
        notifyTarget(t, "pwpwarfare.admin.player.cooldowns_reset_you");
        return null;
    }

    /** Сделать игрока лидером его отряда (прежний лидер становится бойцом). */
    static String giveLeader(ServerPlayer admin, ServerPlayer t) {
        WarfareWorldData data = WarfareWorldData.get(t.serverLevel());
        WarfareWorldData.Squad s = SquadActions.getPlayerSquad(t.getScoreboardName(), data);
        if (s == null) return ERR_NO_SQUAD;
        if (s.leader.equals(t.getScoreboardName())) return ERR_ALREADY_LEADER;
        ServerPlayer old = t.server.getPlayerList().getPlayerByName(s.leader);
        if (old != null) {
            SquadActions.updatePlayerTags(old, s.id, false);
            SquadActions.removeRadio(old);
            notifyTarget(old, "pwpwarfare.admin.player.leader_taken_you");
        }
        setLeader(t, s, data);
        return null;
    }

    /** Снять лидерство: оно переходит первому другому бойцу отряда. Один в отряде — ошибка. */
    static String takeLeader(ServerPlayer admin, ServerPlayer t) {
        WarfareWorldData data = WarfareWorldData.get(t.serverLevel());
        WarfareWorldData.Squad s = SquadActions.getPlayerSquad(t.getScoreboardName(), data);
        if (s == null) return ERR_NO_SQUAD;
        if (!s.leader.equals(t.getScoreboardName())) return ERR_NOT_LEADER;
        ServerPlayer next = null;
        for (String m : s.members) {
            if (m.equals(t.getScoreboardName())) continue;
            next = t.server.getPlayerList().getPlayerByName(m);
            if (next != null) break;
        }
        if (next == null) return ERR_ALONE;
        SquadActions.updatePlayerTags(t, s.id, false);
        SquadActions.removeRadio(t);
        notifyTarget(t, "pwpwarfare.admin.player.leader_taken_you");
        setLeader(next, s, data);
        return null;
    }

    private static void setLeader(ServerPlayer p, WarfareWorldData.Squad s, WarfareWorldData data) {
        s.leader = p.getScoreboardName();
        SquadActions.updatePlayerTags(p, s.id, true);
        if (WarfareConfig.AUTO_GIVE_SL_RADIO.get()) SquadActions.giveRadio(p);
        notifyTarget(p, "pwpwarfare.msg.squad.promoted_you");
        data.setDirty();
        PacketHandler.syncSquadsAll(data.squadRegistry.squads);
        PacketRequestKitMenu.sendKitMenu(p, data);
        PacketHandler.sendToAllClients(p.serverLevel(), data);
    }

    private static void notifyTarget(ServerPlayer t, String key) {
        t.sendSystemMessage(Component.translatable(key).withStyle(ChatFormatting.GOLD));
    }

    // ---------- пакеты ----------

    public record ResetKit(String target) implements Net.C2S {
        public static void encode(ResetKit m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
        }

        public static ResetKit decode(FriendlyByteBuf b) {
            return new ResetKit(b.readUtf(16));
        }

        @Override
        public String validate() {
            return Validate.text("target", target, 16);
        }

        @Override
        public void handle(ServerPlayer s) {
            run(s, RESET_KIT, target, t -> resetKit(s, t));
        }
    }

    public record ResetCooldowns(String target) implements Net.C2S {
        public static void encode(ResetCooldowns m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
        }

        public static ResetCooldowns decode(FriendlyByteBuf b) {
            return new ResetCooldowns(b.readUtf(16));
        }

        @Override
        public String validate() {
            return Validate.text("target", target, 16);
        }

        @Override
        public void handle(ServerPlayer s) {
            run(s, RESET_COOLDOWNS, target, t -> resetCooldowns(s, t));
        }
    }

    /** {@code give} — выдать (true) или снять (false) лидерство. */
    public record Leader(String target, boolean give) implements Net.C2S {
        public static void encode(Leader m, FriendlyByteBuf b) {
            b.writeUtf(m.target, 16);
            b.writeBoolean(m.give);
        }

        public static Leader decode(FriendlyByteBuf b) {
            return new Leader(b.readUtf(16), b.readBoolean());
        }

        @Override
        public String validate() {
            return Validate.text("target", target, 16);
        }

        @Override
        public void handle(ServerPlayer s) {
            run(s, give ? GIVE_LEADER : TAKE_LEADER, target, t -> give ? giveLeader(s, t) : takeLeader(s, t));
        }
    }

    /** Регистрация в канале warfare — В КОНЕЦ списка. */
    public static void register(Net.Registrar r) {
        r.c2s(ResetKit.class, ResetKit::encode, ResetKit::decode)
                .c2s(ResetCooldowns.class, ResetCooldowns::encode, ResetCooldowns::decode)
                .c2s(Leader.class, Leader::encode, Leader::decode);
    }
}
