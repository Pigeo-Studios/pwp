package com.pigeostudios.pwp.warfare.squad;

import com.pwp.api.net.Net;
import com.pwp.api.net.Validate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * C→S пакеты действий отряда — по пакету на действие (раньше один PacketSquadAction с номером
 * действия 0–11 и строкой «на все случаи»). Поля типизированы и проверяются до логики;
 * права (лидер/командир огневой группы) проверяет {@link SquadActions}.
 */
public final class SquadPackets {
    /** Имя отряда: логика обрезает до 12 символов; по сети — не длиннее 32. */
    static final int MAX_SQUAD_NAME = 32;
    /** Ник игрока Minecraft — до 16 символов. */
    static final int MAX_PLAYER_NAME = 16;

    private SquadPackets() {}

    static String playerName(String field, String value) {
        return Validate.text(field, value, MAX_PLAYER_NAME);
    }

    static String checkSquadId(int id) {
        return Validate.range("squadId", id, 0, 1000);
    }

    /** Создать отряд (пустое имя — следующее свободное из NATO-алфавита). */
    public record Create(String name) implements Net.C2S {
        public static void encode(Create m, FriendlyByteBuf b) {
            b.writeUtf(m.name, MAX_SQUAD_NAME);
        }

        public static Create decode(FriendlyByteBuf b) {
            return new Create(b.readUtf(MAX_SQUAD_NAME));
        }

        @Override
        public String validate() {
            return Validate.optionalText("name", name, MAX_SQUAD_NAME);
        }

        @Override
        public void handle(ServerPlayer p) {
            SquadActions.perform(p, (pl, d, n, t) -> SquadActions.create(pl, d, n, t, 0, name));
        }
    }

    /** Вступить в отряд своей команды. */
    public record Join(int squadId) implements Net.C2S {
        public static void encode(Join m, FriendlyByteBuf b) {
            b.writeVarInt(m.squadId);
        }

        public static Join decode(FriendlyByteBuf b) {
            return new Join(b.readVarInt());
        }

        @Override
        public String validate() {
            return SquadPackets.checkSquadId(squadId);
        }

        @Override
        public void handle(ServerPlayer p) {
            SquadActions.perform(p, (pl, d, n, t) -> SquadActions.join(pl, d, n, t, squadId, ""));
        }
    }

    /** Выйти из текущего отряда. */
    public record Leave() implements Net.C2S {
        public static void encode(Leave m, FriendlyByteBuf b) {}

        public static Leave decode(FriendlyByteBuf b) {
            return new Leave();
        }

        @Override
        public void handle(ServerPlayer p) {
            SquadActions.perform(p, (pl, d, n, t) -> SquadActions.leave(pl, d, n, t, 0, ""));
        }
    }

    /** Кикнуть участника (лидер) или удалить офлайн-лидера (участник). */
    public record Kick(int squadId, String target) implements Net.C2S {
        public static void encode(Kick m, FriendlyByteBuf b) {
            b.writeVarInt(m.squadId);
            b.writeUtf(m.target, MAX_PLAYER_NAME);
        }

        public static Kick decode(FriendlyByteBuf b) {
            return new Kick(b.readVarInt(), b.readUtf(MAX_PLAYER_NAME));
        }

        @Override
        public String validate() {
            return Validate.first(SquadPackets.checkSquadId(squadId), playerName("target", target));
        }

        @Override
        public void handle(ServerPlayer p) {
            SquadActions.perform(p, (pl, d, n, t) -> SquadActions.kick(pl, d, n, t, squadId, target));
        }
    }

    /** Передать лидерство отряда. */
    public record Promote(String target) implements Net.C2S {
        public static void encode(Promote m, FriendlyByteBuf b) {
            b.writeUtf(m.target, MAX_PLAYER_NAME);
        }

        public static Promote decode(FriendlyByteBuf b) {
            return new Promote(b.readUtf(MAX_PLAYER_NAME));
        }

        @Override
        public String validate() {
            return playerName("target", target);
        }

        @Override
        public void handle(ServerPlayer p) {
            SquadActions.perform(p, (pl, d, n, t) -> SquadActions.promote(pl, d, n, t, 0, target));
        }
    }

    /** Закрыть/открыть отряд (лидер). */
    public record ToggleLock() implements Net.C2S {
        public static void encode(ToggleLock m, FriendlyByteBuf b) {}

        public static ToggleLock decode(FriendlyByteBuf b) {
            return new ToggleLock();
        }

        @Override
        public void handle(ServerPlayer p) {
            SquadActions.perform(p, (pl, d, n, t) -> SquadActions.toggleLock(pl, d, n, t, 0, ""));
        }
    }

    /** Огневая группа отряда. */
    public enum Fireteam {
        BRAVO,
        CHARLIE
    }

    /** Назначить командира огневой группы. */
    public record SetFireteamLeader(Fireteam fireteam, String target) implements Net.C2S {
        public static void encode(SetFireteamLeader m, FriendlyByteBuf b) {
            b.writeEnum(m.fireteam);
            b.writeUtf(m.target, MAX_PLAYER_NAME);
        }

        public static SetFireteamLeader decode(FriendlyByteBuf b) {
            return new SetFireteamLeader(b.readEnum(Fireteam.class), b.readUtf(MAX_PLAYER_NAME));
        }

        @Override
        public String validate() {
            return playerName("target", target);
        }

        @Override
        public void handle(ServerPlayer p) {
            SquadActions.perform(p, (pl, d, n, t) -> {
                if (fireteam == Fireteam.BRAVO) SquadActions.setBravoLeader(pl, d, n, t, 0, target);
                else SquadActions.setCharlieLeader(pl, d, n, t, 0, target);
            });
        }
    }

    /** Добавить участника в огневую группу. */
    public record AddToFireteam(Fireteam fireteam, String target) implements Net.C2S {
        public static void encode(AddToFireteam m, FriendlyByteBuf b) {
            b.writeEnum(m.fireteam);
            b.writeUtf(m.target, MAX_PLAYER_NAME);
        }

        public static AddToFireteam decode(FriendlyByteBuf b) {
            return new AddToFireteam(b.readEnum(Fireteam.class), b.readUtf(MAX_PLAYER_NAME));
        }

        @Override
        public String validate() {
            return playerName("target", target);
        }

        @Override
        public void handle(ServerPlayer p) {
            SquadActions.perform(p, (pl, d, n, t) -> {
                if (fireteam == Fireteam.BRAVO) SquadActions.addToBravo(pl, d, n, t, 0, target);
                else SquadActions.addToCharlie(pl, d, n, t, 0, target);
            });
        }
    }

    /** Убрать участника из огневых групп. */
    public record RemoveFromFireteam(String target) implements Net.C2S {
        public static void encode(RemoveFromFireteam m, FriendlyByteBuf b) {
            b.writeUtf(m.target, MAX_PLAYER_NAME);
        }

        public static RemoveFromFireteam decode(FriendlyByteBuf b) {
            return new RemoveFromFireteam(b.readUtf(MAX_PLAYER_NAME));
        }

        @Override
        public String validate() {
            return playerName("target", target);
        }

        @Override
        public void handle(ServerPlayer p) {
            SquadActions.perform(p, (pl, d, n, t) -> SquadActions.removeFromFireteam(pl, d, n, t, 0, target));
        }
    }

    /** Распустить отряд (лидер). */
    public record Disband(int squadId) implements Net.C2S {
        public static void encode(Disband m, FriendlyByteBuf b) {
            b.writeVarInt(m.squadId);
        }

        public static Disband decode(FriendlyByteBuf b) {
            return new Disband(b.readVarInt());
        }

        @Override
        public String validate() {
            return SquadPackets.checkSquadId(squadId);
        }

        @Override
        public void handle(ServerPlayer p) {
            SquadActions.perform(p, (pl, d, n, t) -> SquadActions.disband(pl, d, n, t, squadId, ""));
        }
    }

    /** Регистрация в канале pwpwarfare (в конец списка). */
    public static void register(Net.Registrar r) {
        r.c2s(Create.class, Create::encode, Create::decode)
                .c2s(Join.class, Join::encode, Join::decode)
                .c2s(Leave.class, Leave::encode, Leave::decode)
                .c2s(Kick.class, Kick::encode, Kick::decode)
                .c2s(Promote.class, Promote::encode, Promote::decode)
                .c2s(ToggleLock.class, ToggleLock::encode, ToggleLock::decode)
                .c2s(SetFireteamLeader.class, SetFireteamLeader::encode, SetFireteamLeader::decode)
                .c2s(AddToFireteam.class, AddToFireteam::encode, AddToFireteam::decode)
                .c2s(RemoveFromFireteam.class, RemoveFromFireteam::encode, RemoveFromFireteam::decode)
                .c2s(Disband.class, Disband::encode, Disband::decode);
    }
}
